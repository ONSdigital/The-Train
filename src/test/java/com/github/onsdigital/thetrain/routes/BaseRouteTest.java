package com.github.onsdigital.thetrain.routes;

import com.github.onsdigital.thetrain.helpers.uploads.CloseablePartSupplier;
import com.github.onsdigital.thetrain.json.Transaction;
import com.github.onsdigital.thetrain.service.PublisherService;
import com.github.onsdigital.thetrain.service.TransactionsService;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import spark.Request;
import spark.Response;

import javax.servlet.http.HttpServletRequest;

public abstract class BaseRouteTest {

    public static final String TRANSACTION_ID = "666";

    @Mock
    protected Request request;

    @Mock
    protected HttpServletRequest raw;

    @Mock
    protected Response response;

    @Mock
    protected TransactionsService transactionsService;

    @Mock
    protected PublisherService publisherService;

    @Mock
    protected Transaction transaction;

    @Mock
    protected CloseablePartSupplier filePartSupplier;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        customSetUp();
    }

    public abstract void customSetUp() throws Exception;
}
