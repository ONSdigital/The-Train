package com.github.davidcarboni.thetrain.api;

import com.github.davidcarboni.thetrain.json.Result;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.github.davidcarboni.thetrain.api.common.Endpoint.TRANSACTION_ID_KEY;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class CommitTest extends AbstractAPITest {

    private Commit endpoint;

    @Override
    public void customSetUp() throws Exception {
        endpoint = new Commit();

        ReflectionTestUtils.setField(endpoint, "publisherService", publisherService);
        ReflectionTestUtils.setField(endpoint, "transactionsService", transactionsService);
    }

    @Test
    public void shouldReturnBadRequestIfTransactionIDNull() throws Exception {
        when(request.getParameter(TRANSACTION_ID_KEY))
                .thenReturn(null);

        Result actual = endpoint.commit(request, response);
        Result expected = new Result("Please provide a transactionId parameter.", true, null);

        assertResult(actual, expected);

        verify(response, times(1)).setStatus(SC_BAD_REQUEST);
        verify(transactionsService, times(1)).update(null);
        verify(transactionsService, never()).get(anyString());
        verifyZeroInteractions(publisherService);
    }

    @Test
    public void shouldReturnInternalServerErrorIfGetTransactionErrors() throws Exception {
        IOException ex = new IOException("whoops");

        when(request.getParameter(TRANSACTION_ID_KEY))
                .thenReturn(transactionID);
        when(transactionsService.get(transactionID))
                .thenThrow(ex);

        Result actual = endpoint.commit(request, response);
        Result expected = new Result(ExceptionUtils.getStackTrace(ex), true, null);

        assertResult(actual, expected);

        verify(response, times(1)).setStatus(SC_INTERNAL_SERVER_ERROR);
        verify(transactionsService, times(1)).get(transactionID);
        verify(transactionsService, times(1)).update(null);
        verifyZeroInteractions(publisherService);
    }

    @Test
    public void shouldReturnBadRequestIfTransactionNull() throws Exception {
        when(request.getParameter(TRANSACTION_ID_KEY))
                .thenReturn(transactionID);
        when(transactionsService.get(transactionID))
                .thenReturn(null);

        Result actual = endpoint.commit(request, response);
        Result expected = new Result("Unknown transaction " + transactionID, true, null);

        assertResult(actual, expected);

        verify(response, times(1)).setStatus(SC_BAD_REQUEST);
        verify(transactionsService, times(1)).get(transactionID);
        verify(transactionsService, times(1)).update(null);
        verifyZeroInteractions(publisherService);
    }

    @Test
    public void shouldReturnBadRequestIfTransactionClosed() throws Exception {
        when(request.getParameter(TRANSACTION_ID_KEY))
                .thenReturn(transactionID);
        when(transactionsService.get(transactionID))
                .thenReturn(transaction);
        when(transaction.isOpen())
                .thenReturn(false);

        Result actual = endpoint.commit(request, response);
        Result expected = new Result("This transaction is closed.", true, transaction);

        assertResult(actual, expected);

        verify(response, times(1)).setStatus(SC_BAD_REQUEST);
        verify(transactionsService, times(1)).get(transactionID);
        verify(transactionsService, times(1)).update(transaction);
        verifyZeroInteractions(publisherService);
    }

    @Test
    public void shouldReturnBadRequestIfTransactionHasErrors() throws Exception {
        when(request.getParameter(TRANSACTION_ID_KEY))
                .thenReturn(transactionID);
        when(transactionsService.get(transactionID))
                .thenReturn(transaction);
        when(transaction.isOpen())
                .thenReturn(true);
        when(transaction.hasErrors())
                .thenReturn(true);

        Result actual = endpoint.commit(request, response);
        Result expected = new Result("This transaction cannot be committed because errors have been reported.", true, transaction);

        assertResult(actual, expected);

        verify(response, times(1)).setStatus(SC_BAD_REQUEST);
        verify(transactionsService, times(1)).get(transactionID);
        verify(transactionsService, times(1)).update(transaction);
        verifyZeroInteractions(publisherService);
    }

    @Test
    public void shouldReturnInternalServerErrorIfTWebsitePathNull() throws Exception {
        when(request.getParameter(TRANSACTION_ID_KEY))
                .thenReturn(transactionID);
        when(transactionsService.get(transactionID))
                .thenReturn(transaction);
        when(transaction.isOpen())
                .thenReturn(true);
        when(transaction.hasErrors())
                .thenReturn(false);
        when(publisherService.websitePath())
                .thenReturn(null);

        Result actual = endpoint.commit(request, response);
        Result expected = new Result("website folder could not be used: null", true, transaction);

        assertResult(actual, expected);

        verify(response, times(1)).setStatus(SC_INTERNAL_SERVER_ERROR);
        verify(transactionsService, times(1)).get(transactionID);
        verify(transactionsService, times(1)).update(transaction);
        verify(publisherService, times(1)).websitePath();
    }

    @Test
    public void shouldReturnInternalServerErrorIfCommitUnsuccessful() throws Exception {
        Path websitePath = Paths.get("/");

        when(request.getParameter(TRANSACTION_ID_KEY))
                .thenReturn(transactionID);
        when(transactionsService.get(transactionID))
                .thenReturn(transaction);
        when(transaction.isOpen())
                .thenReturn(true);
        when(transaction.hasErrors())
                .thenReturn(false);
        when(publisherService.websitePath())
                .thenReturn(websitePath);
        when(publisherService.commit(transaction, websitePath))
                .thenReturn(false);

        Result actual = endpoint.commit(request, response);
        Result expected = new Result("Errors were detected in committing the transaction.", true, transaction);

        assertResult(actual, expected);

        verify(response, times(1)).setStatus(SC_INTERNAL_SERVER_ERROR);
        verify(transactionsService, times(1)).get(transactionID);
        verify(transactionsService, times(1)).update(transaction);
        verify(publisherService, times(1)).websitePath();
        verify(publisherService, times(1)).commit(transaction, websitePath);
    }

    @Test
    public void shouldReturnOKIfCommitSuccessful() throws Exception {
        Path websitePath = Paths.get("/");

        when(request.getParameter(TRANSACTION_ID_KEY))
                .thenReturn(transactionID);
        when(transactionsService.get(transactionID))
                .thenReturn(transaction);
        when(transaction.isOpen())
                .thenReturn(true);
        when(transaction.hasErrors())
                .thenReturn(false);
        when(publisherService.websitePath())
                .thenReturn(websitePath);
        when(publisherService.commit(transaction, websitePath))
                .thenReturn(true);

        Result actual = endpoint.commit(request, response);
        Result expected = new Result("Transaction committed.", false, transaction);

        assertResult(actual, expected);

        verify(response, times(1)).setStatus(HttpServletResponse.SC_OK);
        verify(transactionsService, times(1)).get(transactionID);
        verify(transactionsService, times(1)).update(transaction);
        verify(publisherService, times(1)).websitePath();
        verify(publisherService, times(1)).commit(transaction, websitePath);
    }
}
