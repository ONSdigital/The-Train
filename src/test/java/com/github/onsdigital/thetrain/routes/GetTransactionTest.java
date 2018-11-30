package com.github.onsdigital.thetrain.routes;

import com.github.onsdigital.thetrain.exception.BadRequestException;
import com.github.onsdigital.thetrain.exception.PublishException;
import com.github.onsdigital.thetrain.json.Result;
import com.github.onsdigital.thetrain.json.Transaction;
import com.github.onsdigital.thetrain.service.TransactionsService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import spark.Request;
import spark.Response;
import spark.Route;

import static com.github.onsdigital.thetrain.routes.GetTransaction.GET_TRANS_SUCCESS_RESULT;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GetTransactionTest {

    private static final String TRANS_ID = "138";

    @Mock
    private TransactionsService transactionsService;

    @Mock
    private Request request;

    @Mock
    private Response response;

    @Mock
    private Transaction transaction;

    private Route route;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        route = new GetTransaction(transactionsService);
    }

    @Test
    public void testGetTransactionSuccess() throws Exception {
        when(transactionsService.getTransaction(request)).thenReturn(transaction);
        when(transaction.id()).thenReturn(TRANS_ID);

        Result actual = (Result) route.handle(request, response);

        assertFalse(actual.error);
        assertThat(actual.transaction, equalTo(transaction));
        assertThat(actual.message, equalTo(format(GET_TRANS_SUCCESS_RESULT, TRANS_ID)));
        verify(transactionsService, times(1)).getTransaction(request);
        verify(response, times(1)).status(200);
    }

    @Test(expected = BadRequestException.class)
    public void testGetTransaction_transactionServiceBadRequestEx() throws Exception {
        try {
            when(transactionsService.getTransaction(request)).thenThrow(new BadRequestException(""));

            route.handle(request, response);
        } catch (BadRequestException e) {
            verify(transactionsService, times(1)).getTransaction(request);
            verify(response, never()).status(anyInt());
            throw e;
        }
    }

    @Test(expected = PublishException.class)
    public void testGetTransaction_transactionServicePublish() throws Exception {
        try {
            when(transactionsService.getTransaction(request)).thenThrow(new PublishException(""));

            route.handle(request, response);
        } catch (PublishException e) {
            verify(transactionsService, times(1)).getTransaction(request);
            verify(response, never()).status(anyInt());
            throw e;
        }
    }
}
