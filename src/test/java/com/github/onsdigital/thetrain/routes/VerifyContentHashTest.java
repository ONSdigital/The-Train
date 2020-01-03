package com.github.onsdigital.thetrain.routes;

import com.github.onsdigital.thetrain.exception.BadRequestException;
import com.github.onsdigital.thetrain.json.JSONReader;
import com.github.onsdigital.thetrain.json.VerifyHashEnity;
import com.github.onsdigital.thetrain.response.VerifyHashResult;
import com.github.onsdigital.thetrain.service.ContentException;
import com.github.onsdigital.thetrain.service.ContentService;
import org.junit.Test;
import org.mockito.Mock;
import spark.HaltException;
import spark.Request;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class VerifyContentHashTest extends BaseRouteTest {

    @Mock
    private JSONReader jsonReader;

    @Mock
    private ContentService contentService;

    private VerifyContentHash route;

    private VerifyHashEnity verifyHashEnity;

    @Override
    public void customSetUp() throws Exception {
        route = new VerifyContentHash(transactionsService, contentService, jsonReader);
        verifyHashEnity = new VerifyHashEnity("/a/b/c", "123456789");
    }

    @Test(expected = BadRequestException.class)
    public void testVerifyContent_transactionNotFound() throws Exception {
        when(transactionsService.getTransaction(any(Request.class)))
                .thenThrow(new BadRequestException("transaction not found"));

        try {
            route.handle(request, response);
        } catch (BadRequestException ex) {
            assertThat(ex.getMessage(), equalTo("transaction not found"));
            throw ex;
        }
    }

    @Test(expected = HaltException.class)
    public void testVerifyContentHash_uriEmpty() throws Exception {
        when(transactionsService.getTransaction(any(Request.class)))
                .thenReturn(transaction);

        when(jsonReader.fromRequestBody(request, VerifyHashEnity.class))
                .thenReturn(new VerifyHashEnity(null, null));

        when(contentService.isValidHash(transaction, null, null))
                .thenThrow(new IllegalArgumentException("uri is null"));

        try {
            route.handle(request, response);
        } catch (HaltException ex) {
            assertThat(ex.statusCode(), equalTo(400));
            throw ex;
        }
    }

    @Test(expected = HaltException.class)
    public void testVerifyContentHash_hashEmpty() throws Exception {
        when(transactionsService.getTransaction(any(Request.class)))
                .thenReturn(transaction);

        when(jsonReader.fromRequestBody(request, VerifyHashEnity.class))
                .thenReturn(new VerifyHashEnity("somevalue", null));

        when(contentService.isValidHash(transaction, "somevalue", null))
                .thenThrow(new IllegalArgumentException("hash is null"));

        try {
            route.handle(request, response);
        } catch (HaltException ex) {
            assertThat(ex.statusCode(), equalTo(400));
            throw ex;
        }
    }

    @Test(expected = HaltException.class)
    public void testVerifyContentHash_ContentException() throws Exception {
        when(transactionsService.getTransaction(any(Request.class)))
                .thenReturn(transaction);

        when(jsonReader.fromRequestBody(request, VerifyHashEnity.class))
                .thenReturn(verifyHashEnity);

        when(contentService.isValidHash(transaction, verifyHashEnity.getUri(), verifyHashEnity.getHash()))
                .thenThrow(new ContentException("unexpected error happened"));

        try {
            route.handle(request, response);
        } catch (HaltException ex) {
            assertThat(ex.statusCode(), equalTo(404));
            throw ex;
        }
    }

    @Test(expected = HaltException.class)
    public void testVerifyContentHash_UnexpectedError() throws Exception {
        when(transactionsService.getTransaction(any(Request.class)))
                .thenReturn(transaction);

        when(jsonReader.fromRequestBody(request, VerifyHashEnity.class))
                .thenReturn(verifyHashEnity);

        when(contentService.isValidHash(transaction, verifyHashEnity.getUri(), verifyHashEnity.getHash()))
                .thenThrow(new RuntimeException());

        try {
            route.handle(request, response);
        } catch (HaltException ex) {
            assertThat(ex.statusCode(), equalTo(500));
            throw ex;
        }
    }

    @Test
    public void testVerifyContentHash_expectedHashIncorrect() throws Exception {
        when(transactionsService.getTransaction(any(Request.class)))
                .thenReturn(transaction);

        when(jsonReader.fromRequestBody(request, VerifyHashEnity.class))
                .thenReturn(verifyHashEnity);

        when(contentService.isValidHash(transaction, verifyHashEnity.getUri(), verifyHashEnity.getHash()))
                .thenReturn(false);

        Object result = route.handle(request, response);

        assertTrue((result instanceof VerifyHashResult));

        VerifyHashResult expected = new VerifyHashResult(
                transaction.id(),
                verifyHashEnity.getUri(),
                verifyHashEnity.getHash(),
                false);

        assertThat((VerifyHashResult) result, equalTo(expected));
    }

    @Test
    public void testVerifyContentHash_success() throws Exception {
        when(transactionsService.getTransaction(any(Request.class)))
                .thenReturn(transaction);

        when(jsonReader.fromRequestBody(request, VerifyHashEnity.class))
                .thenReturn(verifyHashEnity);

        when(contentService.isValidHash(transaction, verifyHashEnity.getUri(), verifyHashEnity.getHash()))
                .thenReturn(true);

        Object result = route.handle(request, response);

        assertTrue((result instanceof VerifyHashResult));

        VerifyHashResult expected = new VerifyHashResult(
                transaction.id(),
                verifyHashEnity.getUri(),
                verifyHashEnity.getHash(),
                true);

        assertThat((VerifyHashResult) result, equalTo(expected));
    }
}
