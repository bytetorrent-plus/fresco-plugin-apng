package com.bytetorrent.apng;

import java.io.IOException;

class ACTLChunk extends Chunk {
    static final int ID = Chunk.fourCCToInt("acTL");
    int num_frames;
    int num_plays;

    @Override
    void innerParse(APNGReader apngReader) throws IOException {
        num_frames = apngReader.readInt();
        num_plays = apngReader.readInt();
    }
}
