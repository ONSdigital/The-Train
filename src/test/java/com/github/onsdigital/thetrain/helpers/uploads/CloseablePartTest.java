package com.github.onsdigital.thetrain.helpers.uploads;

import com.github.onsdigital.thetrain.helpers.uploads.CloseablePart;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.Part;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class CloseablePartTest {

    static final String TRANSACTION_ID = "138";

    @Mock
    private InputStream inputStream;

    @Mock
    private Part wrappedPart;

    private CloseablePart part;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void getInputStream_shouldReturnWrappedPartInputStream() throws Exception {
        when(wrappedPart.getInputStream())
                .thenReturn(inputStream);

        assertThat(new CloseablePart(wrappedPart, TRANSACTION_ID).getInputStream(), equalTo(inputStream));

        verify(wrappedPart, times(1)).getInputStream();
        verifyNoMoreInteractions(wrappedPart);
    }

    @Test
    public void getContentType_shouldReturnWrappedPartContentType() {
        String expected = "application/json";
        when(wrappedPart.getContentType())
                .thenReturn(expected);

        assertThat(new CloseablePart(wrappedPart, TRANSACTION_ID).getContentType(), equalTo(expected));

        verify(wrappedPart, times(1)).getContentType();
        verifyNoMoreInteractions(wrappedPart);
    }

    @Test
    public void getName_shouldReturnWrappedPartName() {
        String expected = "Bruce Wayne";
        when(wrappedPart.getName())
                .thenReturn(expected);

        assertThat(new CloseablePart(wrappedPart, TRANSACTION_ID).getName(), equalTo(expected));

        verify(wrappedPart, times(1)).getName();
        verifyNoMoreInteractions(wrappedPart);
    }

    @Test
    public void getSubmittedFileName_shouldReturnWrappedPartSubmittedFileName() {
        String expected = "expected.json";
        when(wrappedPart.getSubmittedFileName())
                .thenReturn(expected);

        assertThat(new CloseablePart(wrappedPart, TRANSACTION_ID).getSubmittedFileName(), equalTo(expected));

        verify(wrappedPart, times(1)).getSubmittedFileName();
        verifyNoMoreInteractions(wrappedPart);
    }

    @Test
    public void getSize_shouldReturnWrappedPartSize() {
        long expected = 666;
        when(wrappedPart.getSize())
                .thenReturn(expected);

        assertThat(new CloseablePart(wrappedPart, TRANSACTION_ID).getSize(), equalTo(expected));

        verify(wrappedPart, times(1)).getSize();
        verifyNoMoreInteractions(wrappedPart);
    }

    @Test
    public void write_shouldCallWrappedPartWrite() throws IOException {
        String expected = "expected.json";
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        new CloseablePart(wrappedPart, TRANSACTION_ID).write(expected);

        verify(wrappedPart, times(1)).write(captor.capture());
        assertThat(captor.getValue(), equalTo(expected));
        verifyNoMoreInteractions(wrappedPart);
    }

    @Test
    public void delete_shouldCallWrappedPartDelete() throws IOException {
        new CloseablePart(wrappedPart, TRANSACTION_ID).delete();

        verify(wrappedPart, times(1)).delete();
    }

    @Test
    public void getHeader_shouldReturnWrappedPartGetHeader() {
        String name = "header name";
        String expected = "Alfred Pennyworth";

        when(wrappedPart.getHeader(name))
                .thenReturn(expected);

        assertThat(new CloseablePart(wrappedPart, TRANSACTION_ID).getHeader(name), equalTo(expected));

        verify(wrappedPart, times(1)).getHeader(name);
        verifyNoMoreInteractions(wrappedPart);
    }

    @Test
    public void getHeaders_shouldReturnWrappedPartGetHeaders() {
        String name = "header name";

        ArrayList<String> expected = new ArrayList<String>() {{
            add("Alfred Pennyworth");
        }};

        when(wrappedPart.getHeaders(name))
                .thenReturn(expected);

        assertThat(new CloseablePart(wrappedPart, TRANSACTION_ID).getHeaders(name), equalTo(expected));

        verify(wrappedPart, times(1)).getHeaders(name);
        verifyNoMoreInteractions(wrappedPart);
    }

    @Test
    public void getHeaderNames_shouldReturnWrappedPartGetHeaderNames() {
        ArrayList<String> expected = new ArrayList<String>() {{
            add("Alfred Pennyworth");
            add("Bruce Wayne");
            add("Dick Grayson");
        }};

        when(wrappedPart.getHeaderNames())
                .thenReturn(expected);

        assertThat(new CloseablePart(wrappedPart, TRANSACTION_ID).getHeaderNames(), equalTo(expected));

        verify(wrappedPart, times(1)).getHeaderNames();
        verifyNoMoreInteractions(wrappedPart);
    }

    @Test
    public void close_shouldCallWrappedPartDeleteIfNotNull() throws Exception {
        new CloseablePart(wrappedPart, TRANSACTION_ID).close();

        verify(wrappedPart, times(1)).delete();
        verifyNoMoreInteractions(wrappedPart);
    }

    @Test
    public void close_shouldDoNothingIfWrappedPartIsNull() throws Exception {
        new CloseablePart(null, TRANSACTION_ID).close();

        verifyZeroInteractions(wrappedPart);
    }

    @Test
    public void close_shouldSwallowErrorIfWrappedPartDeleteThrowsAnException() throws Exception {
        doThrow(new RuntimeException("boom"))
                .when(wrappedPart)
                .delete();

        new CloseablePart(wrappedPart, TRANSACTION_ID).close();

        verify(wrappedPart, times(1)).delete();
        verifyNoMoreInteractions(wrappedPart);
    }

    @Test
    public void close_shouldBeCalledAutomatiucallyWhenUsedInTryWithResources() throws Exception {
        String expected = "Hello world";

        when(wrappedPart.getInputStream())
                .thenReturn(new ByteArrayInputStream(expected.getBytes()));

        try (
                CloseablePart cp = new CloseablePart(wrappedPart, TRANSACTION_ID);
                InputStream in = cp.getInputStream()
        ) {
            String value = IOUtils.toString(in, StandardCharsets.UTF_8);
            assertThat(value, equalTo(expected));
        }

        verify(wrappedPart, times(1)).delete();
        verify(wrappedPart, times(1)).getInputStream();
        verifyNoMoreInteractions(wrappedPart);
    }

    @Test
    public void getPart_shouldReturnPart() {
        assertThat(new CloseablePart(wrappedPart, TRANSACTION_ID).getPart(), equalTo(wrappedPart));
    }

    @Test
    public void getTransactionID_shouldReturnTransactionID() {
        assertThat(new CloseablePart(wrappedPart, TRANSACTION_ID).getTransactionID(), equalTo(TRANSACTION_ID));
    }
}