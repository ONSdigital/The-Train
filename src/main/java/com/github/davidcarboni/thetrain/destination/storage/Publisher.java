package com.github.davidcarboni.thetrain.destination.storage;

import com.github.davidcarboni.thetrain.destination.json.Timing;
import com.github.davidcarboni.thetrain.destination.json.Transaction;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by david on 03/08/2015.
 */
public class Publisher {


    public static boolean addFile(Transaction transaction, String uri, InputStream input) throws IOException {
        Timing timing = new Timing(uri);
        boolean result = false;

        Path content = Transactions.content(transaction);
        Path file = path(content, uri);
        if (file != null) {
            Files.createDirectories(file.getParent());
            try (OutputStream output = Files.newOutputStream(file)) {
                IOUtils.copy(input, output);
            }
            result = true;
        }

        Transactions.addFile(transaction, timing);
        return result;
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

        Files.walkFileTree(path, new SimpleFileVisitor<Path>(){
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                result.add(file);
                return FileVisitResult.CONTINUE;
            }
        });

        return result;
    }

    /**
     * Determines the {@link Path} for the given uri.
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
     * @param parent The parent directory. This must exist on the filesystem.
     * @param path The path to be checked. This does not need to exist.
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

    public static void main(String[] args) throws IOException {
        Transactions.transactionStore = Paths.get(".");
        List<Path> src = listFiles(Paths.get("src"));
        System.out.println("src = " + src);
    }
}
