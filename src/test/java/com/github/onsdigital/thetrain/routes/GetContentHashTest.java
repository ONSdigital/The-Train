package com.github.onsdigital.thetrain.routes;

import com.github.onsdigital.thetrain.exception.BadRequestException;
import com.github.onsdigital.thetrain.response.ContentHashEntity;
import com.github.onsdigital.thetrain.service.ContentException;
import com.github.onsdigital.thetrain.service.ContentService;
import org.junit.Test;
import org.mockito.Mock;
import spark.HaltException;
import spark.Request;

import java.io.IOException;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class GetContentHashTest extends BaseRouteTest {

    private static final String URI = "/hello/world";

    @Mock
    private ContentService contentService;

    private GetContentHash route;

    @Override
    public void customSetUp() throws Exception {
        route = new GetContentHash(transactionsService, contentService, true);
    }

    @Test(expected = BadRequestException.class)
    public void testHandle_transactionNotFound() throws Exception {
        when(transactionsService.getTransaction(any(Request.class)))
                .thenThrow(new BadRequestException("transaction not found"));

        try {
            route.handle(request, response);
        } catch (BadRequestException ex) {
            assertThat(ex.getMessage(), equalTo("transaction not found"));
            throw ex;
        }
    }

    @Test(expected = BadRequestException.class)
    public void testHandle_uriEmpty() throws Exception {
        when(transactionsService.getTransaction(any(Request.class)))
                .thenReturn(transaction);
        when(request.raw())
                .thenReturn(raw);
        when(raw.getParameter("uri"))
                .thenReturn("");
        when(contentService.getContentHash(transaction, null))
                .thenThrow(new IllegalArgumentException("uri is null"));

        try {
            route.handle(request, response);
        } catch (HaltException ex) {
            assertThat(ex.statusCode(), equalTo(400));
            throw ex;
        }
    }

    @Test(expected = HaltException.class)
    public void testHandle_contentServiceIllegalArgException() throws Exception {
        when(transactionsService.getTransaction(any(Request.class)))
                .thenReturn(transaction);
        when(request.raw())
                .thenReturn(raw);
        when(raw.getParameter("uri"))
                .thenReturn(URI);
        when(contentService.getContentHash(transaction, URI))
                .thenThrow(new IllegalArgumentException(""));

        try {
            route.handle(request, response);
        } catch (HaltException ex) {
            assertThat(ex.statusCode(), equalTo(400));
            throw ex;
        }
    }

    @Test(expected = HaltException.class)
    public void testHandle_contentServiceContentException() throws Exception {
        when(transactionsService.getTransaction(any(Request.class)))
                .thenReturn(transaction);
        when(request.raw())
                .thenReturn(raw);
        when(raw.getParameter("uri"))
                .thenReturn(URI);
        when(contentService.getContentHash(transaction, URI))
                .thenThrow(new ContentException(""));

        try {
            route.handle(request, response);
        } catch (HaltException ex) {
            assertThat(ex.statusCode(), equalTo(404));
            throw ex;
        }
    }

    @Test(expected = HaltException.class)
    public void testHandle_contentServiceIOException() throws Exception {
        when(transactionsService.getTransaction(any(Request.class)))
                .thenReturn(transaction);
        when(request.raw())
                .thenReturn(raw);
        when(raw.getParameter("uri"))
                .thenReturn(URI);
        when(contentService.getContentHash(transaction, URI))
                .thenThrow(new IOException(""));

        try {
            route.handle(request, response);
        } catch (HaltException ex) {
            assertThat(ex.statusCode(), equalTo(500));
            throw ex;
        }
    }

    @Test
    public void testHandle_success() throws Exception {
        when(transactionsService.getTransaction(any(Request.class)))
                .thenReturn(transaction);
        when(request.raw())
                .thenReturn(raw);
        when(raw.getParameter("uri"))
                .thenReturn(URI);
        when(contentService.getContentHash(transaction, URI))
                .thenReturn("666"); // evil all the time.
        when(transaction.id())
                .thenReturn("12345");

        ContentHashEntity expected = new ContentHashEntity("12345", URI, "666");

        Object actual = route.handle(request, response);

        assertTrue(actual instanceof ContentHashEntity);
        assertThat((ContentHashEntity) actual, equalTo(expected));
    }

    @Test(expected = HaltException.class)
    public void handle_verifyPublishFeatureDisabled_haltException() throws Exception {
        route = new GetContentHash(transactionsService, contentService, false);

        try {
            route.handle(request, response);
        } catch (HaltException ex) {
            assertThat(ex.statusCode(), equalTo(404));
            throw ex;
        }
    }
}
