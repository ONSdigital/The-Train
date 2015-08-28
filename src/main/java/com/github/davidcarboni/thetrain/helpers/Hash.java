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

        long size;

        public ShaInputStream(InputStream input) {
            super(input, DigestUtils.getSha1Digest());
        }

        @Override
        public int read(byte[] b) throws IOException {
            int read = super.read(b);
            size += read;
            return read;
        }

        public String sha() {
            return ByteArray.toString(getMessageDigest().digest());
        }

        public long size() {
            return size;
        }
    }

    /**
     * An extension of {@link DigestInputStream} with a convenience method for getting the SHA1 digest as a String.
     */
    public static class ShaOutputStream extends DigestOutputStream {

        long size;

        public ShaOutputStream(OutputStream output) {
            super(output, DigestUtils.getSha1Digest());
        }

        @Override
        public void write(int b) throws IOException {
            super.write(b);
            size++;
        }

        @Override
        public void write(byte[] b) throws IOException {
            super.write(b);
            size += b.length;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            super.write(b, off, len);
            size += len;
        }

        public String sha() {
            return ByteArray.toBase64String(getMessageDigest().digest());
        }

        public long size() {
            return size;
        }
    }
}
