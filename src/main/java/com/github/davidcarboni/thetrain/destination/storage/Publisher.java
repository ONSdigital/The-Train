package com.github.davidcarboni.thetrain.destination.storage;

import com.github.davidcarboni.thetrain.destination.helpers.Hash;
import com.github.davidcarboni.thetrain.destination.json.Timing;
import com.github.davidcarboni.thetrain.destination.json.Transaction;
import com.github.davidcarboni.thetrain.destination.testing.Generator;
import org.apache.commons.io.FileUtils;
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


    public static String addFile(Transaction transaction, String uri, Path data) throws IOException {
        Timing timing = new Timing(uri);
        String sha = null;

        Path content = Transactions.content(transaction);
        Path file = path(content, uri);
        if (file != null) {
            Files.createDirectories(file.getParent());
            Files.move(data, file, StandardCopyOption.REPLACE_EXISTING);
            sha = Hash.sha(file);
            System.out.println("Published " + sha + " " + uri);
        }

        transaction.addUri(timing.stop(sha));
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
        try {

            List<Path> files = listFiles(content);
            for (Path path : files) {
                 relative = content.relativize(path);
                target = website.resolve(relative);
                Path saved = backup.resolve(relative);
                if (Files.exists(target)) {
                    Files.createDirectories(saved.getParent());
                    Files.move(target, saved);
                }
                Files.createDirectories(target.getParent());
                Files.move(path, target);
                Timing timing = commit(relative, transaction);
                transaction.addUri(timing);
                Transactions.update(transaction);
            }

        } catch (Throwable t) {
            transaction.addError("Error committing '" + target + "'.\n" +
                    "Backed up files are in '" + backup + "'.\n" +
                    ExceptionUtils.getStackTrace(t));
        }
    }

    static Timing commit(Path relative, Transaction transaction) {

        String uri = relative.toString();
        if (!StringUtils.startsWith(uri, "/")) {
            uri = "/" + uri;
        }

        for (Timing timing : transaction.uris()) {
            if (StringUtils.equals(timing.uri, uri)) {
                return timing.commit();
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
            if (isContained(content, filePath)) {
                result = filePath;
            }
        }

        return result;
    }

    /**
     * Inspired by <a href="http://stackoverflow.com/questions/18227634/check-if-file-is-in-subdirectory"
     * >http://stackoverflow.com/questions/18227634/check-if-file-is-in-subdirectory</a>
     *
     * @param parent The parent directory. This must exist on the filesystem.
     * @param path   The path to be checked. This does not need to exist.
     * @return If path is under parent, true.
     * @throws IOException
     */
    static boolean isContained(Path parent, Path path) throws IOException {

        if (path == null)
            return false;

        if (Files.exists(path) && Files.isSameFile(parent.normalize(), path.normalize()))
            return true;

        return isContained(parent, path.normalize().getParent());
    }

    /**
     * Manual test of the commit functionality.
     *
     * @param args Not used.
     * @throws IOException If an error occurs.
     */
    public static void main(String[] args) throws IOException {

        // Generate a Transaction containing some content
        Transaction transaction = Transactions.create();
        System.out.println("Transaction: " + Transactions.path(transaction));
        Path generated = Generator.generate();
        System.out.println("Generated: " + generated);
        Files.move(generated, Transactions.content(transaction), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Moved to: " + Transactions.content(transaction));

        // Simulate the content already being on the website
        //Files.delete(Website.path());
        FileUtils.copyDirectory(Transactions.content(transaction).toFile(), Website.path().toFile());

        // Attempt to commit
        commit(transaction, Website.path());
        System.out.println("Committed to " + Website.path());

        // Print out
        System.out.println();
        System.out.println("Content : " + Transactions.content(transaction));
        System.out.println("Website : " + Website.path());
        System.out.println("Backup  : " + Transactions.backup(transaction));
    }
}
