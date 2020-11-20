package com.github.onsdigital.thetrain.helpers.uploads;

import com.github.onsdigital.thetrain.exception.BadRequestException;
import com.github.onsdigital.thetrain.exception.PublishException;
import com.github.onsdigital.thetrain.json.Transaction;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import spark.Request;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.github.onsdigital.thetrain.helpers.uploads.FilePartSupplier.MULTIPART_CONFIG;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNotNull;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FilePartSupplierTest {

    static final String TRANS_ID = "666";

    @Mock
    private Request request;

    @Mock
    private HttpServletRequest raw;

    @Mock
    private Transaction transaction;

    @Mock
    private Part part;

    private FilePartSupplier supplier;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        supplier = new FilePartSupplier(Paths.get("tmp"), -1, -1, 1024*1024*10);

        when(transaction.id())
                .thenReturn("666");
    }

    @Test (expected = PublishException.class)
    public void givenRequestIsNull_shouldThrowPublishingException() throws Exception {
        try {
            supplier.getFilePart(null, null);
        } catch (PublishException ex) {
            assertThat(ex.getMessage(), equalTo("error getting file part from request as request was null"));
            throw ex;
        }
    }

    @Test (expected = PublishException.class)
    public void givenTransactionIsNull_shouldThrowPublishingException() throws Exception {
        try {
            supplier.getFilePart(request, null);
        } catch (PublishException ex) {
            assertThat(ex.getMessage(), equalTo("error getting file part from request transaction expected but was null"));
            throw ex;
        }
    }

    @Test (expected = PublishException.class)
    public void givenRequestRawReturnsNull_shouldThrowPublishingException() throws Exception {
        when(request.raw()).thenReturn(null);

        try {
            supplier.getFilePart(request, transaction);
        } catch (PublishException ex) {
            assertThat(ex.getMessage(), equalTo("error getting file part from request as HttpServletRequest was null"));
            assertThat(ex.getTransaction(), equalTo(transaction));
            throw ex;
        }
    }

    @Test (expected = BadRequestException.class)
    public void givenGetPartThrowsException_shouldThrowBadRequestException() throws Exception {
        IOException expected = new IOException("get part error");

        when(request.raw())
                .thenReturn(raw);
        when(raw.getPart(anyString()))
                .thenThrow(expected);

        try {
            supplier.getFilePart(request, transaction);
        } catch (BadRequestException ex) {
            assertThat(ex.getMessage(), equalTo("error attempting to retriving multipart file upload request body"));
            assertThat(ex.getCause(), equalTo(expected));
            assertThat(ex.getTransactionID(), equalTo(TRANS_ID));
            throw ex;
        }
    }

    @Test (expected = BadRequestException.class)
    public void givenGetPartReturnsNull_shouldThrowBadRequestException() throws Exception {
        IOException expected = new IOException("get part error");

        when(request.raw())
                .thenReturn(raw);
        when(raw.getPart(anyString()))
                .thenReturn(null);

        try {
            supplier.getFilePart(request, transaction);
        } catch (BadRequestException ex) {
            assertThat(ex.getMessage(), equalTo("expected multipart file upload request body but was null"));
            assertThat(ex.getTransactionID(), equalTo(TRANS_ID));
            throw ex;
        }
    }

    @Test
    public void givengetFilePartIsSuccessful_shouldReturnCloseablePart() throws Exception {
        when(request.raw())
                .thenReturn(raw);
        when(raw.getPart(anyString()))
                .thenReturn(part);

        ArgumentCaptor<MultipartConfigElement> captor = ArgumentCaptor.forClass(MultipartConfigElement.class);

        Path location = Paths.get("tmp");
        long fileSizeLimit = 1024*1024*50;
        long requestSizeLimit = -1;
        int threasholdLimit = 1024*1024*10;

        supplier = new FilePartSupplier(location, fileSizeLimit, requestSizeLimit, threasholdLimit);
        CloseablePart actual = supplier.getFilePart(request, transaction);

        assertThat(actual, is(notNullValue()));
        assertThat(actual.getPart(), equalTo(part));

        verify(request, times(1)).attribute(eq(MULTIPART_CONFIG), captor.capture());
        MultipartConfigElement cfg = captor.getValue();
        assertThat(cfg.getLocation(), equalTo(location.toString()));
        assertThat(cfg.getMaxFileSize(), equalTo(fileSizeLimit));
        assertThat(cfg.getMaxRequestSize(), equalTo(requestSizeLimit));
    }
}