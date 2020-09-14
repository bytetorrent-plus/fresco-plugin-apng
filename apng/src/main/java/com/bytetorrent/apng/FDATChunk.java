package com.bytetorrent.apng;

import java.io.IOException;

class FDATChunk extends Chunk {
    static final int ID = Chunk.fourCCToInt("fdAT");
    int sequence_number;

    @Override
    void innerParse(APNGReader reader) throws IOException {
        sequence_number = reader.readInt();
    }
}
