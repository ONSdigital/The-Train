package com.github.onsdigital.thetrain.storage;

import com.github.davidcarboni.cryptolite.Random;
import com.github.onsdigital.thetrain.configuration.ConfigurationException;
import com.github.onsdigital.thetrain.json.Transaction;
import com.github.onsdigital.thetrain.json.UriInfo;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.test.context.event.annotation.BeforeTestExecution;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.*;

/**
 * Test for {@link Transactions}.
 */
public class TransactionsTest {
    private final String RELATIVE_PATH_TO_EXAMPLE_TRANSACTIONS = "src/test/resources/transactions";
    private Path pathOfTransactions;
    private Path transactionToBeProcessed;
    private Path archivedTransactionStore;
    private final static String transaction0 = "ceddd961cc8e482be1bd98ab368878165adfd267a4a0568515cf690cf8a1df89";// Publishing
    private final static String transaction1 = "d1a2faa9ffa59c1aadc1d33611b557e653c4bbb68963f9cd240d8af22ec875b5";// Committed
    private final static String transaction2 = "cf1eec88bf12737202a4c2dacdd85a6148f1bd8ad0ac0bcf4975ee0289566e8f";// Publishing
    private final static String transaction3 = "ed9a71561351e18da0a4aa611c5d139fc532517931840c53737e5ade3e23fe4a";// Started
    private final static String transaction4 = "6edd4c89b135fce22e2df3468685a847fd03ef0b72fe0b4224ccf8853973ff48";// Rolled Back
    private final static String transaction5 = "f7775e129d1458315d9908a8027bae3d9f7ce42b18db8a934129d272522e6b94";// Rolled Back
    private final static String transaction6 = "d988813e21b519f29eff98b0bf65fb08225892bb7498f94c7d2da41e6d696a71";// Started with StartDate in year 2999.

    @Mock
    Transactions transac=null;

    @Before
    public void setUp() throws IOException {
        // Create temp folder for Transactions
        transactionToBeProcessed = Files.createTempDirectory("to-be-processed-transaction");
        archivedTransactionStore = Files.createTempDirectory("archived-transaction");
        // Initialise Trnsactions
        Transactions.init(transactionToBeProcessed, archivedTransactionStore);
        // Create Transactions mock
        transac = org.mockito.Mockito.mock(Transactions.class);
    }

    @BeforeTestExecution
    public void copyExampleTransactions() throws IOException {
        // Copy transactions to transaction store
        FileUtils.copyDirectory(new File(RELATIVE_PATH_TO_EXAMPLE_TRANSACTIONS), new File(transactionToBeProcessed.toAbsolutePath().toString()), false);
    }


    /**
     * Tests that a transaction is created with an ID and start date and can be read using the ID.
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
        assertTrue(StringUtils.isNotBlank(transaction.id()));
        assertTrue(StringUtils.isNotBlank(transaction.startDate()));
        assertTrue(transaction.getStatus().equals(Transaction.STARTED));
        assertNotNull(Transactions.get(transaction.id()));
    }

    /**
     * Tests that a collection is created with an ID and start date and can be read using the ID.
     */
    @Test
    public void checkTransactionsInFolder() throws IOException {
        copyExampleTransactions();
        // Given
        // A set of Transactions
        // A folder of Transactions
        ArrayList<String> transactionsInFolder = Transactions.findTransactionsInFolder(transactionToBeProcessed.toAbsolutePath().toString());
        // Check there are three transactions
        assertEquals(6,transactionsInFolder.size());
        // Check the three transactions are
        assertTrue(transactionsInFolder.contains(transaction0));
        assertTrue(transactionsInFolder.contains(transaction1));
        assertTrue(transactionsInFolder.contains(transaction2));
        assertTrue(transactionsInFolder.contains(transaction3));
        // Check the following isn't located
        assertFalse(transactionsInFolder.contains("You can never find me!"));
    }

