package com.github.davidcarboni.thetrain.helpers;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Functionality for computing SHA1 hashes in order to verify file integrity and correct functioning of encryption/decryption.
 */
public class Hash {

    public static String sha(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path)) {
            return DigestUtils.sha1Hex(input);
        }
    }

}
