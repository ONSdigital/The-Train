package com.github.davidcarboni.thetrain.api;

import com.github.davidcarboni.thetrain.api.common.Endpoint;
import com.github.davidcarboni.thetrain.json.Result;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.eclipse.jetty.http.HttpStatus.OK_200;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BeginTest extends AbstractAPITest implements Endpoint {

    private Begin endpoint;

    @Override
    public void customSetUp() throws Exception {
        endpoint = new Begin();
        ReflectionTestUtils.setField(endpoint, "transactionsService", transactionsService);
    }

    @Test
    public void shouldReturnBadRquestIfEncryptionPasswordNull() throws Exception {
        when(request.getParameter(ENCRYPTION_PASSWORD_KEY))
                .thenReturn(null);

        Result actual = endpoint.beginTransaction(request, response);
        Result expected = new Result("encryption password required but none provided", true, null);

        assertResult(actual, expected);

        verify(transactionsService, never()).create(anyString());
        verify(response, times(1)).setStatus(400);
    }

    @Test
    public void shouldSuccessfullyCreateTransaction() throws Exception {
        when(transactionsService.create(encryptionPassword))
                .thenReturn(transaction);
        when(request.getParameter(ENCRYPTION_PASSWORD_KEY))
                .thenReturn(encryptionPassword);

        Result actual = endpoint.beginTransaction(request, response);
        Result expected = new Result("New transaction created.", false, transaction);

        assertResult(actual, expected);

        verify(transactionsService, times(1)).create(encryptionPassword);
        verify(response, times(1)).setStatus(OK_200);
    }
}
