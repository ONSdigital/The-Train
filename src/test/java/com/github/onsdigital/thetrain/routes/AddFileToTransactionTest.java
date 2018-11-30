package com.github.onsdigital.thetrain.routes;

import com.github.onsdigital.thetrain.exception.BadRequestException;
import com.github.onsdigital.thetrain.exception.PublishException;
import com.github.onsdigital.thetrain.json.Result;
import com.github.onsdigital.thetrain.storage.TransactionUpdate;
import org.apache.http.HttpStatus;
import org.junit.Test;
import spark.Route;

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

public class AddFileToTransactionTest extends BaseRouteTest {

    private Route route;
    private String testURI = "/a/b/c";

    @Override
    public void customSetUp() throws Exception {
        when(transaction.id())
                .thenReturn(TRANSACTION_ID);

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

        when(fileUploadHelper.getFileInputStream(raw, TRANSACTION_ID)).thenThrow(new BadRequestException("Test"));

        try {
            route.handle(request, response);
        } catch (BadRequestException e) {
            assertThat(e.getMessage(), equalTo("Test"));

            verify(transactionsService, times(1)).getTransaction(request);
            verify(fileUploadHelper, times(1)).getFileInputStream(raw, TRANSACTION_ID);
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

        when(fileUploadHelper.getFileInputStream(raw, TRANSACTION_ID)).thenReturn(stream);

        when(publisherService.addContentToTransaction(eq(transaction), eq(testURI), any(InputStream.class), any(Date.class)))
                .thenThrow(cause);

        try {
            route.handle(request, response);
        } catch (PublishException e) {
            assertThat(e.getMessage(), equalTo(ADD_FILE_ERR_MSG));
            assertThat(e.getStatus(), equalTo(HttpStatus.SC_INTERNAL_SERVER_ERROR));
            assertThat(e.getCause(), equalTo(cause));

            verify(transactionsService, times(1)).getTransaction(request);
            verify(fileUploadHelper, times(1)).getFileInputStream(raw, TRANSACTION_ID);
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

        when(fileUploadHelper.getFileInputStream(raw, TRANSACTION_ID)).thenReturn(stream);

        when(publisherService.addContentToTransaction(eq(transaction), eq(testURI), any(InputStream.class), any(Date.class)))
                .thenReturn(update);

        try {
            route.handle(request, response);
        } catch (PublishException e) {
            assertThat(e.getMessage(), equalTo(ADD_FILE_ERR_MSG));
            assertThat(e.getStatus(), equalTo(HttpStatus.SC_INTERNAL_SERVER_ERROR));

            verify(transactionsService, times(1)).getTransaction(request);
            verify(fileUploadHelper, times(1)).getFileInputStream(raw, TRANSACTION_ID);
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

        when(fileUploadHelper.getFileInputStream(raw, TRANSACTION_ID)).thenReturn(stream);

        when(publisherService.addContentToTransaction(eq(transaction), eq(testURI), any(InputStream.class), any(Date.class)))
                .thenReturn(update);

        Result actual = (Result) route.handle(request, response);

        assertThat(actual.message, equalTo("Published to " + testURI));
        assertThat(actual.transaction, equalTo(transaction));
        assertFalse(actual.error);

        verify(transactionsService, times(1)).getTransaction(request);
        verify(fileUploadHelper, times(1)).getFileInputStream(raw, TRANSACTION_ID);
        verify(publisherService, times(1)).addContentToTransaction(
                eq(transaction), eq(testURI), any(InputStream.class), any(Date.class));
        verify(transactionsService, times(1)).tryUpdateAsync(transaction);
    }

}
