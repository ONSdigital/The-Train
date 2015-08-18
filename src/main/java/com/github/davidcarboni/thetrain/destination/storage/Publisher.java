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
import java.util.Date;
import java.util.List;

/**
 * Created by david on 03/08/2015.
 */
public class Publisher {


    public static String addFile(Transaction transaction, String uri, Path data, Date startDate) throws IOException {
        String sha = null;

        Path content = Transactions.content(transaction);
        Path file = path(content, uri);
        if (file != null) {
            Files.createDirectories(file.getParent());
            Files.move(data, file, StandardCopyOption.REPLACE_EXISTING);
            sha = Hash.sha(file);
            System.out.println("Staged " + sha + " " + uri);
        }

        transaction.addUri(new Uri(uri, startDate).stop(sha));
        Transactions.update(transaction);
        return sha;
    }

    public static Path getFile(Transaction transaction, String uri) throws IOException {
        Path result = null;

        Path content = Transactions.content(transaction);
        Path file = path(content, uri);
        if (file != null && Files.exists(file) && Files.isRegularFile(file)) {
            result = file;
        }

        return file;
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
            Path relative = content.relativize(path);
            result.add("/"+relative);
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
        Path relative = null;

        List<Path> files = listFiles(content);
        for (Path path : files) {
            Uri uri = null;
            try {
                relative = content.relativize(path);
                target = website.resolve(relative);
                Path saved = backup.resolve(relative);
                uri = findUri(relative, transaction);
                if (Files.exists(target)) {
                    Files.createDirectories(saved.getParent());
                    Files.move(target, saved);
                }
                Files.createDirectories(target.getParent());
                Files.move(path, target);
                uri.commit();
                Transactions.update(transaction);

            } catch (Throwable t) {
                String error = "Error committing '" + target + "'.\n" +
                        "Backed up files are in '" + backup + "'.\n" +
                        ExceptionUtils.getStackTrace(t);
                transaction.addError(error);
                if (uri !=null) {
                    uri.error(error);
                }
            }
        }
    }

    static Uri findUri(Path relative, Transaction transaction) {

        String uriString = relative.toString();
        if (!StringUtils.startsWith(uriString, "/")) {
            uriString = "/" + uriString;
        }
        Uri relativeUri = new Uri(uriString, new Date());

        for (Uri uri : transaction.uris()) {
            if (uri.equals(relativeUri)) {
                return uri;
            }
        }

        return null;
    }

    /**
     * Determines the {@link Path} for the given uri.
     *
     * @param uri a path to be resolved within the transaction content.
     * @return A {@link Path} within the transaction content folder if the uri can be relativized (sic.).
     */
    static Path path(Path content, String uri) throws IOException {
        Path result = null;

        if (!StringUtils.isBlank(uri) && !uri.matches("/+")) {
            String relativePath = uri;
            while (relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }
            Path filePath = content.resolve(relativePath);
            if (PathUtils.isContained(filePath, content)) {
                result = filePath;
            }
        }

        return result;
    }
}
