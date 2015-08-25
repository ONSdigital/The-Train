package com.github.davidcarboni.thetrain.helpers;

import com.github.davidcarboni.cryptolite.ByteArray;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;

/**
 * Functionality for computing SHA1 hashes in order to verify file integrity and correct functioning of encryption/decryption.
 */
public class Hash {

    public static String sha(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path)) {
            return DigestUtils.sha1Hex(input);
        }
    }

    /**
     * An extension of {@link DigestInputStream} with a convenience method for getting the SHA1 digest as a String.
     */
    public static class ShaInputStream extends DigestInputStream {
        public ShaInputStream(InputStream input) {
            super(input, DigestUtils.getSha1Digest());
        }

        public String sha() {
            return ByteArray.toString(getMessageDigest().digest());
        }
    }

    /**
     * An extension of {@link DigestInputStream} with a convenience method for getting the SHA1 digest as a String.
     */
    public static class ShaOutputStream extends DigestOutputStream {
        public ShaOutputStream(OutputStream output) {
            super(output, DigestUtils.getSha1Digest());
        }

        public String sha() {
            return ByteArray.toBase64String(getMessageDigest().digest());
        }
    }
}
