package com.github.davidcarboni.thetrain.destination.json;

import com.github.davidcarboni.cryptolite.Random;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Created by david on 18/08/2015.
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
        assertFalse(StringUtils.isBlank(transaction.id()));
        assertFalse(StringUtils.isBlank(transaction.startDate()));
    }

    @Test
    public void shouldAddUri() throws Exception {

        // Given
        // A transaction and a URI info
        Transaction transaction = new Transaction();
        Uri UriInfo = new Uri("test", new Date());

        // When
        // We add the URI info to the Transaction
        transaction.addUri(UriInfo);

        // Then
        // We should have one URI info in the collection
        assertEquals(1, transaction.uris().size());
    }

    @Test
    public void shouldAddUrisConcurrently() throws Exception {

        // Given
        // A transaction and lots of URI infos
        final Transaction transaction = new Transaction();
        Set<Uri> uriInfos = new HashSet<>();
        for (int i = 0; i < 2000; i++) {
            uriInfos.add(new Uri("/"+Random.id(), new Date()));
        }
        ExecutorService pool = Executors.newFixedThreadPool(100);

        // When
        // We add all the URI infos to the Transaction
        for (final Uri UriInfo : uriInfos) {
            pool.submit(new Runnable() {
                @Override
                public void run() {
                    transaction.addUri(UriInfo);
                }
            });
        }

        // Then
        // We should have added all URI infos to the Transaction and not lost any
        pool.shutdown();
        pool.awaitTermination(10, SECONDS);
        assertEquals(uriInfos.size(), transaction.uris().size());
        for (Uri UriInfo : transaction.uris()) {
            Assert.assertTrue(uriInfos.contains(UriInfo));
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldNotBeAbleToModifyUris() throws Exception {

        // Given
        // A transaction
        Transaction transaction = new Transaction();
        transaction.addUri(new Uri("test", new Date()));

        // When
        // We attempt to modify the URIs
        transaction.uris().clear();

        // Then
        // We should get an exception
    }

    @Test
    public void shouldAddError()  {

        // Given
        // A transaction and an error message
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
    public void shouldAddErrorsConcurrently() throws Exception {

        // Given
        // A transaction and lots of error messages
        final Transaction transaction = new Transaction();
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < 2000; i++) {
            errors.add(Random.id());
        }
        ExecutorService pool = Executors.newFixedThreadPool(100);

        // When
        // We add all the error messages to the Transaction
        for (final String error : errors) {
            pool.submit(new Runnable() {
                @Override
                public void run() {
                    transaction.addError(error);
                }
            });
        }

        // Then
        // We should have added all error messages to the Transaction and not lost any
        pool.shutdown();
        pool.awaitTermination(10, SECONDS);
        assertEquals(errors.size(), transaction.errors().size());
        for (String error : transaction.errors()) {
            Assert.assertTrue(errors.contains(error));
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldNotBeAbleToModifyErrors() throws Exception {

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
}