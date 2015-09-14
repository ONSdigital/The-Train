package com.github.davidcarboni.thetrain.storage;

import com.github.davidcarboni.thetrain.helpers.Hash.ShaInputStream;
import com.github.davidcarboni.thetrain.helpers.Hash.ShaOutputStream;
import com.github.davidcarboni.thetrain.helpers.PathUtils;
import com.github.davidcarboni.thetrain.json.Transaction;
import com.github.davidcarboni.thetrain.json.UriInfo;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Class for handling publishing actions.
 */
public class Publisher {

    /**
     * Adds a set of files contained in a zip to the given transaction. The start date for each file transfer is the instant when each {@link ZipEntry} is accessed.
     *
     * @param transaction The transaction to add the file to
     * @param uri         The target URI for the file
     * @param zip         The zipped files
     * @return The hash of the file once included in the transaction.
     * @throws IOException If a filesystem error occurs.
     */
    public static boolean addFiles(Transaction transaction, String uri, ZipInputStream zip) throws IOException {
        boolean result = true;
        ZipEntry entry;
        Date startDate;
        while ((entry = zip.getNextEntry()) != null && !entry.isDirectory()) {
            startDate = new Date();
            String targetUri = PathUtils.stripTrailingSlash(uri) + PathUtils.setLeadingSlash(entry.getName());
            System.out.println("Unzipping: " + targetUri);
            result &= addFile(transaction, targetUri, zip, startDate);
            zip.closeEntry();
        }
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
        return addFile(transaction, uri, input, new Date());
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
    public static boolean addFile(Transaction transaction, String uri, InputStream input, Date startDate) throws IOException {
        String sha = null;
        long size = 0;

        // Add the file
        Path content = Transactions.content(transaction);
        Path target = PathUtils.toPath(uri, content);
        if (target != null) {
            // Encrypt if a key was provided, then delete the original
            Files.createDirectories(target.getParent());
            try (ShaOutputStream output = PathUtils.encryptingStream(target, transaction.key())) {
                IOUtils.copy(input, output);
                sha = output.sha();
                size = output.size();
            }
        }

        // Update the transaction
        UriInfo uriInfo = new UriInfo(uri, startDate);
        uriInfo.stop(sha, size);
        transaction.addUri(uriInfo);
        Transactions.tryUpdateAsync(transaction.id());

        return StringUtils.isNotBlank(sha);
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
        boolean result = true;

        List<String> uris = listUris(transaction);
        for (String uri : uris) {
            result &= commitFile(uri, transaction, website);
        }

        transaction.commit(result);
        Transactions.update(transaction);

        if (result) {
            Transactions.end(transaction);
        }

        return result;
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
        Path backup = null;

        // We use a very broad exception catch clause to
        // ensure any and all commit errors are trapped
        try {

            // Back up the existing file, if present
            String action = UriInfo.CREATE;
            if (Files.exists(target)) {
                backup = PathUtils.toPath(uri, Transactions.backup(transaction));
                Files.createDirectories(backup.getParent());
                Files.move(target, backup);
                action = UriInfo.UPDATE;
            }

            // Publish the file
            // NB we don't need to worry about overwriting because
            // any existing copy will already have been moved.
            Files.createDirectories(target.getParent());
            // NB We're using copy rather than move for two reasons:
            // - To be able to review a transaction after the fac and see all the files that were published
            // - If we use encryption we need to copy through a cipher stream to handle decryption
            String uploadedSha = uriInfo.sha();
            long uploadedSize = uriInfo.size();
            String committedSha;
            long committedSize;
            try (ShaInputStream input = PathUtils.decryptingStream(source, transaction.key()); ShaOutputStream output = new ShaOutputStream(PathUtils.outputStream(target))) {
                IOUtils.copy(input, output);
                committedSha = output.sha();
                committedSize = output.size();
            }
            if (StringUtils.equals(uploadedSha, committedSha) && uploadedSize == committedSize) {
                uriInfo.commit(action);
                result = true;
            } else {
                uriInfo.fail("Published file mismatch. Uploaded: " + uploadedSize + " bytes (" + uploadedSha + ") Committed: " + committedSize + " bytes (" + committedSha + ")");
            }

        } catch (Throwable t) {

            // Record the error
            String error = "Error committing '" + source + "' to '" + target + "'.\n";
            if (backup != null) error += "\nBackup file is '" + backup + "'.\n";
            error += ExceptionUtils.getStackTrace(t);
            if (uriInfo != null) {
                uriInfo.fail(error);
            } else {
                transaction.addError(error);
            }
        }

        // If this fails, we have a serious issue, so let it fail the entire request:
        Transactions.tryUpdateAsync(transaction.id());

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

        // If this fails, we have a serious issue, so let it fail the entire request:
        Transactions.tryUpdateAsync(transaction.id());

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
