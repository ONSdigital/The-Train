package com.github.davidcarboni.thetrain.destination.Testing;

import com.github.davidcarboni.thetrain.destination.json.Transaction;
import com.github.davidcarboni.thetrain.destination.storage.Publisher;
import com.github.davidcarboni.thetrain.destination.storage.Transactions;
import com.github.davidcarboni.thetrain.destination.storage.Website;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

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
        return com.github.davidcarboni.cryptolite.Random.password(random() * 1000) + "\n";
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
        System.out.println("Transaction: " + Transactions.content(transaction).getParent());
        Path generated = Generator.generate();
        System.out.println("Generated: " + generated);
        Files.move(generated, Transactions.content(transaction), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Moved to: " + Transactions.content(transaction));

        // Simulate the content already being on the website
        //Files.delete(Website.path());
        FileUtils.copyDirectory(Transactions.content(transaction).toFile(), Website.path().toFile());

        // Attempt to commit
        Publisher.commit(transaction, Website.path());
        System.out.println("Committed to " + Website.path());

        // Print out
        System.out.println();
        System.out.println("Content : " + Transactions.content(transaction));
        System.out.println("Website : " + Website.path());
        System.out.println("Backup  : " + Transactions.backup(transaction));
    }
}
