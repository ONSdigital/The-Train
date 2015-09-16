package com.github.davidcarboni.thetrain.helpers;

import com.github.davidcarboni.cryptolite.Random;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests for {@link Hash}.
 */
public class HashTest {

    @Test
    public void shouldHashFile() throws Exception {

        // Given
        // A file with some content
        Path path = Files.createTempFile("shouldHashFile", "HashTest");
        try (InputStream input = Random.inputStream(700); OutputStream output = Files.newOutputStream(path)) {
            IOUtils.copy(input, output);
        }
        String expectedSha;
        try (InputStream input = Files.newInputStream(path)) {
            expectedSha = DigestUtils.sha1Hex(input);
        }

        // When
        // We comptute the file SHA
        String actualSha = Hash.sha(path);

        // Then
        // The hashes should be the same
        Assert.assertEquals(expectedSha, actualSha);
    }
}