    /**
     * Tests that a collection is created with an ID and start date and can be read using the ID.
     */
    @Test
    public void checkEndDateOfTransactionsInFolder() throws IOException {
        copyExampleTransactions();
        // Given
        // A folder of Transactions
        ArrayList<String> transactionsInFolder = Transactions.findTransactionsInFolder(transactionToBeProcessed.toAbsolutePath().toString());
        // Check there are three transactions
        assertEquals(6,transactionsInFolder.size());
        // Check the three transactions EndDate
        //
        for (int i=0; i<=5; i++) {
            String id = transactionsInFolder.get(i);
            Transaction t = Transactions.get(id);
            System.out.println("endDate = " + t.startDate());
            assertEquals(t.getStartDate(), t.startDate());
        }
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
        Transaction got = Transactions.get(transaction.id());

        // Then
        // The read transaction should contain the expected values:
        assertNotNull(got);
        assertEquals(transaction.id(), got.id());
        assertEquals(transaction.startDate(), got.startDate());
    }

    /**
     * Tests that a collection is created with an ID and start date and can be read using the ID.
     */
    @Test
    public void shouldUpdateTransaction() throws IOException {

        // Given
        // A transaction
        Transaction transaction = Transactions.create();
        UriInfo uriInfo = new UriInfo("/uri.txt");
        String error = "error";
        transaction.addError(error);
        transaction.addUri(uriInfo);

        // When
        // We update the transaction
        Transactions.update(transaction);

        // Then
        // Re-reading the Transaction should load the updates
        synchronized (Transactions.getTransactionMap()) {
            // So we can run tests in parallel
            Transactions.getTransactionMap().clear();
            Transaction read = Transactions.get(transaction.id());
            assertEquals(1, read.errors().size());
            assertEquals(1, read.uris().size());
            assertTrue(read.errors().contains(error));
            assertTrue(read.uris().contains(uriInfo));
        }
    }

    /**
     * Tests that a transaction can be created and ended.
     */
    @Test
    public void shouldEndTransaction() throws IOException {

        // Given
        // A transaction
        Transaction transaction = Transactions.create();

        assertTrue(Transactions.getTransactionMap().containsKey(transaction.id()));
        assertTrue(Transactions.getTransactionExecutorMap().containsKey(transaction.id()));

        // When
        // We end the transaction
        Transactions.end(transaction);

        // Then
        // The transaction maps should not contain entries for the transaction
        assertFalse(Transactions.getTransactionMap().containsKey(transaction.id()));
        assertFalse(Transactions.getTransactionExecutorMap().containsKey(transaction.id()));
    }

    /**
     * Tests that a transaction can be created and ended.
     */
    @Test
    public void shouldUpdateTransactionAsync() throws IOException, ExecutionException, InterruptedException {

        // Given
        // A transaction
        Transaction transaction = Transactions.create();

        // When
        // We update the transaction using the async method
        Future<Boolean> future = Transactions.tryUpdateAsync(transaction.id());
        boolean result = future.get();

        // Then
        // The response should be true as the update succeeds.
        assertTrue(result);

        Transactions.end(transaction);
    }

    /**
     * Tests that a transaction can be created and ended.
     */
    @Test
    public void shouldNotUpdateTransactionAsyncWhenTransactionEnded() throws IOException, ExecutionException, InterruptedException {

        // Given
        // A transaction
        Transaction transaction = Transactions.create();

        // When
        // We update the transaction using the async method when the transaction is ended.
        Transactions.end(transaction);
        Future<Boolean> future = Transactions.tryUpdateAsync(transaction.id());

        // Then
        // The response should be null;
        assertNull(future);

    }

