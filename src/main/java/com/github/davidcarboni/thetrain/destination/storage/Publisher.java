package com.github.davidcarboni.thetrain.destination.storage;

import com.github.davidcarboni.thetrain.destination.helpers.Hash;
import com.github.davidcarboni.thetrain.destination.helpers.PathUtils;
import com.github.davidcarboni.thetrain.destination.json.Transaction;
import com.github.davidcarboni.thetrain.destination.json.UriInfo;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Class for handling publishing actions.
 */
public class Publisher {

    /**
     * Adds a file to the given transaction. The start date for the file transfer is assumed to be the current instant.
     *
     * @param transaction The transaction to add the file to
     * @param uri         The target URI for the file
     * @param data        The file content
     * @return The hash of the file once included in the transaction.
     * @throws IOException If a filesystem error occurs.
     */
    public static String addFile(Transaction transaction, String uri, Path data) throws IOException {
        return addFile(transaction, uri, data, new Date());
    }

    /**
     * Adds a file to the given transaction. The start date for the file transfer is assumed to be the current instant.
     *
     * @param transaction The transaction to add the file to
     * @param uri         The target URI for the file
     * @param source      The file content
     * @param startDate   The start date for the file transfer. Typically this is when an HTTP request was received, before the uploaded file started being processed.
     * @return The hash of the file once included in the transaction.
     * @throws IOException If a filesystem error occurs.
     */
    public static String addFile(Transaction transaction, String uri, Path source, Date startDate) throws IOException {
        String sha = Hash.sha(source);
        System.out.println("Cleartex sha: "+sha);

        // Add the file
        Path content = Transactions.content(transaction);
        Path target = PathUtils.toPath(uri, content);
        if (target != null) {
            Files.createDirectories(target.getParent());
            if (transaction.key() == null) {
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            } else {
                // Encrypt, then delete the original
                try (InputStream input = PathUtils.inputStream(source); OutputStream output = PathUtils.encryptingStream(target, transaction.key())) {
                    IOUtils.copy(input, output);
                }
                Files.delete(source);
            }
        }

        // Update the transaction
        UriInfo uriInfo = new UriInfo(uri, startDate);
        uriInfo.stop(sha);
        transaction.addUri(uriInfo);
        Transactions.update(transaction);
        System.out.println("Encrypted sha: " + Hash.sha(target));

        return sha;
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
        List<Path> paths = listFiles(content);
        List<String> result = new ArrayList<>();
        for (Path path : paths) {
            String uri = PathUtils.toUri(path, content);
            result.add(uri);
        }
        return result;
    }

    static List<Path> listFiles(Path path) throws IOException {
        final List<Path> result = new ArrayList<>();

        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                result.add(file);
                return FileVisitResult.CONTINUE;
            }
        });

        return result;
    }

    public static boolean commit(Transaction transaction, Path website) throws IOException {
        boolean result = true;

        List<String> uris = listUris(transaction);
        for (String uri : uris) {
            result &= commitFile(uri, transaction, website);
        }

        transaction.end();
        Transactions.update(transaction);

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
            if (Files.exists(target)) {
                backup = PathUtils.toPath(uri, Transactions.backup(transaction));
                Files.createDirectories(backup.getParent());
                Files.move(target, backup);
            }

            // Publish the file
            // NB we don't need to worry about overwriting because
            // any existing copy will already have been moved.
            Files.createDirectories(target.getParent());
            // NB We're using copy rather than move for two reasons:
            // - To be able to review a transaction after the fac and see all the files that were published
            // - If we use encryption we need to copy through a cipher stream to handle decryption
            try (InputStream input = PathUtils.decryptingStream(source, transaction.key()); OutputStream output = PathUtils.outputStream(target)) {
                IOUtils.copy(input, output);
            }
            if (StringUtils.equals(uriInfo.sha(), Hash.sha(target))) {
                uriInfo.commit();
                result = true;
            } else {
                uriInfo.fail("Published file hash mismatch. Source: " + Hash.sha(source) + " Target: " + Hash.sha(target));
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

            System.out.println("Encrypted sha: " + Hash.sha(source));
            System.out.println("Decrypted sha: " + Hash.sha(target));
        }

        // If this fails, we have a serious issue, so let it fail the entire request:
        Transactions.update(transaction);

        return result;
    }

    public static boolean rollback(Transaction transaction) throws IOException {
        boolean result = true;

        List<String> uris = listUris(transaction);
        for (String uri : uris) {
            result &= rollbackFile(uri, transaction);
        }
        FileUtils.deleteQuietly(Transactions.content(transaction).toFile());
        FileUtils.deleteQuietly(Transactions.backup(transaction).toFile());

        transaction.end();
        Transactions.update(transaction);

        return result;
    }

    static boolean rollbackFile(String uri, Transaction transaction) throws IOException {
        boolean result = false;

        UriInfo uriInfo = findUri(uri, transaction);
        Path source = PathUtils.toPath(uri, Transactions.content(transaction));

        // We use a very broad exception catch clause to
        // ensure any and all commit errors are trapped
        try {

            // Delete the file
            Files.delete(source);
            uriInfo.rollback();

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
        Transactions.update(transaction);

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
