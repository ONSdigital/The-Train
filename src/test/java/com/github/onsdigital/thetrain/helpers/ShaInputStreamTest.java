package com.github.onsdigital.thetrain.helpers;

import com.github.davidcarboni.cryptolite.Random;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link ShaInputStream}.
 */
public class ShaInputStreamTest {

    @Test
    public void testRead() throws IOException {

        // Given
        // Some data with a known SHA
        byte[] bytes = Random.bytes(500);
        String expectedSha = DigestUtils.sha1Hex(bytes);
        int expectedSize = bytes.length;

        // When
        // We read the data through the stream
        String actualSha;
        long actualSize;
        try (ShaInputStream input = new ShaInputStream(new ByteArrayInputStream(bytes))) {
            int b;
            do {
                b = input.read();
            } while (b != -1);
            actualSha = input.sha();
            actualSize = input.size();
        }

        // Then
        // The SHA and size should match
        assertEquals(expectedSize, actualSize);
        assertEquals(expectedSha, actualSha);
    }

    @Test
    public void testReadArray() throws IOException {

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
        try (ShaInputStream input = new ShaInputStream(new ByteArrayInputStream(bytes))) {
            int read;
            do {
                read = input.read(buffer);
            } while (read == buffer.length);
            actualSha = input.sha();
            actualSize = input.size();
        }

        // Then
        // The SHA and size should match
        assertEquals(expectedSize, actualSize);
        assertEquals(expectedSha, actualSha);
    }

    @Test
    public void testReadArrayOffset() throws IOException {

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
        try (ShaInputStream input = new ShaInputStream(new ByteArrayInputStream(bytes))) {
            int read;
            do {
                read = input.read(buffer, 10, 10);
            } while (read == 10);
            actualSha = input.sha();
            actualSize = input.size();
        }

        // Then
        // The SHA and size should match
        assertEquals(expectedSize, actualSize);
        assertEquals(expectedSha, actualSha);
    }
}