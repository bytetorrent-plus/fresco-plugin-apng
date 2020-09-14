package com.bytetorrent.apng;

import java.io.IOException;

public interface Loader {
    Reader obtain() throws IOException;
}
