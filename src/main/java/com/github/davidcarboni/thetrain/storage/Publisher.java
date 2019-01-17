package com.github.davidcarboni.thetrain.storage;

import com.github.davidcarboni.thetrain.helpers.PathUtils;
import com.github.davidcarboni.thetrain.helpers.UnionInputStream;
import com.github.davidcarboni.thetrain.json.Transaction;
import com.github.davidcarboni.thetrain.json.UriInfo;
import com.github.davidcarboni.thetrain.json.request.FileCopy;
import com.github.davidcarboni.thetrain.json.request.Manifest;
import com.github.davidcarboni.thetrain.logging.LogBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.github.davidcarboni.thetrain.logging.LogBuilder.logBuilder;
import static com.github.davidcarboni.thetrain.logging.MetricEvents.ADD_DELETE_FILES;
import static com.github.davidcarboni.thetrain.logging.MetricEvents.ADD_FILES;
import static com.github.davidcarboni.thetrain.logging.MetricEvents.APPLY_DELETES;
import static com.github.davidcarboni.thetrain.logging.MetricEvents.COMMIT;
import static com.github.davidcarboni.thetrain.logging.MetricEvents.COPY_FILE;
import static com.github.davidcarboni.thetrain.logging.MetricEvents.COPY_FILES;

/**
 * Class for handling publishing actions.
 */
public class Publisher {

    private static ExecutorService pool;
    private static Publisher instance;

    private final int bufferSize;

    /**
     * Initalize the publisher
     */
    public static void init(int threadPoolSzie) {
        pool = Executors.newFixedThreadPool(threadPoolSzie);
        logBuilder().addParameter("threads", threadPoolSzie).info("initialised publisher thread pool");

        Runtime.getRuntime().addShutdownHook(new ShutdownTask(pool));
        getInstance();
    }

    /**
     * @return the singleton instance of the publisher/
     */
    public static Publisher getInstance() {
        if (instance == null) {
            synchronized (Publisher.class) {
                if (instance == null) {
                    int bufferSize = 100 * 1024;
                    instance = new Publisher(bufferSize);
                    logBuilder().addParameter("bufferSize", bufferSize).info("initialised new publisher instance");
                }
            }
        }
        return instance;
    }

    /**
     * Construct a new Publisher instance with the specified buffer size. Publisher is a singleton instance - use
     * {@link Publisher#getInstance()}
     */
    private Publisher(final int bufferSize) {
        this.bufferSize = bufferSize;
    }

