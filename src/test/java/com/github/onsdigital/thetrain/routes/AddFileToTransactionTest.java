package com.github.onsdigital.thetrain.routes;

import com.github.onsdigital.thetrain.exception.BadRequestException;
import com.github.onsdigital.thetrain.exception.PublishException;
import com.github.onsdigital.thetrain.helpers.FileUploadHelper;
import com.github.onsdigital.thetrain.json.Result;
import com.github.onsdigital.thetrain.json.Transaction;
import com.github.onsdigital.thetrain.service.PublisherService;
import com.github.onsdigital.thetrain.service.TransactionsService;
import com.github.onsdigital.thetrain.storage.TransactionUpdate;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import spark.Request;
import spark.Response;
import spark.Route;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Date;

import static com.github.onsdigital.thetrain.routes.AddFileToTransaction.ADD_FILE_ERR_MSG;
import static com.github.onsdigital.thetrain.routes.BaseHandler.URI_MISSING_ERR;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class AddFileToTransactionTest {

    @Mock
    private Request request;

    @Mock
    private HttpServletRequest raw;

    @Mock
    private Response response;

    @Mock
    private TransactionsService transactionsService;

    @Mock
    private PublisherService publisherService;

    @Mock
    private FileUploadHelper fileUploadHelper;

    @Mock
    private Transaction transaction;

    private Route route;
    private String testURI = "/a/b/c";
    private String transactionID = "666";

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(transaction.id())
                .thenReturn(transactionID);

        route = new AddFileToTransaction(transactionsService, publisherService, fileUploadHelper);
    }

    @Test(expected = BadRequestException.class)
    public void testAddFileTransactionIDNull() throws Exception {
        when(request.raw()).thenReturn(raw);

        when(transactionsService.getTransaction(request))
                .thenThrow(new BadRequestException("TEST", 500));

        try {
            route.handle(request, response);
        } catch (BadRequestException e) {
            assertThat(e.getMessage(), equalTo("TEST"));

            verify(transactionsService, times(1)).getTransaction(request);
            verifyZeroInteractions(publisherService);
            throw e;
        }
    }

    @Test(expected = BadRequestException.class)
    public void testAddFileURINull() throws Exception {
        when(request.raw()).thenReturn(raw);

        when(raw.getParameter("uri")).thenReturn(null);

        when(transactionsService.getTransaction(request)).thenReturn(transaction);

        try {
            route.handle(request, response);
        } catch (BadRequestException e) {
            assertThat(e.getMessage(), equalTo(URI_MISSING_ERR));

            verify(transactionsService, times(1)).getTransaction(request);
            verifyZeroInteractions(publisherService);
            throw e;
        }
    }

    @Test(expected = BadRequestException.class)
    public void testAddFileGetFileInputStreamException() throws Exception {
        when(request.raw()).thenReturn(raw);

        when(raw.getParameter("uri")).thenReturn(testURI);

        when(transactionsService.getTransaction(request)).thenReturn(transaction);

        when(fileUploadHelper.getFileInputStream(raw, transactionID)).thenThrow(new BadRequestException("Test"));

        try {
            route.handle(request, response);
        } catch (BadRequestException e) {
            assertThat(e.getMessage(), equalTo("Test"));

            verify(transactionsService, times(1)).getTransaction(request);
            verify(fileUploadHelper, times(1)).getFileInputStream(raw, transactionID);
            verifyZeroInteractions(publisherService);
            throw e;
        }
    }

    @Test(expected = PublishException.class)
    public void testAddContentToTransactionException() throws Exception {
        InputStream stream = new ByteArrayInputStream("SOME DATE".getBytes());
        PublishException cause = new PublishException("publisher error!");

        when(request.raw()).thenReturn(raw);

        when(raw.getParameter("uri")).thenReturn(testURI);

        when(transactionsService.getTransaction(request)).thenReturn(transaction);

        when(fileUploadHelper.getFileInputStream(raw, transactionID)).thenReturn(stream);

        when(publisherService.addContentToTransaction(eq(transaction), eq(testURI), any(InputStream.class), any(Date.class)))
                .thenThrow(cause);

        try {
            route.handle(request, response);
        } catch (PublishException e) {
            assertThat(e.getMessage(), equalTo(ADD_FILE_ERR_MSG));
            assertThat(e.getStatus(), equalTo(HttpStatus.SC_INTERNAL_SERVER_ERROR));
            assertThat(e.getCause(), equalTo(cause));

            verify(transactionsService, times(1)).getTransaction(request);
            verify(fileUploadHelper, times(1)).getFileInputStream(raw, transactionID);
            verify(publisherService, times(1)).addContentToTransaction(
                    eq(transaction), eq(testURI), any(InputStream.class), any(Date.class));
            throw e;
        }
    }

    @Test(expected = PublishException.class)
    public void testAddContentToTransactionUnsuccessful() throws Exception {
        InputStream stream = new ByteArrayInputStream("SOME DATE".getBytes());
        TransactionUpdate update = new TransactionUpdate();
        update.setSuccess(false);

        when(request.raw()).thenReturn(raw);

        when(raw.getParameter("uri")).thenReturn(testURI);

        when(transactionsService.getTransaction(request)).thenReturn(transaction);

        when(fileUploadHelper.getFileInputStream(raw, transactionID)).thenReturn(stream);

        when(publisherService.addContentToTransaction(eq(transaction), eq(testURI), any(InputStream.class), any(Date.class)))
                .thenReturn(update);

        try {
            route.handle(request, response);
        } catch (PublishException e) {
            assertThat(e.getMessage(), equalTo(ADD_FILE_ERR_MSG));
            assertThat(e.getStatus(), equalTo(HttpStatus.SC_INTERNAL_SERVER_ERROR));

            verify(transactionsService, times(1)).getTransaction(request);
            verify(fileUploadHelper, times(1)).getFileInputStream(raw, transactionID);
            verify(publisherService, times(1)).addContentToTransaction(
                    eq(transaction), eq(testURI), any(InputStream.class), any(Date.class));
            throw e;
        }
    }

    @Test
    public void testAddContentToTransactionSuccessful() throws Exception {
        InputStream stream = new ByteArrayInputStream("SOME DATE".getBytes());
        TransactionUpdate update = new TransactionUpdate();
        update.setSuccess(true);

        when(request.raw()).thenReturn(raw);

        when(raw.getParameter("uri")).thenReturn(testURI);

        when(transactionsService.getTransaction(request)).thenReturn(transaction);

        when(fileUploadHelper.getFileInputStream(raw, transactionID)).thenReturn(stream);

        when(publisherService.addContentToTransaction(eq(transaction), eq(testURI), any(InputStream.class), any(Date.class)))
                .thenReturn(update);

        Result actual = (Result) route.handle(request, response);

        assertThat(actual.message, equalTo("Published to " + testURI));
        assertThat(actual.transaction, equalTo(transaction));
        assertFalse(actual.error);

        verify(transactionsService, times(1)).getTransaction(request);
        verify(fileUploadHelper, times(1)).getFileInputStream(raw, transactionID);
        verify(publisherService, times(1)).addContentToTransaction(
                eq(transaction), eq(testURI), any(InputStream.class), any(Date.class));
        verify(transactionsService, times(1)).tryUpdateAsync(transaction);
    }

}
