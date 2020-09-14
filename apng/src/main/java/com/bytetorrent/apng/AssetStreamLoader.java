package com.bytetorrent.apng;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;

public class AssetStreamLoader extends StreamLoader {

    private final Context mContext;
    private final String  mAssetName;

    public AssetStreamLoader(Context context, String assetName) {
        mContext = context.getApplicationContext();
        mAssetName = assetName;
    }

    @Override
    protected InputStream getInputStream() throws IOException {
        return mContext.getAssets().open(mAssetName);
    }
}
