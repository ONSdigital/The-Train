package com.github.onsdigital.thetrain.helpers;

import com.github.davidcarboni.cryptolite.Random;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang.ArrayUtils;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link ShaOutputStream}.
 */
public class ShaOutputStreamTest {

    @Test
    public void testWrite() throws IOException {

        // Given
        // Some data with a known SHA
        byte[] bytes = Random.bytes(500);
        String expectedSha = DigestUtils.sha1Hex(bytes);
        int expectedSize = bytes.length;

        // When
        // We read the data through the stream
        String actualSha;
        long actualSize;
        try (InputStream input = new ByteArrayInputStream(bytes);
             ShaOutputStream output = new ShaOutputStream(new NullOutputStream())) {
            int b;
            while ((b=input.read())!=-1) {
                output.write(b);
            }
            actualSha = output.sha();
            actualSize = output.size();
        }

        // Then
        // The SHA and size should match
        assertEquals(expectedSize, actualSize);
        assertEquals(expectedSha, actualSha);
    }

    @Test
    public void testWriteArray() throws IOException {

        // Given
        // Some data with a known SHA
        byte[] bytes = Random.bytes(500);
        String expectedSha = DigestUtils.sha1Hex(bytes);
        int expectedSize = bytes.length;

        // When
        // We read the data through the stream
        String actualSha;
        long actualSize;
        byte[] buffer = new byte[40];
        try (InputStream input = new ByteArrayInputStream(bytes);
             ShaOutputStream output = new ShaOutputStream(new NullOutputStream())) {
            int read;
            while ((read=input.read(buffer))>0) {
                output.write(ArrayUtils.subarray(buffer, 0, read));
            }
            actualSha = output.sha();
            actualSize = output.size();
        }

        // Then
        // The SHA and size should match
        assertEquals(expectedSize, actualSize);
        assertEquals(expectedSha, actualSha);
    }

    @Test
    public void testWriteArrayOffset() throws IOException {

        // Given
        // Some data with a known SHA
        byte[] bytes = Random.bytes(500);
        String expectedSha = DigestUtils.sha1Hex(bytes);
        int expectedSize = bytes.length;

        // When
        // We read the data through the stream
        String actualSha;
        long actualSize;
        byte[] buffer = new byte[40];
        try (InputStream input = new ByteArrayInputStream(bytes);
             ShaOutputStream output = new ShaOutputStream(new NullOutputStream())) {
            int read;
            while ((read=input.read(buffer))>0) {
                output.write(buffer, 0, read);
            }
            actualSha = output.sha();
            actualSize = output.size();
        }

        // Then
        // The SHA and size should match
        assertEquals(expectedSha, actualSha);
        assertEquals(expectedSize, actualSize);
    }
}