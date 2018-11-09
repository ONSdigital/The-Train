package com.github.davidcarboni.thetrain.api;

import com.github.davidcarboni.thetrain.json.Result;
import com.github.davidcarboni.thetrain.json.Transaction;
import com.github.davidcarboni.thetrain.service.PublisherService;
import com.github.davidcarboni.thetrain.service.TransactionsService;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public abstract class AbstractAPITest {

    protected String transactionID = "666";

    @Mock
    protected TransactionsService transactionsService;

    @Mock
    protected PublisherService publisherService;

    @Mock
    protected HttpServletRequest request;

    @Mock
    protected HttpServletResponse response;

    @Mock
    protected Transaction transaction;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(transaction.id())
                .thenReturn(transactionID);

        customSetUp();
    }

    public abstract void customSetUp() throws Exception;

    protected void assertResult(Result actual, Result expected) {
        assertThat(actual.message, equalTo(expected.message));
        assertThat(actual.error, is(expected.error));
        assertThat(actual.transaction, equalTo(expected.transaction));
    }

}