    /**
     * Tests that a collection is created with an ID and start date and can be read using the ID.
     */
    @Test
    public void shouldUpdateTransactionConcurrently() throws IOException, InterruptedException {
        // Given
        // A transaction and lots of URI infos and errors
        final Transaction transaction = Transactions.create();
        Set<UriInfo> uriInfos = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            uriInfos.add(new UriInfo("/" + Random.id()));
        }
        Set<String> errors = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            errors.add("error " + Random.id());
        }
        ExecutorService pool = Executors.newFixedThreadPool(100);

        // When
        // We add all the URI infos and errors to the Transaction
        for (final UriInfo uriInfo : uriInfos) {
            pool.submit(new Runnable() {
                @Override
                public void run() {
                    transaction.addUri(uriInfo);
                    try {
                        //System.out.println("Updating "+UriInfo);
                        Transactions.update(transaction);
                    } catch (IOException e) {
                        System.out.println("Error adding URI info to transaction (" + uriInfo + ")");
                    }
                }
            });
        }
        for (final String error : errors) {
            pool.submit(new Runnable() {
                @Override
                public void run() {
                    transaction.addError(error);
                    try {
                        //System.out.println("Updating "+error);
                        Transactions.update(transaction);
                    } catch (IOException e) {
                        System.out.println("Error adding error to transaction (" + error + ")");
                    }
                }
            });
        }

        // Then
        // Wait for updates to complete
        pool.shutdown();
        pool.awaitTermination(10, SECONDS);

        // Clear the transaction map so we can double-check the Json serialised correctly when it's re-read:
        synchronized (Transactions.getTransactionMap()) {
            // So we can run tests in parallel
            Transactions.getTransactionMap().clear();
        }

        // We should have added all URI infos to the Transaction and not lost any
        assertEquals(uriInfos.size(), transaction.uris().size());
        assertEquals(errors.size(), transaction.errors().size());
        for (UriInfo uriInfo : transaction.uris()) {
            Assert.assertTrue(uriInfos.contains(uriInfo));
        }
        for (String error : transaction.errors()) {
            Assert.assertTrue(error.contains(error));
        }
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
    public void shouldGetContentPath() throws IOException {

        // Given
        // A transaction
        Transaction transaction = Transactions.create();

        // When
        // We get the content staging path
        Path content = Transactions.content(transaction);

        // Then
        // The transaction should contain a content directory:
        assertNotNull(content);
        assertTrue(Files.exists(content));
        assertTrue(Files.isDirectory(content));
        assertEquals(Transactions.CONTENT, content.getFileName().toString());
    }

    @Test
    public void shouldNotGetNonexistentContentPath() throws IOException {

        // Given
        // A nonexistent transaction ID (ie not created on disk)
        Transaction transaction = new Transaction();

        // When
        // We get the content staging path
        Path content = Transactions.content(transaction);

        // Then
        // We should get null and no error
        assertNull(content);
    }

    @Test
    public void shouldGetBackupPath() throws IOException {

        // Given
        // A transaction
        Transaction transaction = Transactions.create();

        // When
        // We get the backup path
        Path backup = Transactions.backup(transaction);

        // Then
        // The transaction should contain a content directory:
        assertNotNull(backup);
        assertTrue(Files.exists(backup));
        assertTrue(Files.isDirectory(backup));
        assertEquals(Transactions.BACKUP, backup.getFileName().toString());
    }

    @Test
    public void shouldNotGetNonexistentBackupPath() throws IOException {

        // Given
        // A nonexistent transaction ID (ie not created on disk)
        Transaction transaction = new Transaction();

        // When
        // We get the backup path
        Path backup = Transactions.backup(transaction);

        // Then
        // We should get null and no error
        assertNull(backup);
    }

    @Test
    public void testArchiveTransactions() throws IllegalAccessException, NoSuchFieldException, ClassNotFoundException, ConfigurationException, IOException {
        // Copy the transactions to the working directory
        copyExampleTransactions();
        // Check which transaction folders are there
        ArrayList<String> transactionsInFolder = Transactions.findTransactionsInFolder(transactionToBeProcessed.toAbsolutePath().toString());
        assertEquals(7,transactionsInFolder.size());

        System.out.println("transactionToBeProcessed folder = " + transactionToBeProcessed);
        System.out.println("archivedTransactionStore folder = " + archivedTransactionStore);

        // When The-Train
        // Initialiases with one transaction having a start date of 2999-07-13
        Transactions.init(transactionToBeProcessed, archivedTransactionStore);
        // Check that the only transaction that is left to be processed is the one with start date in the future
        transactionsInFolder = Transactions.findTransactionsInFolder(transactionToBeProcessed.toAbsolutePath().toString());
        assertEquals(1,transactionsInFolder.size());
        assertTrue(transactionsInFolder.contains(transaction6));
    }
}