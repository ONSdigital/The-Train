package com.github.onsdigital.thetrain.routes;

import com.github.onsdigital.thetrain.exception.BadRequestException;
import com.github.onsdigital.thetrain.exception.PublishException;
import com.github.onsdigital.thetrain.json.Result;
import com.github.onsdigital.thetrain.json.Transaction;
import org.junit.Test;
import spark.Route;

import java.nio.file.Path;
import java.nio.file.Paths;

import static com.github.onsdigital.thetrain.routes.CommitTransaction.COMMIT_UNSUCCESSFUL_ERR;
import static com.github.onsdigital.thetrain.routes.CommitTransaction.RESULT_SUCCESS_MSG;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.*;

public class CommitTransactionTest extends BaseRouteTest {

    private Route route;
    private BadRequestException badRequestException;
    private PublishException publishException;
    private Path websitePath = Paths.get("/website/path");

    @Override
    public void customSetUp() throws Exception {
        route = new CommitTransaction(transactionsService, publisherService);
        badRequestException = new BadRequestException("Boom!");
        publishException = new PublishException("Boom!");
    }

    @Test(expected = BadRequestException.class)
    public void testTransactionServiceBadRequestException() throws Exception {
        when(transactionsService.getTransaction(request)).thenThrow(badRequestException);

        try {
            route.handle(request, response);
        } catch (BadRequestException e) {
            assertThat(e.getMessage(), equalTo(badRequestException.getMessage()));
            verify(transactionsService, times(1)).getTransaction(request);
            verify(transactionsService, times(1)).update(null);
            verifyZeroInteractions(publisherService);
            throw e;
        }
    }

    @Test(expected = PublishException.class)
    public void testTransactionServicePublishException() throws Exception {
        when(transactionsService.getTransaction(request)).thenThrow(publishException);

        try {
            route.handle(request, response);
        } catch (PublishException e) {
            assertThat(e.getMessage(), equalTo(publishException.getMessage()));
            verify(transactionsService, times(1)).getTransaction(request);
            verify(transactionsService, times(1)).update(null);
            verifyZeroInteractions(publisherService);
            throw e;
        }
    }

    @Test(expected = PublishException.class)
    public void testPublisherServiceCommitError() throws Exception {
        when(transactionsService.getTransaction(request)).thenReturn(transaction);
        when(publisherService.commit(transaction)).thenThrow(publishException);
        try {
            route.handle(request, response);
        } catch (PublishException e) {
            assertThat(e.getMessage(), equalTo(publishException.getMessage()));
            verify(transactionsService, times(1)).getTransaction(request);
            verify(transactionsService, times(1)).update(transaction);
            verify(publisherService, times(1)).commit(transaction);
            throw e;
        }
    }

        @Test(expected = PublishException.class)
    public void testPublisherServiceCommitUnsuccessful() throws Exception {
        when(transactionsService.getTransaction(request)).thenReturn(transaction);

        when(publisherService.commit(transaction)).thenReturn(false);

        try {
            route.handle(request, response);
        } catch (PublishException e) {
            assertThat(e.getMessage(), equalTo(COMMIT_UNSUCCESSFUL_ERR));
            verify(transactionsService, times(1)).getTransaction(request);
            verify(transactionsService, times(1)).update(transaction);
            verify(publisherService, times(1)).commit(transaction);
            throw e;
        }
    }

    @Test
    public void testCommitTransactionSuccess() throws Exception {
        when(transactionsService.getTransaction(request)).thenReturn(transaction);

        when(publisherService.commit(transaction)).thenReturn(true);

        Result result = (Result) route.handle(request, response);

        assertThat(result.transaction, equalTo(transaction));
        assertThat(result.message, equalTo(RESULT_SUCCESS_MSG));
        assertFalse(result.error);

        verify(transaction, times(1)).setStatus(Transaction.COMMITTED);

        verify(transactionsService, times(1)).getTransaction(request);
        verify(transactionsService, times(1)).update(transaction);
        verify(publisherService, times(2)).commit(transaction);
    }
}
