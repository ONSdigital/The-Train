package com.github.davidcarboni.thetrain.helpers;

import com.github.davidcarboni.cryptolite.Random;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 * Created by david on 15/09/2015.
 */
public class UnionInputStreamTest {

    @Test
    public void shouldReadBytesBuffered() throws IOException {

        // Given
        // Two streams for some data
        byte[] data = Random.bytes(1024);
        ByteArrayInputStream a = new ByteArrayInputStream(data, 0, 512);
        ByteArrayInputStream b = new ByteArrayInputStream(data, 512, 1024);
        UnionInputStream input = new UnionInputStream(a, b);

        // When
        // We union the streams (buffered read)
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int r;
        byte[] buffer = new byte[10];
        while ((r=input.read(buffer, 0, 10))>0) {
            output.write(buffer, 0, r);
        }

        // Then
        // We should get the expected data
        Assert.assertArrayEquals(data, output.toByteArray());
    }

    @Test
    public void shouldReadBytesUnbuffered() throws IOException {

        // Given
        // Two streams for some data
        byte[] data = Random.bytes(1024);
        ByteArrayInputStream a = new ByteArrayInputStream(data, 0, 512);
        ByteArrayInputStream b = new ByteArrayInputStream(data, 512, 1024);
        UnionInputStream input = new UnionInputStream(a, b);

        // When
        // We union the streams (unbuffered read)
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int r;
        while ((r=input.read())!=-1) {
            output.write(r);
        }

        // Then
        // We should get the expected data
        Assert.assertArrayEquals(data, output.toByteArray());
    }

    @Test
    public void shouldClose() throws Exception {

        // Given
        // Two mock input streams
        InputStream a = Mockito.mock(InputStream.class);
        InputStream b = Mockito.mock(InputStream.class);
        UnionInputStream input = new UnionInputStream(a, b);

        // When
        // We close the stream
        input.close();

        // Then
        // The underlying streams should be closed
        Mockito.verify(a).close();
        Mockito.verify(b).close();
    }
}