package com.github.davidcarboni.thetrain.api;

import com.github.davidcarboni.thetrain.json.Result;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import static com.github.davidcarboni.thetrain.api.common.Endpoint.TRANSACTION_ID_KEY;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class RollbackTest extends AbstractAPITest {

    private Rollback endpoint;

    @Override
    public void customSetUp() throws Exception {
        endpoint = new Rollback();

        ReflectionTestUtils.setField(endpoint, "transactionsService", transactionsService);
        ReflectionTestUtils.setField(endpoint, "publisherService", publisherService);
    }

    @Test
    public void shouldReturnBadRequestIfTransactionIDNull() throws Exception {
        when(request.getParameter(TRANSACTION_ID_KEY))
                .thenReturn(null);

        Result actual = endpoint.rollback(request, response);

        assertResult(actual, new Result("Please provide a transactionId parameter.", true, null));

        verify(response, times(1)).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(transactionsService, times(1)).update(null);
        verify(transactionsService, never()).get(anyString());
        verifyZeroInteractions(publisherService);
    }

    @Test
    public void shouldReturnBadRequestIfTransactionNull() throws Exception {
        when(request.getParameter(TRANSACTION_ID_KEY))
                .thenReturn(transactionID);
        when(transactionsService.get(transactionID))
                .thenReturn(null);

        Result actual = endpoint.rollback(request, response);

        assertResult(actual, new Result("Unknown transaction " + transactionID, true, null));

        verify(response, times(1)).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(transactionsService, times(1)).get(transactionID);
        verify(transactionsService, times(1)).update(null);
        verifyZeroInteractions(publisherService);
    }

    @Test
    public void shouldReturnInternalServerErrorIfGetTransactionerrors() throws Exception {
        Exception e = new IOException("bad things are happening");

        when(request.getParameter(TRANSACTION_ID_KEY))
                .thenReturn(transactionID);
        when(transactionsService.get(transactionID))
                .thenThrow(e);

        Result actual = endpoint.rollback(request, response);

        assertResult(actual, new Result(ExceptionUtils.getStackTrace(e), true, null));

        verify(response, times(1)).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        verify(transactionsService, times(1)).get(transactionID);
        verify(transactionsService, times(1)).update(any());
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

        Result actual = endpoint.rollback(request, response);

        assertResult(actual, new Result("This transaction is closed.", true, transaction));

        verify(response, times(1)).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(transactionsService, times(1)).get(transactionID);
        verify(transactionsService, times(1)).update(transaction);
        verifyZeroInteractions(publisherService);
    }

    @Test
    public void shouldReturnInternalServerErrorIfRollbackFails() throws Exception {
        when(request.getParameter(TRANSACTION_ID_KEY))
                .thenReturn(transactionID);
        when(transactionsService.get(transactionID))
                .thenReturn(transaction);
        when(transaction.isOpen())
                .thenReturn(true);
        when(publisherService.rollback(transaction))
                .thenReturn(false);

        Result actual = endpoint.rollback(request, response);

        assertResult(actual, new Result("Errors were detected in rolling back the transaction.", true, transaction));

        verify(response, times(1)).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        verify(transactionsService, times(1)).get(transactionID);
        verify(publisherService, times(1)).rollback(transaction);
        verify(transactionsService, times(1)).update(transaction);
    }

    @Test
    public void shouldReturnOKIfRollbackSuccessful() throws Exception {
        when(request.getParameter(TRANSACTION_ID_KEY))
                .thenReturn(transactionID);
        when(transactionsService.get(transactionID))
                .thenReturn(transaction);
        when(transaction.isOpen())
                .thenReturn(true);
        when(publisherService.rollback(transaction))
                .thenReturn(true);

        Result actual = endpoint.rollback(request, response);

        assertResult(actual, new Result("Transaction rolled back.", false, transaction));

        verify(response, times(1)).setStatus(HttpServletResponse.SC_OK);
        verify(transactionsService, times(1)).get(transactionID);
        verify(publisherService, times(1)).rollback(transaction);
        verify(transactionsService, times(1)).listFiles(transaction);
        verify(transactionsService, times(1)).update(transaction);
    }
}
