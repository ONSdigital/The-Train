package com.github.davidcarboni.thetrain.destination.storage;

import com.github.davidcarboni.cryptolite.Random;
import com.github.davidcarboni.thetrain.destination.helpers.Hash;
import com.github.davidcarboni.thetrain.destination.json.Transaction;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Created by david on 03/08/2015.
 */
public class PublisherTest {

    Transaction transaction;

    @Before
    public void setUp() throws Exception {
        transaction = Transactions.create();
    }


    @Test
    public void shouldPublishFile() throws IOException {

        // Given
        // A file and a URI to copy to
        Path file = tempFile();
        String uri = "/test.txt";

        // When
        // We publish the file
        //try (InputStream input = Files.newInputStream(file)) {
            Publisher.addFile(transaction, uri, file);
        //}

        // Then
        // The transaction should exist and be populated with values
        Path path = Publisher.getFile(transaction, uri);
        assertNotNull(path);
    }


    @Test
    public void shouldComputeHash() throws IOException {

        // Given
        // A file and a URI to copy to
        Path file = tempFile();
        String sha = Hash.hash(file);
        String uri = "/test.txt";

        // When
        // We publish the file
        //try (InputStream input = Files.newInputStream(file)) {
        Publisher.addFile(transaction, uri, file);
        //}

        // Then
        // The transaction should exist and be populated with values
        Path path = Publisher.getFile(transaction, uri);
        assertNotNull(path);
    }


    @Test
    public void shouldGetFile() throws IOException {

        // Given
        // A published file
        Path file = tempFile();
        String uri = "/greeneggs.txt";
        //try (InputStream input = Files.newInputStream(file)) {
            Publisher.addFile(transaction, uri, file);
        //}

        // When
        // We get the file
        Path path = Publisher.getFile(transaction, "greeneggs.txt");

        // Then
        // The transaction should exist and be populated with values
        assertNotNull(path);
    }


    @Test
    public void shouldHandleSlashes() throws IOException {

        // Given
        // Files with inconsistent leading slashes
        Path file0 = tempFile();
        Path file1 = tempFile();
        Path file2 = tempFile();
        String zero = "zero.txt";
        String one = "/one.txt";
        String two = "//two.txt";

        // When
        // We publish the files
        //try (InputStream input = Files.newInputStream(file)) {
            Publisher.addFile(transaction, zero, file0);
        //}
        //try (InputStream input = Files.newInputStream(file)) {
        Publisher.addFile(transaction, one, file1);
        //}
        //try (InputStream input = Files.newInputStream(file)) {
            Publisher.addFile(transaction, two, file2);
        //}

        // Then
        // The transaction should exist and be populated with values
        Path pathZero = Publisher.getFile(transaction, "/zero.txt");
        Path pathOne = Publisher.getFile(transaction, "/one.txt");
        Path pathTwo = Publisher.getFile(transaction, "/two.txt");
        assertNotNull(pathZero);
        assertNotNull(pathOne);
        assertNotNull(pathTwo);
    }


    @Test
    public void shouldHandleSubdirectories() throws IOException {

        // Given
        // Files with inconsistent leading slashes
        Path file1 = tempFile();
        Path file2 = tempFile();
        String sub = "/folder/sub.txt";
        String subsub = "/another/directory/subsub.txt";

        // When
        // We publish the files
        //try (InputStream input = Files.newInputStream(file)) {
            Publisher.addFile(transaction, sub, file1);
        //}
        //try (InputStream input = Files.newInputStream(file)) {
            Publisher.addFile(transaction, subsub, file2);
        //}

        // Then
        // The transaction should exist and be populated with values
        Path pathSub = Publisher.getFile(transaction, sub);
        Path pathSubsub = Publisher.getFile(transaction, subsub);
        assertNotNull(pathSub);
        assertNotNull(pathSubsub);
    }


    @Test
    public void shouldListFilesOnly() throws IOException {

        // Given
        // Files in the transaction
        Path file1 = tempFile();
        Path file2 = tempFile();
        //try (InputStream input = Files.newInputStream(file1)) {
            Publisher.addFile(transaction, "/folder1/file1.txt", file1);
        //}
        //try (InputStream input = Files.newInputStream(file2)) {
            Publisher.addFile(transaction, "/folder2/file2.txt", file2);
        //}

        // When
        // We list the files
        List<Path> files = Publisher.listFiles(transaction);

        // Then
        // We should get the files, but not the folders
        assertEquals(2, files.size());
        Set<String> filenames = new HashSet<>();
        filenames.add(files.get(0).getFileName().toString());
        filenames.add(files.get(1).getFileName().toString());
        assertTrue(filenames.contains("file1.txt"));
        assertTrue(filenames.contains("file2.txt"));
    }


    private Path tempFile() throws IOException {

        // A temp file
        Path file = Files.createTempFile(this.getClass().getSimpleName(), ".txt");

        //
        try (OutputStream output = Files.newOutputStream(file); InputStream input = new ByteArrayInputStream(Random.password(5000).getBytes())) {
            IOUtils.copy(input, output);
        }

        return file;
    }
}