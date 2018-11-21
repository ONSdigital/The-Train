package com.github.onsdigital.thetrain.routes;

import com.github.onsdigital.thetrain.exception.BadRequestException;
import com.github.onsdigital.thetrain.exception.PublishException;
import com.github.onsdigital.thetrain.json.Transaction;
import com.github.onsdigital.thetrain.service.PublisherService;
import com.github.onsdigital.thetrain.service.TransactionsService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import spark.Request;
import spark.Response;
import spark.Route;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class CommitTransactionTest {

    @Mock
    private Request request;

    @Mock
    private Response response;

    @Mock
    private TransactionsService transactionsService;

    @Mock
    private PublisherService publisherService;

    @Mock
    private Transaction transaction;

    private Route route;
    private BadRequestException badRequestException;
    private PublishException publishException;
    private Path websitePath = Paths.get("/website/path");

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
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
        } catch (BadRequestException e) {
            assertThat(e.getMessage(), equalTo(publishException.getMessage()));
            verify(transactionsService, times(1)).getTransaction(request);
            verify(transactionsService, times(1)).update(null);
            verifyZeroInteractions(publisherService);
            throw e;
        }
    }

    @Test(expected = PublishException.class)
    public void testPublisherServiceGetWebsiteError() throws Exception {
        when(transactionsService.getTransaction(request)).thenReturn(transaction);

        when(publisherService.websitePath()).thenThrow(publishException);

        try {
            route.handle(request, response);
        } catch (BadRequestException e) {
            assertThat(e.getMessage(), equalTo(publishException.getMessage()));
            verify(transactionsService, times(1)).getTransaction(request);
            verify(transactionsService, times(1)).update(null);
            verify(publisherService, times(1)).websitePath();
            verifyNoMoreInteractions(transactionsService, publisherService);
            throw e;
        }
    }

    @Test(expected = PublishException.class)
    public void testPublisherServiceCommitError() throws Exception {
        when(transactionsService.getTransaction(request)).thenReturn(transaction);

        when(publisherService.websitePath()).thenReturn(websitePath);

        when(publisherService.commit(transaction, websitePath)).thenThrow(publishException);

        try {
            route.handle(request, response);
        } catch (BadRequestException e) {
            assertThat(e.getMessage(), equalTo(publishException.getMessage()));
            verify(transactionsService, times(1)).getTransaction(request);
            verify(transactionsService, times(1)).update(transaction);
            verify(publisherService, times(1)).websitePath();
            verify(publisherService, times(1)).commit(transaction, websitePath);
            throw e;
        }
    }

    @Test(expected = PublishException.class)
    public void testPublisherServiceCommitUnsuccessful() throws Exception {
        when(transactionsService.getTransaction(request)).thenReturn(transaction);

        when(publisherService.websitePath()).thenReturn(websitePath);

        when(publisherService.commit(transaction, websitePath)).thenReturn(false);

        try {
            route.handle(request, response);
        } catch (BadRequestException e) {
            assertThat(e.getMessage(), equalTo(publishException.getMessage()));
            verify(transactionsService, times(1)).getTransaction(request);
            verify(transactionsService, times(1)).update(transaction);
            verify(publisherService, times(1)).websitePath();
            verify(publisherService, times(1)).commit(transaction, websitePath);
            throw e;
        }
    }
}
