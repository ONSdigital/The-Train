package com.github.onsdigital.thetrain.helpers;

import com.github.davidcarboni.cryptolite.ByteArray;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;

/**
 * An extension of {@link DigestInputStream} with a convenience method for getting the SHA1 digest as a String and the size of data written.
 */
public class ShaInputStream extends DigestInputStream {

    long size;

    public ShaInputStream(InputStream input) {
        super(input, DigestUtils.getSha1Digest());
    }

    @Override
    public int read() throws IOException {
        int read = super.read();
        if (read != -1) size++;
        return read;
    }

    @Override
    public int read(byte[] b) throws IOException {
        // NB we don't increase the read count here
        // because the super method calls read(b, off, len)
        return super.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int read = super.read(b, off, len);
        if (read > 0) size += read;
        return read;
    }

    public String sha() {
        return ByteArray.toHexString(getMessageDigest().digest());
    }

    public long size() {
        return size;
    }
}
