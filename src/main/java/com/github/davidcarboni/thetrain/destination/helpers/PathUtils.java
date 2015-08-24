package com.github.davidcarboni.thetrain.destination.helpers;

import com.github.davidcarboni.cryptolite.Crypto;

import javax.crypto.SecretKey;
import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.List;

import static com.github.davidcarboni.thetrain.destination.helpers.Hash.ShaInputStream;
import static com.github.davidcarboni.thetrain.destination.helpers.Hash.ShaOutputStream;

/**
 * Utility methods for dealing with paths and converting to/from URI strings.
 */
public class PathUtils {

    /**
     * Inspired by <a href="http://stackoverflow.com/questions/18227634/check-if-file-is-in-subdirectory"
     * >http://stackoverflow.com/questions/18227634/check-if-file-is-in-subdirectory</a>
     *
     * @param contained The path to be checked. This does not need to exist.
     * @param container The parent directory. This must exist on the filesystem.
     * @return If <code>contained</code> is a subfolder of <code>container</code>, true.
     * @throws IOException If an error occurs or, potentially, if either path does not exist.
     */
    public static boolean isContained(Path contained, Path container) throws IOException {
        Path current = contained.normalize();

        // Iterate up the path to see if we find the container path:
        while (current != null) {
            if (Files.isSameFile(container, current)) {
                return true;
            }
            current = current.getParent();
        }

        // If we didn't find the container path
        // amongst the parents of the contained path,
        // this path is not contained:
        return false;
    }

    /**
     * Computes a full path for a URI within a
     *
     * @param uri  The URI to be resolved
     * @param root The directory within which to resolve the URI.
     * @return A {@link Path} for the given URI, under the given root.
     */
    public static Path toPath(String uri, Path root) {
        String relative = stripLeadingSlash(uri);
        return root.resolve(relative);
    }

    /**
     * @param path The path to be rendered as a URI
     * @param root The path the URI should start from.
     * @return If path is contained within root, a URI relative to root (with leading slash).
     * @throws IOException If a filesystem error occurs.
     */
    public static String toUri(Path path, Path root) throws IOException {
        String result = null;

        if (isContained(path, root)) {
            Path relative = root.relativize(path);
            result = setLeadingSlash(relative.toString());
        }

        return result;
    }

    /**
     * Ensures the given string has a leading slash. This is useful for ensuring URIs always have a leading slash.
     *
     * @param string The value to be altered if necessary.
     * @return If the given string has a leading slash, the string is returned, otherwise a string with a slash prepended.
     */
    public static String setLeadingSlash(String string) {
        String result = string;
        if (result != null && !result.startsWith("/")) {
            result = "/" + result;
        }
        return result;
    }

    /**
     * Ensures the given string does not have a leading slash. This is useful for ensuring relative paths don't have a leading slash.
     *
     * @param string The value to be altered if necessary.
     * @return If the given string has no leading slash, the string is returned, otherwise a string with any leading slashes removed.
     */
    public static String stripLeadingSlash(String string) {
        String result = string;
        while (string != null && result.length() > 0 && result.startsWith("/")) {
            result = result.substring(1);
        }
        return result;
    }

    /**
     * Generates a {@link BufferedInputStream} for the given {@link Path}.
     *
     * @param file The file to be read.
     * @return A {@link BufferedInputStream}.
     * @throws IOException If an error occurs in getting the stream.
     */
    public static InputStream inputStream(Path file) throws IOException {
        return new BufferedInputStream(Files.newInputStream(file));
    }

    /**
     * Generates a {@link BufferedOutputStream} for the given {@link Path}.
     *
     * @param file The file to be read.
     * @return A {@link BufferedOutputStream}.
     * @throws IOException If an error occurs in getting the stream.
     */
    public static OutputStream outputStream(Path file) throws IOException {
        return new BufferedOutputStream(Files.newOutputStream(file));
    }

    /**
     * Generates a {@link BufferedInputStream} for the given {@link Path}, which will encrypt data as they are read if the given key is not null.
     *
     * @param file The file to be read.
     * @param key  The encryption key. If null, no encryption will be performed.
     * @return A {@link BufferedInputStream}, wrapped with a cipher stream if a key is provided, wrapped in an {@link ShaOutputStream}.
     * @throws IOException If an error occurs in getting the stream, or if the encryption key is invalid.
     */
    public static ShaOutputStream encryptingStream(Path file, SecretKey key) throws IOException {
        OutputStream result = outputStream(file);
        if (key != null) {
            try {
                result = new Crypto().encrypt(result, key);
            } catch (InvalidKeyException e) {
                throw new IOException("Error using encryption key", e);
            }
        }

        // NB the ShaOutputStream must process cleartext content as it is written to the stream, before being encrypted, in order to return the correct SHA:
        return new ShaOutputStream(result);
    }

    /**
     * Generates a {@link BufferedInputStream} for the given {@link Path}, which will decrypt data as they are read if the given key is not null.
     *
     * @param file The file to be read.
     * @param key  The encryption key. If null, no decryption will be performed.
     * @return A {@link BufferedInputStream}, wrapped with a cipher stream if a key is provided, wrapped in an {@link ShaOutputStream}.
     * @throws IOException If an error occurs in getting the stream, or if the encryption key is invalid.
     */
    public static ShaInputStream decryptingStream(Path file, SecretKey key) throws IOException {
        InputStream result = inputStream(file);
        if (key != null) {
            try {
                result = new Crypto().decrypt(result, key);
            } catch (InvalidKeyException e) {
                throw new IOException("Error using encryption key", e);
            }
        }

        // NB the ShaInputStream must process content read from the stream after decryption in order to return the correct SHA:
        return new ShaInputStream(result);
    }

    /**
     * Lists URIs relative to the given {@link Path}.
     *
     * @param content The path within which to list URIs.
     * @return A list of URIs for files only (not directories)>
     * @throws IOException If a filesystem error occurs.
     */
    public static List<String> listUris(Path content) throws IOException {
        List<Path> paths = listFiles(content);
        List<String> result = new ArrayList<>();
        for (Path path : paths) {
            String uri = toUri(path, content);
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
}
