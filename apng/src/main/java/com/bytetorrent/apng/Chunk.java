package com.bytetorrent.apng;

import android.text.TextUtils;

import java.io.IOException;

class Chunk {
    int length;
    int fourcc;
    int crc;
    int offset;

    static int fourCCToInt(String fourCC) {
        if (TextUtils.isEmpty(fourCC) || fourCC.length() != 4) {
            return 0xbadeffff;
        }
        return (fourCC.charAt(0) & 0xff)
                | (fourCC.charAt(1) & 0xff) << 8
                | (fourCC.charAt(2) & 0xff) << 16
                | (fourCC.charAt(3) & 0xff) << 24
                ;
    }

    void parse(APNGReader reader) throws IOException {
        int available = reader.available();
        innerParse(reader);
        int offset = available - reader.available();
        if (offset > length) {
            throw new IOException("Out of chunk area");
        } else if (offset < length) {
            reader.skip(length - offset);
        }
    }

    void innerParse(APNGReader reader) throws IOException {
    }
}
