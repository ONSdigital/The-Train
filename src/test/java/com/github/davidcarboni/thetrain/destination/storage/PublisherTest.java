package com.github.davidcarboni.thetrain.destination.storage;

import com.github.davidcarboni.cryptolite.Random;
import com.github.davidcarboni.thetrain.destination.helpers.Hash;
import com.github.davidcarboni.thetrain.destination.helpers.PathUtils;
import com.github.davidcarboni.thetrain.destination.json.Transaction;
import com.github.davidcarboni.thetrain.destination.json.UriInfo;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

/**
 * Test for {@link Publisher}.
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
        Publisher.addFile(transaction, uri, file);

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
        String uri = "/test.txt";

        // When
        // We publish the file
        Publisher.addFile(transaction, uri, file);

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
        Publisher.addFile(transaction, uri, file);

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
        Publisher.addFile(transaction, zero, file0);
        Publisher.addFile(transaction, one, file1);
        Publisher.addFile(transaction, two, file2);

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
        Publisher.addFile(transaction, sub, file1);
        Publisher.addFile(transaction, subsub, file2);

        // Then
        // The transaction should exist and be populated with values
        Path pathSub = Publisher.getFile(transaction, sub);
        Path pathSubsub = Publisher.getFile(transaction, subsub);
        assertNotNull(pathSub);
        assertNotNull(pathSubsub);
    }


    @Test
    public void shouldCommitTransaction() throws IOException {

        // Given

        // A transaction
        Transaction transaction = Transactions.create();
        Path content = Transactions.content(transaction);
        Path backup = Transactions.backup(transaction);
        Path website = Website.path();

        // Files being published
        String create = "/create-" + Random.id() + ".txt";
        String update = "/update-" + Random.id() + ".txt";
        Publisher.addFile(transaction, create, tempFile());
        Publisher.addFile(transaction, update, tempFile());

        // An existing file on the website
        Files.move(tempFile(), PathUtils.toPath(update, website));


        // When
        // We commit the transaction
        Publisher.commit(transaction, website);


        // Then

        // The published files should be on the website
        assertTrue(Files.exists(PathUtils.toPath(create, website)));
        assertTrue(Files.exists(PathUtils.toPath(update, website)));
        assertEquals(Hash.sha(PathUtils.toPath(create, content)),
                Hash.sha(PathUtils.toPath(create, website)));
        assertEquals(Hash.sha(PathUtils.toPath(update, content)),
                Hash.sha(PathUtils.toPath(update, website)));

        // Only the replaced file should be backed up - and we should see that the backed up content is different
        assertFalse(Files.exists(PathUtils.toPath(create, backup)));
        assertTrue(Files.exists(PathUtils.toPath(update, backup)));
        assertNotEquals(Hash.sha(PathUtils.toPath(update, backup)),
                Hash.sha(PathUtils.toPath(update, website)));

        // Check the transaction details
        assertTrue(StringUtils.isNotBlank(transaction.startDate()));
        assertTrue(StringUtils.isNotBlank(transaction.endDate()));
        assertEquals(0, transaction.errors().size());
        assertEquals(2, transaction.uris().size());
        assertTrue(transaction.uris().contains(new UriInfo(create)));
        assertTrue(transaction.uris().contains(new UriInfo(update)));
        for (UriInfo uriInfo : transaction.uris()) {
            assertTrue(StringUtils.isBlank(uriInfo.error()));
        }
    }

    private Path tempFile() throws IOException {

        // A temp file
        Path file = Files.createTempFile(this.getClass().getSimpleName(), ".txt");

        try (OutputStream output = Files.newOutputStream(file); InputStream input = new ByteArrayInputStream(Random.password(5000).getBytes())) {
            IOUtils.copy(input, output);
        }

        return file;
    }
}