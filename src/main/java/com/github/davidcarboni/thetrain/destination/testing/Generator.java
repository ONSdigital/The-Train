package com.github.davidcarboni.thetrain.destination.testing;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by david on 30/07/2015.
 */
public class Generator {

    public static Path generate() throws IOException {
        Path thetrain = Files.createTempDirectory("thetrain");
        generate(thetrain);
        return thetrain;
    }

    static void generate(Path path) throws IOException {

        generateFiles(path);
        generateFolders(path);
    }

    static void generateFolders(Path path) throws IOException {

        // Generate some folders
        for (int folders = 0; folders < random(); folders++) {
            Path folder = path.resolve(name());
            Files.createDirectory(folder);
            generateFiles(folder);
        }
    }

    static void generateFiles(Path path) throws IOException {

        // Generate some files
        for (int files = 0; files < random(); files++) {
            Path file = path.resolve(name() + ".txt");
            try (OutputStream output = Files.newOutputStream(file); InputStream input = new ByteArrayInputStream(content().getBytes())) {
                IOUtils.copy(input, output);
            }
        }
    }

    /**
     * @return A random number between 5 and 10.
     */
    static int random() {
        double random = Math.random();
        return (int) ((random * 6) + 5);
    }

    static String name() {
        return com.github.davidcarboni.cryptolite.Random.password(random());
    }

    static String content() {
        return com.github.davidcarboni.cryptolite.Random.password(random() * 1000)+"\n";
    }

    //public static void main(String[] args) {
    //    for (int i = 0; i<100; i++) System.out.println(random());
    //}

    public static void main(String[] args) throws IOException {
        Path generate = generate();
        System.out.println("generate = " + generate);
    }
}
