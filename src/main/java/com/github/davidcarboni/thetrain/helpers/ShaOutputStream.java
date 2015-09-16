package com.github.davidcarboni.thetrain.helpers;


import com.github.davidcarboni.cryptolite.ByteArray;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;

/**
 * An extension of {@link DigestInputStream} with a convenience method for getting the SHA1 digest digest as a String and the size of data written.
 */
public class ShaOutputStream extends DigestOutputStream {

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
        // NB we don't increase the write count here
        // because the super method calls write(b, off, len)
        super.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        super.write(b, off, len);
        size += len;
    }


    public String sha() {
        return ByteArray.toHexString(getMessageDigest().digest());
    }

    public long size() {
        return size;
    }
}

