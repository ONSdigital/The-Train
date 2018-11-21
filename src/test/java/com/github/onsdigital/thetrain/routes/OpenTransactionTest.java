package com.github.onsdigital.thetrain.routes;

import com.github.onsdigital.thetrain.exception.PublishException;
import com.github.onsdigital.thetrain.json.Result;
import com.github.onsdigital.thetrain.json.Transaction;
import com.github.onsdigital.thetrain.service.TransactionsService;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import spark.Request;
import spark.Response;
import spark.Route;

import static com.github.onsdigital.thetrain.routes.OpenTransaction.SUCCESS_MSG;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OpenTransactionTest {

    @Mock
    private Request request;

    @Mock
    private Response response;

    @Mock
    private TransactionsService transactionsService;

    @Mock
    private Transaction transaction;

    private Route route;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(transaction.id())
                .thenReturn("666");

        route = new OpenTransaction(transactionsService);
    }

    @Test(expected = PublishException.class)
    public void testHandlePublishException() throws Exception {
        when(transactionsService.create())
                .thenThrow(new PublishException("Test"));

        try {
            route.handle(request, response);
        } catch (PublishException e) {
            assertThat(e.getStatus(), equalTo(HttpStatus.SC_INTERNAL_SERVER_ERROR));
            assertThat(e.getMessage(), equalTo("Test"));
            verify(transactionsService, times(1)).create();
            throw e;
        }
    }

    @Test
    public void testHandleSuccess() throws Exception {
        when(transactionsService.create())
                .thenReturn(transaction);

        Result result = (Result) route.handle(request, response);

        verify(transactionsService, times(1)).create();
        verify(response, times(1)).status(HttpStatus.SC_OK);
        assertThat(result.message, equalTo(SUCCESS_MSG));
        assertThat(result.transaction, equalTo(transaction));
        assertFalse(result.error);
    }
}
