package com.github.davidcarboni.thetrain.storage;

import com.github.davidcarboni.thetrain.helpers.Configuration;
import com.github.davidcarboni.thetrain.helpers.PathUtils;
import com.github.davidcarboni.thetrain.helpers.ShaInputStream;
import com.github.davidcarboni.thetrain.helpers.UnionInputStream;
import com.github.davidcarboni.thetrain.json.Transaction;
import com.github.davidcarboni.thetrain.json.UriInfo;
import com.github.davidcarboni.thetrain.json.request.FileCopy;
import com.github.davidcarboni.thetrain.json.request.Manifest;
import com.github.davidcarboni.thetrain.logging.LogBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.github.davidcarboni.thetrain.logging.LogBuilder.logBuilder;

/**
 * Class for handling publishing actions.
 */
public class Publisher {

    private static final int bufferSize;
    private static final ExecutorService pool;

    // TODO - Don't like this but for now it will do.
    static {
        bufferSize = 100 * 1024;
        pool = Executors.newFixedThreadPool(Configuration.threadPoolSize());
        Runtime.getRuntime().addShutdownHook(new ShutdownTask(pool));
    }

    private static void copyFile(File src, File dest) throws IOException {
        try (
                FileInputStream fis = new FileInputStream(src);
                FileOutputStream fos = new FileOutputStream(dest);
                FileChannel srcChannel = fis.getChannel();
                FileChannel destChannel = fos.getChannel()
        ) {
            destChannel.transferFrom(srcChannel, 0, srcChannel.size());
        } catch (IOException e) {
            logBuilder()
                    .addParameter("src", src.toString())
                    .addParameter("dest", dest.toString())
                    .error(e, "unexpected error while attempting to copy files");
            throw e;
        }
    }

    /**
     * Adds a set of files contained in a zip to the given transaction. The start date for each file transfer is the instant when each {@link ZipEntry} is accessed.
     *
     * @param transaction The transaction to add the file to
     * @param uri         The target URI for the file
     * @param zip         The zipped files
     * @return The hash of the file once included in the transaction.
     * @throws IOException If a filesystem error occurs.
     */
    public static boolean addFiles(final Transaction transaction, String uri, final ZipInputStream zip) throws IOException {
        long start = System.currentTimeMillis();
        boolean result = true;
        ZipEntry entry;

        // Small files are written asynchronously from byte array buffers:
        List<Future<Boolean>> smallFileWrites = new ArrayList<>();
        int big = 0;
        int small = 0;

        try {
            while ((entry = zip.getNextEntry()) != null && !entry.isDirectory()) {

                final Date startDate = new Date();
                final String targetUri = PathUtils.stripTrailingSlash(uri) + PathUtils.setLeadingSlash(entry.getName());

                // Read small files into a buffer and write them asynchronously
                // NB the size can be -1 if it is unknown, so we read into a buffer to see how much data we're dealing with.
                byte[] buffer = new byte[bufferSize];
                int read;
                int count = 0;
                do {
                    read = zip.read(buffer, count, buffer.length - count);
                    if (read != -1) count += read;
                } while (read != -1 && count < bufferSize);

                final InputStream data = new ByteArrayInputStream(buffer, 0, count);

                // If entry data fit into the buffer, go asynchronous:
                if (count < buffer.length) {
                    smallFileWrites.add(pool.submit(() -> Boolean.valueOf(addFile(transaction, targetUri, new ShaInputStream(data), startDate))));
                    // todo close input stream - pass in data and create stream insude addFile()
                    small++;
                } else {
                    // Large file, so read from (data + "more from the zip")
                    try (
                            UnionInputStream uin = new UnionInputStream(data, zip);
                            ShaInputStream input = new ShaInputStream(uin)
                    ) {
                        result &= addFile(transaction, targetUri, input, startDate);
                        big++;
                    }
                }

                zip.closeEntry(); // do we need to wrap this in finally?
            }

        } catch (IOException e) {
            logBuilder().error(e, "addFiles threw unexpected error");
            throw e;
        }

        // Process results of any asynchronous writes
        for (Future<Boolean> smallFileWrite : smallFileWrites) {
            try {
                result &= smallFileWrite.get().booleanValue();
            } catch (InterruptedException | ExecutionException e) {
                throw new IOException("Error completing small file write", e);
            }
        }
        logBuilder().performanceMetric(start, "addFiles")
                .transactionID(transaction.id())
                .info("step completed");

        logBuilder()
                .addParameter("largeFileSynchronouss", big)
                .addParameter("smallFileAsynchronous", small)
                .addParameter("total", small + big)
                .info("unzip results");
        return result;
    }

