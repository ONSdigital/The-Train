package com.github.onsdigital.thetrain.storage;

import com.github.davidcarboni.cryptolite.Random;
import com.github.onsdigital.thetrain.helpers.DateConverter;
import com.github.onsdigital.thetrain.json.Transaction;
import com.github.onsdigital.thetrain.json.UriInfo;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
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
    private Path transactionStore;
    private Path archivedTransactionStore;

    private final static String transaction0 = "ceddd961cc8e482be1bd98ab368878165adfd267a4a0568515cf690cf8a1df89";// Publishing
    private final static String startDate0 = "2020-01-10T13:33:43.101+0000"; //"2020-01-10T13:33:43.101+0000";
    private final static String endDate0 = null;

    private final static String transaction1 = "d1a2faa9ffa59c1aadc1d33611b557e653c4bbb68963f9cd240d8af22ec875b5";// Committed
    private final static String startDate1 = "2021-07-01T11:56:55.581+0100";
    private final static String endDate1 = null;

    private final static String transaction2 = "cf1eec88bf12737202a4c2dacdd85a6148f1bd8ad0ac0bcf4975ee0289566e8f";// Publishing
    private final static String startDate2 = "2021-10-10T13:04:11.310+0100";
    private final static String endDate2 = null;

    private final static String transaction3 = "ed9a71561351e18da0a4aa611c5d139fc532517931840c53737e5ade3e23fe4a";// Started
    private final static String startDate3 = "3021-01-01T09:04:11.310+0100";
    private final static String endDate3 = null;

    private final static String transaction4 = "6edd4c89b135fce22e2df3468685a847fd03ef0b72fe0b4224ccf8853973ff48";// Rolled Back
    private final static String startDate4 = "2020-11-17T08:39:06.992+0000";
    private final static String endDate4 = "2020-11-17T08:39:07.263+0000";

    private final static String transaction5 = "f7775e129d1458315d9908a8027bae3d9f7ce42b18db8a934129d272522e6b94";// Rolled Back
    private final static String startDate5 = "2020-11-09T07:41:52.568+0000";
    private final static String endDate5 = "2020-11-09T07:41:52.857+0000";

    private final static String transaction6 = "d988813e21b519f29eff98b0bf65fb08225892bb7498f94c7d2da41e6d696a71";// Started with StartDate in year 2999.
    private final static String startDate6 = "2999-07-13T20:13:52.533+0100";
    private final static String endDate6 = "2999-07-13T21:13:52.533+0100";


    @Mock
    Transactions transac=null;

    @Before
    public void setUp() throws IOException {
        transactionStore = Files.createTempDirectory("transaction-store");
        archivedTransactionStore = Files.createTempDirectory("archived-transaction");

        copyExampleTransactions(transactionStore);
        Transactions.init(transactionStore);

    }

    public void copyExampleTransactions(Path transactionStore) throws IOException {
        // Copy transactions to transaction store
        FileUtils.copyDirectory(new File(RELATIVE_PATH_TO_EXAMPLE_TRANSACTIONS), new File(transactionStore.toAbsolutePath().toString()), false);
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
        // Given
        // A set of Transactions
        // A folder of Transactions
        ArrayList<String> transactionsInFolder = Transactions.findTransactionsInFolder(transactionStore.toAbsolutePath().toString());
        // Check there are four transactions
        assertEquals(7,transactionsInFolder.size());
        // Check the three transactions are
        assertTrue(transactionsInFolder.contains(transaction0));
        assertTrue(transactionsInFolder.contains(transaction1));
        assertTrue(transactionsInFolder.contains(transaction2));
        assertTrue(transactionsInFolder.contains(transaction3));
        assertTrue(transactionsInFolder.contains(transaction4));
        assertTrue(transactionsInFolder.contains(transaction5));
        assertTrue(transactionsInFolder.contains(transaction6));
        // Check the following isn't located
        assertFalse(transactionsInFolder.contains("You can never find me!"));
    }

    /**
     * Tests that the EndDates are working correctly.
     */
    @Test
    public void checkEndDateOfTransactionsInFolder() throws IOException {
        // Given the transactions
        // Check transaction end dates for Transaction0
        Transaction a = Transactions.get(transaction0);
        assertEquals(endDate0, a.endDate());

        // Check transaction end dates for Transaction1
        a = Transactions.get(transaction1);
        assertEquals(endDate1, a.endDate());

        // Check transaction end dates for Transaction2
        a = Transactions.get(transaction2);
        assertEquals(endDate2, a.endDate());

        // Check transaction end dates for Transaction4
        a = Transactions.get(transaction3);
        assertEquals(endDate3, a.endDate());

        // Check transaction end dates for Transaction4
        a = Transactions.get(transaction4);
        assertEquals(endDate4, a.endDate());

        // Check transaction end dates for Transaction5
        a = Transactions.get(transaction5);
        assertEquals(endDate5, a.endDate());

        // Check transaction end dates for Transaction6
        a = Transactions.get(transaction6);
        assertEquals(endDate6, a.endDate());

        // Check transaction end dates for Transaction6
        a = Transactions.get(transaction6);
        assertEquals(endDate6, a.endDate());

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
    public void shouldNotUpdateTransactionAsyncWhenTransactionEnded() throws IOException {

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

    private Date addSubtractMinutesFromStringDate(String date, int minutes) {
        Date subtractFrom = DateConverter.toDate(date);
        Calendar cal = Calendar.getInstance();
        cal.setTime(subtractFrom);
        cal.add(Calendar.MINUTE, minutes);
        return cal.getTime();
    }

    @Test
    public void archiveBasedOnTimeThresholdTest() throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // Check which transaction folders are there
        ArrayList<String> transactionsInFolder = Transactions.findTransactionsInFolder(transactionStore.toAbsolutePath().toString());
        assertEquals(7,transactionsInFolder.size());

        // Given a Private and Static method archiveBasedOnTimeThreshold,
        // create a accessor to the method.
        Method archiveBasedOnTimeThreshold = Transactions.class.getDeclaredMethod("archiveBasedOnTimeThreshold", Transaction.class, Date.class);
        archiveBasedOnTimeThreshold.setAccessible(true);

        // Check that transaction should be flagged to archive one minute before the threshold
        Transaction transaction0 = Transactions.get(TransactionsTest.transaction0);
        assertTrue((boolean) archiveBasedOnTimeThreshold.invoke("archiveBasedOnTimeThreshold",
                Transactions.get(TransactionsTest.transaction0),
                addSubtractMinutesFromStringDate(transaction0.getStartDate(), 1)));

        // Check that transaction should NOT be flagged to archive if the date is the same as the transaction threshold
        assertFalse((boolean) archiveBasedOnTimeThreshold.invoke("archiveBasedOnTimeThreshold",
                Transactions.get(TransactionsTest.transaction0),
                addSubtractMinutesFromStringDate(transaction0.getStartDate(), 0)));

        // Check that transaction should NOT be flagged to archive one minute after the threshold
        assertFalse((boolean) archiveBasedOnTimeThreshold.invoke("archiveBasedOnTimeThreshold",
                Transactions.get(TransactionsTest.transaction0),
                addSubtractMinutesFromStringDate(transaction0.getStartDate(), -1)));

        // Given a transaction with a null endDate, check whether it should be archived
        // Check if the start date is the same as the threshold
        boolean result = (boolean) archiveBasedOnTimeThreshold.invoke("archiveBasedOnTimeThreshold",
                Transactions.get(TransactionsTest.transaction0),
                addSubtractMinutesFromStringDate(transaction0.getStartDate(), 0));
        assertFalse(result);

        // Given a transaction with a null endDate, check whether it should be archived
        // Check if the start date is just before the threshold
        result = (boolean) archiveBasedOnTimeThreshold.invoke("archiveBasedOnTimeThreshold",
                Transactions.get(TransactionsTest.transaction0),
                addSubtractMinutesFromStringDate(transaction0.getStartDate(), 1));
        assertTrue(result);

        // Given a transaction with a null endDate, check whether it should be archived
        // Check if the start date is just after the threshold
        result = (boolean) archiveBasedOnTimeThreshold.invoke("archiveBasedOnTimeThreshold",
                Transactions.get(TransactionsTest.transaction0),
                addSubtractMinutesFromStringDate(transaction0.getStartDate(), -1));
        assertFalse(result);
    }


    @Test
    public void archiveBasedOnTimeThreshold2Test() throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        // Given a Private and Static method archiveBasedOnTimeThreshold,
        // create a accessor to the method.
        Method archiveBasedOnTimeThreshold = Transactions.class.getDeclaredMethod("archiveBasedOnTimeThreshold", Transaction.class, Date.class);
        archiveBasedOnTimeThreshold.setAccessible(true);

        // Given a transaction with a non-null endDate, and a threshold which has passed the EndDate
        Transaction transaction6 = Transactions.get(TransactionsTest.transaction6);
        Calendar before = Calendar.getInstance();
        before.setTime(transaction6.getEndDateObject());
        before.add(Calendar.MINUTE, 1);
        Date beforeDate = before.getTime();

        // Check if the transaction is identified to be archived
        Boolean result = (boolean) archiveBasedOnTimeThreshold.invoke("archiveBasedOnTimeThreshold",
                Transactions.get(TransactionsTest.transaction6),
                beforeDate);
        assertTrue(result);

        Calendar after = Calendar.getInstance();
        before.setTime(transaction6.getEndDateObject());
        before.add(Calendar.MINUTE, -1);
        Date afterDate = after.getTime();

        // Check if the transaction is identified to be archived
        result = (boolean) archiveBasedOnTimeThreshold.invoke("archiveBasedOnTimeThreshold",
                Transactions.get(TransactionsTest.transaction6),
                afterDate);
        assertFalse(result);
    }

    @Test
    public void testArchiveTransactions() throws IOException {
        // Given a set of Transactions in a folder
        // Check to make sure they are all there first.
        ArrayList<String> transactionsInFolder = Transactions.findTransactionsInFolder(transactionStore.toAbsolutePath().toString());
        assertEquals(7,transactionsInFolder.size());

        System.out.println("transactionToBeProcessed folder = " + transactionStore);
        System.out.println("archivedTransactionStore folder = " + archivedTransactionStore);

        // When The-Train
        // Initialiases with two transaction that are so far into the future they should not be archived.
        Transactions.init(transactionStore);
        Transactions.archiveTransactions(transactionStore, archivedTransactionStore, Duration.ofMinutes(10));
        // Check that the only transaction that is left to be processed is the one with start date in the future
        transactionsInFolder = Transactions.findTransactionsInFolder(transactionStore.toAbsolutePath().toString());
        assertEquals(2,transactionsInFolder.size());
        assertTrue(transactionsInFolder.contains(transaction6));
    }
}