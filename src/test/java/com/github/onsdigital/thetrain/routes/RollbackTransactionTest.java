package com.github.onsdigital.thetrain.routes;

import com.github.onsdigital.thetrain.exception.BadRequestException;
import com.github.onsdigital.thetrain.exception.PublishException;
import com.github.onsdigital.thetrain.json.Result;
import org.junit.Test;
import spark.Route;

import static com.github.onsdigital.thetrain.routes.RollbackTransaction.ROLLBACK_SUCCESS_MSG;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class RollbackTransactionTest extends BaseRouteTest {

    private Route route;

    @Override
    public void customSetUp() throws Exception {
        route = new RollbackTransaction(transactionsService, publisherService);
    }

    @Test
    public void testRollbackTransactionSuccess() throws Exception {
        when(transactionsService.getTransaction(request)).thenReturn(transaction);
        when(publisherService.rollback(transaction)).thenReturn(true);

        Result result = (Result) route.handle(request, response);

        assertThat(result.message, equalTo(ROLLBACK_SUCCESS_MSG));
        assertThat(result.transaction, equalTo(transaction));
        assertFalse(result.error);

        verify(transactionsService, times(1)).getTransaction(request);
        verify(publisherService, times(1)).rollback(transaction);
        verify(transactionsService, times(1)).listFiles(transaction);
        verify(transactionsService, times(1)).update(transaction);
    }

    @Test(expected = BadRequestException.class)
    public void testRollback_getTransBadRequestEx() throws Exception {
        try {
            when(transactionsService.getTransaction(request)).thenThrow(new BadRequestException(""));

            route.handle(request, response);
        } catch (BadRequestException e) {
            verify(transactionsService, times(1)).getTransaction(request);
            verifyNoMoreInteractions(transactionsService);
            verifyZeroInteractions(publisherService);
            throw e;
        }
    }

    @Test(expected = PublishException.class)
    public void testRollback_getTransPublishEx() throws Exception {
        try {
            when(transactionsService.getTransaction(request)).thenThrow(new PublishException(""));

            route.handle(request, response);
        } catch (PublishException e) {
            verify(transactionsService, times(1)).getTransaction(request);
            verifyNoMoreInteractions(transactionsService);
            verifyZeroInteractions(publisherService);
            throw e;
        }
    }

    @Test(expected = PublishException.class)
    public void testRollback_publisherPublishEx() throws Exception {
        try {
            when(transactionsService.getTransaction(request)).thenReturn(transaction);
            when(publisherService.rollback(transaction)).thenThrow(new PublishException(""));

            route.handle(request, response);
        } catch (PublishException e) {
            verify(transactionsService, times(1)).getTransaction(request);
            verify(publisherService, times(1)).rollback(transaction);
            verify(transactionsService, times(1)).update(transaction);
            verifyNoMoreInteractions(transactionsService, publisherService);
            throw e;
        }
    }

    @Test(expected = PublishException.class)
    public void testRollback_publisherUnsuccessful() throws Exception {
        try {
            when(transactionsService.getTransaction(request)).thenReturn(transaction);
            when(publisherService.rollback(transaction)).thenReturn(false);

            route.handle(request, response);
        } catch (PublishException e) {
            verify(transactionsService, times(1)).getTransaction(request);
            verify(publisherService, times(1)).rollback(transaction);
            verify(transactionsService, times(1)).update(transaction);
            verifyNoMoreInteractions(transactionsService, publisherService);
            throw e;
        }
    }

    @Test(expected = PublishException.class)
    public void testRollback_listFilesPublishEx() throws Exception {
        try {
            when(transactionsService.getTransaction(request)).thenReturn(transaction);
            when(publisherService.rollback(transaction)).thenReturn(true);
            doThrow(new PublishException("")).when(transactionsService).listFiles(transaction);

            route.handle(request, response);
        } catch (PublishException e) {
            verify(transactionsService, times(1)).getTransaction(request);
            verify(publisherService, times(1)).rollback(transaction);
            verify(transactionsService, times(1)).update(transaction);
            verify(transactionsService, times(1)).listFiles(transaction);
            throw e;
        }
    }
}