    /**
     * Adds a file to the given transaction. The start date for the file transfer is assumed to be the current instant.
     *
     * @param transaction The transaction to add the file to
     * @param uri         The target URI for the file
     * @param input       The file content
     * @return The hash of the file once included in the transaction.
     * @throws IOException If a filesystem error occurs.
     */
    public static boolean addFile(Transaction transaction, String uri, InputStream input) throws IOException {
        return addFile(transaction, uri, new ShaInputStream(input), new Date());
        // TODO again this needs to close the stream
    }

    /**
     * Adds a file to the given transaction. The start date for the file transfer is assumed to be the current instant.
     *
     * @param transaction The transaction to add the file to
     * @param uri         The target URI for the file
     * @param input       The file content
     * @param startDate   The start date for the file transfer. Typically this is when an HTTP request was received, before the uploaded file started being processed.
     * @return The hash of the file once included in the transaction.
     * @throws IOException If a filesystem error occurs.
     */
    public static boolean addFile(Transaction transaction, String uri, ShaInputStream input, Date startDate) throws IOException {
        boolean result = false;

        // Add the file
        Path content = Transactions.content(transaction);
        Path target = PathUtils.toPath(uri, content);

        String action = backupExistingFile(transaction, uri);

        if (target != null) {
            // Encrypt if a key was provided, then delete the original
            Files.createDirectories(target.getParent());
            try (OutputStream output = PathUtils.outputStream(target)) {
                IOUtils.copy(input, output); // comes from request body so use stream copy.
                result = true;
            }
        }

        // Update the transaction
        UriInfo uriInfo = new UriInfo(uri, startDate);
        uriInfo.stop();
        uriInfo.setAction(action);
        transaction.addUri(uriInfo);
        return result;
    }

    /**
     * When making a change to a file on the website, we copy the existing file into a backup
     *
     * @param transaction
     * @param uri
     * @return
     * @throws IOException
     */
    private static String backupExistingFile(Transaction transaction, String uri) throws IOException {
        // Back up the existing file, if present
        String action = UriInfo.CREATE;
        Path website = Website.path();
        Path target = PathUtils.toPath(uri, website);
        if (Files.exists(target)) {
            Path backup = PathUtils.toPath(uri, Transactions.backup(transaction));
            Files.createDirectories(backup.getParent());
            copyFile(target.toFile(), backup.toFile());
            action = UriInfo.UPDATE;
        }
        return action;
    }

