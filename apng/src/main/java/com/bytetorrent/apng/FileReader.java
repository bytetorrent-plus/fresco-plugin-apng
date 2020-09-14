package com.bytetorrent.apng;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FileReader extends FilterReader {
    private final File mFile;

    public FileReader(File file) throws IOException {
        super(new StreamReader(new FileInputStream(file)));
        mFile = file;
    }

    @Override
    public void reset() throws IOException {
        reader.close();
        reader = new StreamReader(new FileInputStream(mFile));
    }
}
