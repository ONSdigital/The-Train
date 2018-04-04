package com.github.davidcarboni.thetrain.api;

import com.github.davidcarboni.thetrain.json.Result;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.github.davidcarboni.thetrain.api.common.Endpoint.ENCRYPTION_PASSWORD_KEY;
import static com.github.davidcarboni.thetrain.api.common.Endpoint.TRANSACTION_ID_KEY;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class TransactionTest extends AbstractAPITest {

    private Transaction endpoint;

    @Override
    public void customSetUp() throws Exception {
        endpoint = new Transaction();

        ReflectionTestUtils.setField(endpoint, "transactionsService", transactionsService);
    }

    @Test
    public void shouldReturnBadRequestIfTransactionIDNull() throws Exception {
        when(request.getParameter(TRANSACTION_ID_KEY))
                .thenReturn(null);

        Result actual = endpoint.getTransactionDetails(request, response);

        assertResult(actual, new Result("Please provide a transactionId parameter.", true, null));

        verify(response, times(1)).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verifyZeroInteractions(transactionsService);
    }

    @Test
    public void shouldReturnBadRequestIfEncryptionPasswordNull() throws Exception {
        when(request.getParameter(TRANSACTION_ID_KEY))
                .thenReturn(transactionID);
        when(request.getParameter(ENCRYPTION_PASSWORD_KEY))
                .thenReturn(null);

        Result actual = endpoint.getTransactionDetails(request, response);

        assertResult(actual, new Result("transaction requires encryptionPassword but none was provided", true, null));

        verify(response, times(1)).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verifyZeroInteractions(transactionsService);
    }

    @Test
    public void shouldReturnBadRequestIfTransactionNull() throws Exception {
        when(request.getParameter(TRANSACTION_ID_KEY))
                .thenReturn(transactionID);
        when(request.getParameter(ENCRYPTION_PASSWORD_KEY))
                .thenReturn(encryptionPassword);

        Result actual = endpoint.getTransactionDetails(request, response);

        assertResult(actual, new Result("Unknown transaction " + transactionID, true, null));

        verify(response, times(1)).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(transactionsService, times(1)).get(transactionID, encryptionPassword);
        verifyNoMoreInteractions(transactionsService, response);
    }

    @Test
    public void shouldReturnInternalServerErrorIfGetTransactionThrowsException() throws Exception {
        Exception ex = new IOException("explosions!");

        when(request.getParameter(TRANSACTION_ID_KEY))
                .thenReturn(transactionID);
        when(request.getParameter(ENCRYPTION_PASSWORD_KEY))
                .thenReturn(encryptionPassword);
        when(transactionsService.get(transactionID, encryptionPassword))
                .thenThrow(ex);

        Result actual = endpoint.getTransactionDetails(request, response);

        assertResult(actual, new Result(ExceptionUtils.getStackTrace(ex), true, null));

        verify(response, times(1)).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        verify(transactionsService, times(1)).get(transactionID, encryptionPassword);
        verifyNoMoreInteractions(transactionsService, response);
    }

    @Test
    public void shouldReturnOKIfSuccessful() throws Exception {
        when(request.getParameter(TRANSACTION_ID_KEY))
                .thenReturn(transactionID);
        when(request.getParameter(ENCRYPTION_PASSWORD_KEY))
                .thenReturn(encryptionPassword);
        when(transactionsService.get(transactionID, encryptionPassword))
                .thenReturn(transaction);

        Result actual = endpoint.getTransactionDetails(request, response);

        assertResult(actual, new Result("Details for transaction " + transaction.id(), false, transaction));

        verify(response, times(1)).setStatus(HttpServletResponse.SC_OK);
        verify(transactionsService, times(1)).get(transactionID, encryptionPassword);
        verifyNoMoreInteractions(transactionsService, response);
    }
}