    public static int copyFiles(Transaction transaction, Manifest manifest, Path websitePath) throws IOException {
        long start = System.currentTimeMillis();
        int filesMoved = 0;
        List<Future<Boolean>> futures = new ArrayList<>();

        for (FileCopy move : manifest.getFilesToCopy()) {
            futures.add(pool.submit(() -> copyFileIntoTransaction(transaction, move.source, move.target, websitePath)));
        }

        // Process results of any asynchronous writes
        for (Future<Boolean> future : futures) {
            try {
                if (future.get().booleanValue()) {
                    filesMoved++;
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new IOException("Error on commit of file", e);
            }
        }

        logBuilder().performanceMetric(start, "copyFiles")
                .transactionID(transaction.id())
                .info("step completed");
        return filesMoved;
    }

    /**
     * Read the list of URI's to delete from the manifest and add them to the transaction.
     *
     * @param transaction
     * @param manifest
     * @return
     */
    public static int addFilesToDelete(Transaction transaction, Manifest manifest) throws IOException {
        long start = System.currentTimeMillis();
        LogBuilder logBuilder = logBuilder();
        int filesToDelete = 0;

        if (manifest.getUrisToDelete() != null) {
            for (String uri : manifest.getUrisToDelete()) {
                UriInfo uriInfo = new UriInfo(uri, new Date());
                uriInfo.setAction(UriInfo.DELETE);
                transaction.addUriDelete(uriInfo);

                Path website = Website.path();
                Path target = PathUtils.toPath(uri, website);
                Path targetDirectory = target;
                if (Files.exists(targetDirectory)) {
                    Path backupDirectory = PathUtils.toPath(uri, Transactions.backup(transaction));
                    logBuilder.addParameter("directory", target.toString())
                            .info("backing up directory before deletion");
                    FileUtils.copyDirectory(targetDirectory.toFile(), backupDirectory.toFile());
                } else {
                    logBuilder.addParameter("directory", target.toString())
                            .info("cannot backup directory as it does not exist, skipping");
                }
                filesToDelete++;
            }
        }

        logBuilder().performanceMetric(start, "addFilesToDelete")
                .transactionID(transaction.id())
                .info("step completed");
        return filesToDelete;
    }

    /**
     * Copy an existing file from the website into the given transaction.
     *
     * @param transaction
     * @param sourceUri
     * @param targetUri
     * @param websitePath
     * @return
     * @throws IOException
     */
    static boolean copyFileIntoTransaction(Transaction transaction, String sourceUri, String targetUri, Path websitePath) throws IOException {
        LogBuilder logBuilder = logBuilder();
        long start = System.currentTimeMillis();
        boolean moved = false;

        Path source = PathUtils.toPath(sourceUri, websitePath);
        Path target = PathUtils.toPath(targetUri, Transactions.content(transaction));
        Path finalWebsiteTarget = PathUtils.toPath(targetUri, websitePath);

        String action = backupExistingFile(transaction, targetUri);

        if (!Files.exists(source)) {
            logBuilder.addParameter("path", source.toString()).info("could not move file because it does not exist");
            return false;
        }

        // if the file already exists it has already been copied so ignore it.
        // doing this allows the publish to be reattempted if it fails without trying to copy files over existing files.
        if (Files.exists(finalWebsiteTarget)) {
            logBuilder.addParameter("path", finalWebsiteTarget.toString())
                    .info("could not move file as it already exists");
            return false;
        }

        if (target != null) {
            Files.createDirectories(target.getParent());
            copyFile(source.toFile(), target.toFile());
            moved = true;
        }

        // Update the transaction
        UriInfo uriInfo = new UriInfo(targetUri, new Date());
        uriInfo.stop();
        uriInfo.setAction(action);
        transaction.addUri(uriInfo);

        logBuilder().performanceMetric(start, "copyFileIntoTransaction")
                .transactionID(transaction.id())
                .info("step completed");
        return moved;
    }


    public static Path getFile(Transaction transaction, String uri) throws IOException {
        Path result = null;

        Path content = Transactions.content(transaction);
        Path path = PathUtils.toPath(uri, content);
        if (path != null && Files.exists(path) && Files.isRegularFile(path)) {
            result = path;
        }

        return result;
    }

    /**
     * Lists all URIs in this {@link Transaction}.
     *
     * @param transaction The {@link Transaction}
     * @return The list of files (directories are not included).
     * @throws IOException If an error occurs.
     */
    public static List<String> listUris(Transaction transaction) throws IOException {
        Path content = Transactions.content(transaction);
        return PathUtils.listUris(content);
    }

    public static boolean commit(Transaction transaction, Path website) throws IOException {
        long start = System.currentTimeMillis();
        boolean result = true;
        applyTransactionDeletes(transaction, website);

        // Then move file updates from the transaction to the website.
        List<Future<Boolean>> futures = new ArrayList<>();

        try {
            List<String> uris = listUris(transaction);
            for (String uri : uris) {
                futures.add(pool.submit(() -> commitFile(uri, transaction, website)));
            }
        } catch (IOException e) {
            logBuilder().error(e, "commit threw unexpected exception");
            throw e;
        }

        // Process results of any asynchronous writes
        for (Future<Boolean> future : futures) {
            try {
                result &= future.get().booleanValue();
            } catch (InterruptedException | ExecutionException e) {
                throw new IOException("Error on commit of file", e);
            }
        }

        transaction.commit(result);

        if (result) {
            Transactions.end(transaction);
        }

        logBuilder().performanceMetric(start, "commit")
                .transactionID(transaction.id())
                .info("step completed");

        return result;
    }

    private static void applyTransactionDeletes(Transaction transaction, Path website) throws IOException {
        long start = System.currentTimeMillis();
        LogBuilder logBuilder = logBuilder();

        // Apply any deletes that are defined in the transaction first to ensure we do not delete updated files.
        for (UriInfo uriInfo : transaction.urisToDelete()) {
            String uri = uriInfo.uri();
            Path target = PathUtils.toPath(uri, website);

            logBuilder.addParameter("path", target.toString())
                    .transactionID(transaction.id())
                    .info("deleting directory");

            FileUtils.deleteDirectory(target.toFile());

            logBuilder().performanceMetric(start, "applyTransactionDeletes")
                    .transactionID(transaction.id())
                    .info("step completed");
        }
    }

    /**
     * Commits a single file in a transaction to the website, backing up any existing file if necessary.
     *
     * @param uri         The URI to be committed.
     * @param transaction The transaction to commit from.
     * @param website     The website directory to commit to.
     * @throws IOException If a filesystem error occurs.
     */
    static boolean commitFile(String uri, Transaction transaction, Path website) throws IOException {
        boolean result = false;

        UriInfo uriInfo = findUri(uri, transaction);
        Path source = PathUtils.toPath(uri, Transactions.content(transaction));
        Path target = PathUtils.toPath(uri, website);

        // We use a very broad exception catch clause to
        // ensure any and all commit errors are trapped
        try {

            // Publish the file
            // NB we don't need to worry about overwriting because
            // any existing copy will already have been moved.
            Files.createDirectories(target.getParent());
            // NB We're using copy rather than move for two reasons:
            // - To be able to review a transaction after the fact and see all the files that were published
            // - If we use encryption we need to copy through a cipher stream to handle decryption
            copyFile(source.toFile(), target.toFile());
            uriInfo.commit();
            result = true;

        } catch (Throwable t) {

            // Record the error
            String error = "Error committing '" + source + "' to '" + target + "'.\n";
            error += ExceptionUtils.getStackTrace(t);
            if (uriInfo != null) {
                uriInfo.fail(error);
            } else {
                transaction.addError(error);
            }
        }
        return result;
    }

    public static boolean rollback(Transaction transaction) throws IOException {
        boolean result = true;

        List<String> uris = listUris(transaction);
        for (String uri : uris) {
            result &= rollbackFile(uri, transaction);
        }

        transaction.rollback(result);
        Transactions.update(transaction);

        if (result) {
            Transactions.end(transaction);
        }

        return result;
    }

    static boolean rollbackFile(String uri, Transaction transaction) throws IOException {
        boolean result = false;

        UriInfo uriInfo = findUri(uri, transaction);
        Path source = PathUtils.toPath(uri, Transactions.content(transaction));

        // We use a very broad exception catch clause to
        // ensure any and all commit errors are trapped
        try {

            uriInfo.rollback();
            result = true;

        } catch (Throwable t) {

            // Record the error
            String error = "Error rolling back '" + source + ".\n";
            error += ExceptionUtils.getStackTrace(t);
            if (uriInfo != null) {
                uriInfo.fail(error);
            } else {
                transaction.addError(error);
            }
        }

        return result;
    }

    static UriInfo findUri(String uri, Transaction transaction) {
        for (UriInfo transactionUriInfo : transaction.uris()) {
            if (StringUtils.equals(uri, transactionUriInfo.uri())) {
                return transactionUriInfo;
            }
        }

        // We didn't find the requested URI:
        return new UriInfo(uri);
    }
}
