package com.github.davidcarboni.thetrain.destination.storage;

import com.github.davidcarboni.thetrain.destination.json.Transaction;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

/**
 * Created by david on 03/08/2015.
 */
public class TransactionsTest {

    @Before
    public void setUp() {

    }

    @After
    public void tearDown() {

    }

    /**
     * Tests that a collection is created with an ID and start date and can be read using the ID.
     */
    @Test
    public void shouldCreateTransaction() throws IOException {

        // Given
        // No transaction yet

        // When
        // We create a transaction
        Transaction transaction = Transactions.create();

        // Then
        // The transaction should exist and be populated with values
        assertNotNull(transaction);
        assertTrue(StringUtils.isNotBlank(transaction.id));
        assertTrue(StringUtils.isNotBlank(transaction.startDate));
        assertNotNull(Transactions.get(transaction.id));
    }

    /**
     * Tests that a collection is created with an ID and start date and can be read using the ID.
     */
    @Test
    public void shouldGetTransaction() throws IOException {

        // Given
        // A transaction
        Transaction transaction = Transactions.create();

        // When
        // We get the transaction
        Transaction got = Transactions.get(transaction.id);

        // Then
        // The read transaction should contain the expected values:
        assertNotNull(got);
        assertEquals(transaction.id, got.id);
        assertEquals(transaction.startDate, got.startDate);
    }

    /**
     * Tests that a collection is created with an ID and start date and can be read using the ID.
     */
    @Test
    public void shouldNotGetNonexistentTransaction() throws IOException {

        // Given
        // A nonexistent transaction ID
        String id = "Not here, mate";

        // When
        // We get with the ID
        Transaction got = Transactions.get(id);

        // Then
        // We should get null and no error
        assertNull(got);
    }

    /**
     * Tests that a collection is created with an ID and start date and can be read using the ID.
     */
    @Test
    public void shouldHandleNullTransactionId() throws IOException {

        // Given
        // A null transaction ID
        String id = null;

        // When
        // We get with the ID
        Transaction got = Transactions.get(id);

        // Then
        // We should get null and no error
        assertNull(got);
    }

    @Test
    public void shouldGetFilesPath() throws IOException {

        // Given
        // A transaction
        Transaction transaction = Transactions.create();

        // When
        // We get the files path
        Path files = Transactions.content(transaction);

        // Then
        // The transaction should contain a content directory:
        assertNotNull(files);
        assertTrue(Files.exists(files));
        assertTrue(Files.isDirectory(files));
    }

    @Test
    public void shouldNotGetNonexistentFilesPath() throws IOException {

        // Given
        // A nonexistent transaction ID
        String id = "Still not here, mate";
        Transaction transaction = new Transaction();
        transaction.id = id;

        // When
        // We get the files path
        Path files = Transactions.content(transaction);

        // Then
        // We should get null and no error
        assertNull(files);
    }
}