    private void copyFile(File src, File dest) throws IOException {
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


    private boolean addStreamContentToTransaction(Path target, InputStream input) throws IOException {
        if (target != null) {
            ReadableByteChannel src = null;
            FileOutputStream fos = null;
            FileChannel dest = null;

            Files.createDirectories(target.getParent());
            try {
                src = Channels.newChannel(input);
                fos = new FileOutputStream(target.toFile());
                dest = fos.getChannel();

                dest.transferFrom(src, 0, Long.MAX_VALUE);
            } catch (Exception e) {
                logBuilder()
                        .addParameter("targetPath", target.toString())
                        .error(e, "unexpected error transfering inputstream content to transaction via file channel");
                return false;
            } finally {
                src.close();
                fos.close();
                dest.close();
                logBuilder().uri(target.toString()).info("exiting addStreamContentToTransaction");
            }
        }
        return true;
    }

/*
    private boolean addStreamContentToTransaction(Path target, InputStream input) throws IOException {
        if (target != null) {
            Files.createDirectories(target.getParent());
            try (
                    ReadableByteChannel src = Channels.newChannel(input);
                    FileOutputStream fos = new FileOutputStream(target.toFile());
                    FileChannel dest = fos.getChannel()
            ) {
                dest.transferFrom(src, 0, Long.MAX_VALUE);
            } catch (Exception e) {
                logBuilder()
                        .addParameter("targetPath", target.toString())
                        .error(e, "unexpected error transfering inputstream content to transaction via file channel");
                return false;
            } finally {
                logBuilder().uri(target.toString()).info("exiting addStreamContentToTransaction");
            }
        }
        return true;
    }*/

    /**
     * Adds a set of files contained in a zip to the given transaction. The start date for each file transfer is the instant when each {@link ZipEntry} is accessed.
     *
     * @param transaction The transaction to add the file to
     * @param uri         The target URI for the file
     * @param zip         The zipped files
     * @return The hash of the file once included in the transaction.
     * @throws IOException If a filesystem error occurs.
     */
    public boolean addFiles(final Transaction transaction, String uri, final ZipInputStream zip) throws IOException {
        LocalDateTime start = LocalDateTime.now();
        boolean result = true;
        ZipEntry entry;

        // Small files are written asynchronously from byte array buffers:
        List<Future<TransactionUpdate>> smallFileWrites = new ArrayList<>();
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
                    smallFileWrites.add(pool.submit(() -> addContentToTransaction(transaction, targetUri, data, startDate)));
                    small++;
                } else {
                    // Large file, so read from (data + "more from the zip")

                    // TODO: Defect: These streams should be closed however closing the UnionInputStream will close
                    // the underlying zip stream - which breaks the app.
                    // It's probably possible to fix this but it would mean a big refactor (and this code is mental)
                    // - its been this way for 2+ years and hasn't broken anything and the train's day are number so we
                    // are taking the view - leave it and do it properly when we replace the Train.
                    //
                    // - Dave L.
                    //ShaInputStream input = new ShaInputStream(new UnionInputStream(data, zip));
                    try {
                        InputStream input = new UnionInputStream(data, zip);
                        TransactionUpdate update = addContentToTransaction(transaction, targetUri, input, startDate);
                        result &= update.isSuccess();
                        big++;
                    } catch (Exception e) {
                        logBuilder()
                                .uri(uri)
                                .error(e, "Large zip file error: " + e.getCause().getMessage());
                        throw new IOException(e);
                    }
                }
                logBuilder().uri(uri).info("calling close on zip entry");
                try {
                    zip.closeEntry();
                }catch (Exception e) {
                    logBuilder().uri(uri).error(e, "zip.closeEntry threw exception");
                }
            }

        } catch (IOException e) {
            logBuilder()
                    .transactionID(transaction.id())
                    .error(e, "addFiles threw unexpected error");
            throw e;
        }

        List<UriInfo> infos = new ArrayList<>();
        // Process results of any asynchronous writes
        for (Future<TransactionUpdate> smallFileWrite : smallFileWrites) {
            try {

                TransactionUpdate res = smallFileWrite.get();
                result &= res.isSuccess();
                infos.add(res.getUriInfo());
            } catch (InterruptedException | ExecutionException e) {
                throw new IOException("Error completing small file write", e);
            }
        }

        transaction.addUris(infos);

        logBuilder().metrics(start, ADD_FILES)
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
    public boolean addFile(Transaction transaction, String uri, InputStream input) throws IOException {
        TransactionUpdate update = addContentToTransaction(transaction, uri, input, new Date());
        if (update.isSuccess()) {
            transaction.addUri(update.getUriInfo());
            return true;
        }
        return false;
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
    public TransactionUpdate addContentToTransaction(Transaction transaction, String uri, InputStream input, Date startDate)
            throws IOException {
        Path content = Transactions.content(transaction);
        Path target = PathUtils.toPath(uri, content);
        String action = backupExistingFile(transaction, uri);

        TransactionUpdate result = new TransactionUpdate();
        UriInfo uriInfo = new UriInfo(uri, startDate);

        boolean addResult = addStreamContentToTransaction(target, input);
        result.setSuccess(addResult);

        uriInfo.stop();
        uriInfo.setAction(action);

        result.setUriInfo(uriInfo);
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
    private String backupExistingFile(Transaction transaction, String uri) throws IOException {
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

    public int copyFilesIntoTransaction(Transaction transaction, Manifest manifest, Path websitePath) throws IOException {
        LocalDateTime start = LocalDateTime.now();
        int filesMoved = 0;
        List<Future<TransactionUpdate>> futures = new ArrayList<>();

        for (FileCopy move : manifest.getFilesToCopy()) {
            futures.add(pool.submit(() -> copyFileIntoTransaction(transaction, move.source, move.target, websitePath)));
        }

        List<UriInfo> results = new ArrayList<>();

        // Process results of any asynchronous writes
        for (Future<TransactionUpdate> future : futures) {
            try {
                TransactionUpdate res = future.get();
                if (res.isSuccess()) {
                    filesMoved++;
                    results.add(res.getUriInfo());
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new IOException("Error on commit of file", e);
            }
        }

        // all good update transaction
        transaction.addUris(results);

        logBuilder().metrics(start, COPY_FILES)
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
    public int addFilesToDelete(Transaction transaction, Manifest manifest) throws IOException {
        LocalDateTime start = LocalDateTime.now();
        LogBuilder logBuilder = logBuilder();

        List<UriInfo> deletedURIS = new ArrayList<>();
        Path website = Website.path();

        if (manifest.getUrisToDelete() != null) {

            for (String uri : manifest.getUrisToDelete()) {
                UriInfo uriInfo = new UriInfo(uri, new Date());
                uriInfo.setAction(UriInfo.DELETE);

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
                deletedURIS.add(uriInfo);
            }
            transaction.addUriDeletes(deletedURIS);
        }

        logBuilder().metrics(start, ADD_DELETE_FILES)
                .transactionID(transaction.id())
                .info("step completed");

        return deletedURIS.size();
    }

    /**
     * Copy an existing file from the website into the given transaction.
     */
    TransactionUpdate copyFileIntoTransaction(Transaction transaction, String sourceUri, String targetUri, Path websitePath) throws IOException {
        LogBuilder logBuilder = logBuilder();
        LocalDateTime start = LocalDateTime.now();
        boolean moved = false;
        TransactionUpdate result = new TransactionUpdate();

        Path source = PathUtils.toPath(sourceUri, websitePath);
        Path target = PathUtils.toPath(targetUri, Transactions.content(transaction));
        Path finalWebsiteTarget = PathUtils.toPath(targetUri, websitePath);

        String action = backupExistingFile(transaction, targetUri);

        if (!Files.exists(source)) {
            logBuilder
                    .transactionID(transaction.id())
                    .addParameter("path", source.toString()).info("could not move file because it does not exist");
            return result;
        }

        // if the file already exists it has already been copied so ignore it.
        // doing this allows the publish to be reattempted if it fails without trying to copy files over existing files.
        if (Files.exists(finalWebsiteTarget)) {
            logBuilder.addParameter("path", finalWebsiteTarget.toString())
                    .info("could not move file as it already exists");
            return result;
        }

        if (target != null) {
            Files.createDirectories(target.getParent());
            copyFile(source.toFile(), target.toFile());
            result.setSuccess(true);
        }

        // Update the transaction
        UriInfo uriInfo = new UriInfo(targetUri, new Date());
        uriInfo.stop();
        uriInfo.setAction(action);

        logBuilder().metrics(start, COPY_FILE)
                .transactionID(transaction.id())
                .info("step completed");

        result.setUriInfo(uriInfo);
        return result;
    }

    public Path getFile(Transaction transaction, String uri) throws IOException {
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
    public List<String> listUris(Transaction transaction) throws IOException {
        Path content = Transactions.content(transaction);
        return PathUtils.listUris(content);
    }

    public boolean commit(Transaction transaction, Path website) throws IOException {
        boolean result = true;
        applyTransactionDeletes(transaction, website);
        LocalDateTime start = LocalDateTime.now();

        // Then move file updates from the transaction to the website.
        List<Future<Boolean>> futures = new ArrayList<>();

        try {
            List<String> uris = listUris(transaction);
            for (String uri : uris) {
                futures.add(pool.submit(() -> commitFile(uri, transaction, website)));
            }
        } catch (IOException e) {
            logBuilder().transactionID(transaction.id()).error(e, "commit threw unexpected exception");
            throw e;
        }

        // Process results of any asynchronous writes
        for (Future<Boolean> future : futures) {
            try {
                result &= future.get().booleanValue();
            } catch (InterruptedException | ExecutionException e) {
                logBuilder().transactionID(transaction.id()).error(e, "Error on commit of file");
                throw new IOException("Error on commit of file", e);
            }
        }

        transaction.commit(result);

        if (result) {
            Transactions.end(transaction);
        }

        logBuilder().metrics(start, COMMIT)
                .transactionID(transaction.id())
                .info("step completed");

        return result;
    }

    private void applyTransactionDeletes(Transaction transaction, Path website) throws IOException {
        LocalDateTime start = LocalDateTime.now();
        LogBuilder logBuilder = logBuilder();

        // Apply any deletes that are defined in the transaction first to ensure we do not delete updated files.
        for (UriInfo uriInfo : transaction.urisToDelete()) {
            String uri = uriInfo.uri();
            Path target = PathUtils.toPath(uri, website);

            logBuilder.addParameter("path", target.toString())
                    .transactionID(transaction.id())
                    .info("deleting directory");

            FileUtils.deleteDirectory(target.toFile());

            logBuilder().metrics(start, APPLY_DELETES)
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
    boolean commitFile(String uri, Transaction transaction, Path website) throws IOException {
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

    public boolean rollback(Transaction transaction) throws IOException {
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

    boolean rollbackFile(String uri, Transaction transaction) throws IOException {
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

    UriInfo findUri(String uri, Transaction transaction) {
        Optional<UriInfo> result = transaction.uris()
                .parallelStream()
                .filter(uriInfo -> StringUtils.equals(uri, uriInfo.uri()))
                .findFirst();

        if (result.isPresent()) {
            return result.get();
        }
        return new UriInfo(uri);
    }
}
