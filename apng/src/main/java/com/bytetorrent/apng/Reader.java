package com.bytetorrent.apng;

import java.io.IOException;
import java.io.InputStream;

public interface Reader {
    long skip(long total) throws IOException;

    byte peek() throws IOException;

    void reset() throws IOException;

    int position();

    int read(byte[] buffer, int start, int byteCount) throws IOException;

    int available() throws IOException;

    /**
     * close io
     */
    void close() throws IOException;

    InputStream toInputStream() throws IOException;
}
