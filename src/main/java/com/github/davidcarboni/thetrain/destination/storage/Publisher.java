package com.github.davidcarboni.thetrain.destination.storage;

import com.github.davidcarboni.thetrain.destination.helpers.Hash;
import com.github.davidcarboni.thetrain.destination.helpers.PathUtils;
import com.github.davidcarboni.thetrain.destination.json.Transaction;
import com.github.davidcarboni.thetrain.destination.json.Uri;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by david on 03/08/2015.
 */
public class Publisher {


    public static String addFile(Transaction transaction, Uri uri, Path data) throws IOException {
        String sha = null;

        Path content = Transactions.content(transaction);
        Path file = PathUtils.toPath(uri, content);
        if (file != null) {
            Files.createDirectories(file.getParent());
            Files.move(data, file, StandardCopyOption.REPLACE_EXISTING);
            sha = Hash.sha(file);
            System.out.println("Staged " + sha + " " + uri);
        }

        uri.stop(sha);
        transaction.addUri(uri);
        Transactions.update(transaction);
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

    /**
     * Lists all files in this {@link Transaction}.
     *
     * @param transaction The {@link Transaction}
     * @return The list of files (directories are not included).
     * @throws IOException If an error occurs.
     */
    public static List<Path> listFiles(Transaction transaction) throws IOException {
        Path content = Transactions.content(transaction);
        return listFiles(content);
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

    public static void commit(Transaction transaction, Path website) throws IOException {
        Transaction result = null;

        // We use a very broad exception catch clause to
        // ensure any and all commit errors are trapped
        Path target = null;
        Path content = Transactions.content(transaction);
        Path backup = Transactions.backup(transaction);

        List<Path> files = listFiles(content);
        for (Path source : files) {
            String uri = PathUtils.toUri(source, content);
            Uri uriInfo = findUri(uri, transaction);
            target = PathUtils.toPath(uri, website);
            Path backedUp = PathUtils.toPath(uri, backup);
            commitFile(transaction, uriInfo, source, target, backedUp);
            Transactions.update(transaction);
        }
    }

    static void commitFile(Transaction transaction, Uri uriInfo, Path source, Path target, Path backedUp) {

        try {

            // Back up the existing file, if present
            if (Files.exists(target)) {
                Files.createDirectories(backedUp.getParent());
                Files.move(target, backedUp);
            }

            // Publish the new file
            Files.createDirectories(target.getParent());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            uriInfo.commit();

        } catch (Throwable t) {

            // Record the error
            String error = "Error committing '" + source + "' to '" + target + "'.\n";
            if (Files.exists(backedUp)) error += "\nBacked up file is '" + backedUp + "'.\n";
            error += ExceptionUtils.getStackTrace(t);
            if (uriInfo != null) {
                uriInfo.error(error);
            } else {
                transaction.addError(error);
            }

        }
    }

    static Uri findUri(String uri, Transaction transaction) {

        for (Uri transactionUri : transaction.uris()) {
            if (StringUtils.equals(uri, transactionUri.uri())) {
                return transactionUri;
            }
        }

        // We didn't find the requested URI:
        return null;
    }
}
