package com.github.onsdigital.thetrain.helpers;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * This class is inspired by {@link java.io.SequenceInputStream SequenceInputStream},
 * but this class does not close the underlying streams until {@link #close()} is called.
 */
public class UnionInputStream extends InputStream {

    InputStream a;
    InputStream b;
    InputStream in;

    /**
     * Initializes a newly
     * created <code>UnionInputStream</code>
     * by remembering the two arguments, which
     * will be read in order, first <code>a</code>
     * and then <code>b</code>, to provide the
     * bytes to be read from this <code>UnionInputStream</code>.
     *
     * @param a the first input stream to read.
     * @param b the second input stream to read.
     */
    public UnionInputStream(InputStream a, InputStream b) {
        this.a = a;
        this.b = b;
        in = a;
    }

    /**
     * Continues reading in the next stream if an EOF is reached.
     */
    final void nextStream() throws IOException {
        if (in == a) {
            in = b;
        } else {
            in = null;
        }
    }

    /**
     * Returns an estimate of the number of bytes that can be read (or
     * skipped over) from the current underlying input stream without
     * blocking by the next invocation of a method for the current
     * underlying input stream. The next invocation might be
     * the same thread or another thread.  A single read or skip of this
     * many bytes will not block, but may read or skip fewer bytes.
     * <p>
     * This method simply calls {@code available} of the current underlying
     * input stream and returns the result.
     *
     * @return an estimate of the number of bytes that can be read (or
     * skipped over) from the current underlying input stream
     * without blocking or {@code 0} if this input stream
     * has been closed by invoking its {@link #close()} method
     * @throws IOException if an I/O error occurs.
     */
    public int available() throws IOException {
        if (in == null) {
            return 0; // no way to signal EOF from available()
        }
        return in.available();
    }

    /**
     * Reads the next byte of data from this input stream. The byte is
     * returned as an <code>int</code> in the range <code>0</code> to
     * <code>255</code>. If no byte is available because the end of the
     * stream has been reached, the value <code>-1</code> is returned.
     * This method blocks until input data is available, the end of the
     * stream is detected, or an exception is thrown.
     * <p>
     * This method
     * tries to read one character from the current substream. If it
     * reaches the end of the stream, it begins reading from the next
     * substream.
     *
     * @return the next byte of data, or <code>-1</code> if the end of the
     * stream is reached.
     * @throws IOException if an I/O error occurs.
     */
    public int read() throws IOException {
        while (in != null) {
            int c = in.read();
            if (c != -1) {
                return c;
            }
            nextStream();
        }
        return -1;
    }

    /**
     * Reads up to <code>len</code> bytes of data from this input stream
     * into an array of bytes.  If <code>len</code> is not zero, the method
     * blocks until at least 1 byte of input is available; otherwise, no
     * bytes are read and <code>0</code> is returned.
     * <p>
     * The <code>read</code> method of <code>SequenceInputStream</code>
     * tries to read the data from the current substream. If it fails to
     * read any characters because the substream has reached the end of
     * the stream, it calls the <code>close</code> method of the current
     * substream and begins reading from the next substream.
     *
     * @param b   the buffer into which the data is read.
     * @param off the start offset in array <code>b</code>
     *            at which the data is written.
     * @param len the maximum number of bytes read.
     * @return int   the number of bytes read.
     * @throws NullPointerException      If <code>b</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException If <code>off</code> is negative,
     *                                   <code>len</code> is negative, or <code>len</code> is greater than
     *                                   <code>b.length - off</code>
     * @throws IOException               if an I/O error occurs.
     */
    public int read(byte b[], int off, int len) throws IOException {
        if (in == null) {
            return -1;
        } else if (b == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }
        do {
            int n = in.read(b, off, len);
            if (n > 0) {
                return n;
            }
            nextStream();
        } while (in != null);
        return -1;
    }

    /**
     * Closes the underlying input streams and releases any system resources
     * associated with them.
     * A closed <code>SequenceInputStream</code>
     * cannot  perform input operations and cannot
     * be reopened.
     *
     * @throws IOException if an I/O error occurs.
     */
    public void close() throws IOException {
        IOUtils.closeQuietly(a);
        // TODO explain why this is not closed.
        //IOUtils.closeQuietly(b);
    }
}
