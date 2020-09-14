package com.bytetorrent.apng;

import java.io.IOException;
import java.io.InputStream;

public abstract class StreamLoader implements Loader {
    protected abstract InputStream getInputStream() throws IOException;


    public final synchronized Reader obtain() throws IOException {
        return new StreamReader(getInputStream());
    }
}
