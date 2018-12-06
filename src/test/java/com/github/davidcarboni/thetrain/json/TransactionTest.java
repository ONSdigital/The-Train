package com.github.davidcarboni.thetrain.json;

import com.github.davidcarboni.cryptolite.Random;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test for {@link Transaction}.
 */
public class TransactionTest {

    @Test
    public void shouldGenerateIdAndStartDateWhenInstantiated() throws Exception {

        // Given
        // A transaction
        Transaction transaction;

        // When
        // We instantiate
        transaction = new Transaction();

        // Then
        // A random ID should be generated and the start date should be set
        assertTrue(StringUtils.isNotBlank(transaction.id()));
        assertTrue(StringUtils.isNotBlank(transaction.startDate()));
        assertEquals(Transaction.STARTED, transaction.getStatus());
    }

    @Test
    public void shouldAddUri() throws Exception {

        // Given
        // A transaction and a URI info
        Transaction transaction = new Transaction();
        UriInfo uriInfo = new UriInfo("test");

        // When
        // We add the URI info to the Transaction
        transaction.addUri(uriInfo);

        // Then
        // We should have one URI info in the collection
        assertEquals(1, transaction.uris().size());
        assertEquals(Transaction.PUBLISHING, transaction.getStatus());
    }

    @Test
    public void shouldAddUrisConcurrently() throws InterruptedException, ExecutionException {
        ExecutorService pool = null;

        try {
            pool = Executors.newFixedThreadPool(100);

            final Transaction transaction = new Transaction();
            final int totalUris = 5000;

            Set<UriInfo> uriInfos = IntStream.range(0, totalUris)
                    .boxed()
                    .map(String::valueOf)
                    .map(s -> new UriInfo("/" + s))
                    .collect(Collectors.toSet());


            List<Callable<Boolean>> jobs = uriInfos.stream()
                    .map(uri -> (Callable<Boolean>) () -> {
                        transaction.addUri(uri);
                        return true;
                    }).collect(Collectors.toList());


            for (Future<Boolean> f : pool.invokeAll(jobs)) {
                assertTrue(f.get());
            }

            Set<UriInfo> actual = transaction.uris();
            uriInfos.stream()
                    .filter(e -> !actual.contains(e))
                    .findAny()
                    .ifPresent(e -> fail("expected uri was missing from transaction uris: " + e));

            assertEquals(totalUris, actual.size());
        } finally {
            if (pool != null) {
                pool.shutdown();
                pool.awaitTermination(10, SECONDS);
            }
        }
    }


    @Test(expected = UnsupportedOperationException.class)
    public void shouldNotBeAbleToModifyUris() {

        // Given
        // A transaction
        Transaction transaction = new Transaction();
        transaction.addUri(new UriInfo("test"));

        // When
        // We attempt to modify the URIs
        transaction.uris().clear();

        // Then
        // We should get an exception
    }

    @Test
    public void shouldAddError() {

        // Given
        // A transaction and an error debug
        Transaction transaction = new Transaction();
        String error = "test";

        // When
        // We add the error to the Transaction
        transaction.addError(error);

        // Then
        // We should have one error in the collection
        assertEquals(1, transaction.errors().size());
    }

    @Test
    public void shouldAddErrorsConcurrently() throws InterruptedException, ExecutionException {
        ExecutorService pool = null;
        try {
            pool = Executors.newFixedThreadPool(100);

            final int totalErrors = 5000;

            List<String> testErrors = IntStream.range(0, totalErrors)
                    .boxed()
                    .map(String::valueOf)
                    .collect(Collectors.toList());

            final Transaction transaction = new Transaction();

            List<Callable<Boolean>> jobs = testErrors.stream()
                    .map(e -> (Callable<Boolean>) () -> {
                        transaction.addError(e);
                        return true;
                    }).collect(Collectors.toList());


            for (Future<Boolean> f : pool.invokeAll(jobs)) {
                assertTrue(f.get());
            }

            assertEquals(totalErrors, transaction.errors().size());

            testErrors.stream()
                    .filter(e -> !transaction.errors().contains(e))
                    .findAny()
                    .ifPresent(e -> fail("expected error was missing from transaction errors: " + e));
        } finally {
            if (pool != null) {
                pool.shutdown();
                pool.awaitTermination(10, SECONDS);
            }
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldNotBeAbleToModifyErrors() {

        // Given
        // A transaction
        Transaction transaction = new Transaction();
        transaction.addError("test");

        // When
        // We attempt to modify the errors
        transaction.errors().clear();

        // Then
        // We should get an exception
    }

    @Test
    public void shouldCommitTransaction() {

        // Given
        // Transactions
        Transaction ok = new Transaction();
        Transaction error = new Transaction();

        // When
        // We flag the transactions as committed
        ok.commit(true);
        error.commit(false);

        // Then
        // We should have end dates and the expected status strings
        assertEquals(Transaction.COMMITTED, ok.getStatus());
        assertEquals(Transaction.COMMIT_FAILED, error.getStatus());
        assertTrue(StringUtils.isNotBlank(ok.endDate()));
        assertTrue(StringUtils.isNotBlank(error.endDate()));
    }

    @Test
    public void shouldRollBackTransaction() {

        // Given
        // Transactions
        Transaction ok = new Transaction();
        Transaction error = new Transaction();

        // When
        // We flag the transactions as rolled back
        ok.rollback(true);
        error.rollback(false);

        // Then
        // We should have end dates and the expected status strings
        assertEquals(Transaction.ROLLED_BACK, ok.getStatus());
        assertEquals(Transaction.ROLLBACK_FAILED, error.getStatus());
        assertTrue(StringUtils.isNotBlank(ok.endDate()));
        assertTrue(StringUtils.isNotBlank(error.endDate()));
    }
}