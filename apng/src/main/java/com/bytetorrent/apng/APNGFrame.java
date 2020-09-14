package com.bytetorrent.apng;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

public class APNGFrame extends Frame<APNGReader, APNGWriter> {
    public byte blend_op;
    public byte dispose_op;
    byte[] ihdrData;
    public long dataSize = 0;
    List<Chunk> imageChunks  = new ArrayList<>();
    List<Chunk> prefixChunks = new ArrayList<>();
    private static final byte[] sPNGSignatures = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};
    private static final byte[] sPNGEndChunk = {0, 0, 0, 0, 0x49, 0x45, 0x4E, 0x44, (byte) 0xAE, 0x42, 0x60, (byte) 0x82};

    private static ThreadLocal<CRC32> sCRC32 = new ThreadLocal<>();

    FrameSeqDecoder mFrameSeqDecoder;

    Object mAnimatedImageFrameObj;

    Object AnimatedDrawableFrameInfoObj;

    class AnimatedImageFrameHandler implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            Log.d("AnimatedImageHandler", "AnimatedImageFrameHandler-" + method.getName());

            try {
                if(method.getName().equals("getWidth")){

                    Log.d("AnimatedImageHandler", method.getName() + ":" + frameWidth);

                    return frameWidth;
                }

                if(method.getName().equals("getHeight")){

                    Log.d("AnimatedImageHandler", method.getName() + ":" + frameHeight);

                    return frameHeight;
                }

                if(method.getName().equals("getXOffset")){

                    Log.d("AnimatedImageHandler", method.getName() + ":" + frameX);

                    return frameX;
                }

                if(method.getName().equals("getYOffset")){

                    Log.d("AnimatedImageHandler", method.getName() + ":" + frameY);

                    return frameY;
                }

                if(method.getName().equals("getDurationMs")){

                    Log.d("AnimatedImageHandler", method.getName() + ":" + frameDuration);

                    return frameDuration;
                }

                if(method.getName().equals("renderFrame")){

                    Log.d("AnimatedImageHandler", method.getName());

                    mFrameSeqDecoder.renderFrame(APNGFrame.this, (Bitmap) args[2]);
                    mFrameSeqDecoder.renderListener.onRender(mFrameSeqDecoder.frameBuffer);
                    return null;
                }

            } catch (Throwable throwable){
                throwable.printStackTrace();
            }
            return null;
        }
    }

    private CRC32 getCRC32() {
        CRC32 crc32 = sCRC32.get();
        if (crc32 == null) {
            crc32 = new CRC32();
            sCRC32.set(crc32);
        }
        return crc32;
    }

    public APNGFrame(APNGReader reader, FCTLChunk fctlChunk, int p, FrameSeqDecoder mFrameSeqDecoder) {
        super(reader);
        this.mFrameSeqDecoder = mFrameSeqDecoder;
        blend_op = fctlChunk.blend_op;
        dispose_op = fctlChunk.dispose_op;
        frameDuration = fctlChunk.delay_num * 1000 / (fctlChunk.delay_den == 0 ? 100 : fctlChunk.delay_den);
        frameWidth = fctlChunk.width;
        frameHeight = fctlChunk.height;
        frameX = fctlChunk.x_offset;
        frameY = fctlChunk.y_offset;

        try {
            Class classAnimatedImageFrame = Class.forName("com.facebook.imagepipeline.animated.base.AnimatedImageFrame");

            mAnimatedImageFrameObj = Proxy.newProxyInstance(APNGFrame.class.getClassLoader(), new Class[]{classAnimatedImageFrame}, new AnimatedImageFrameHandler());


            Class classAnimatedDrawableFrameInfo = Class.forName("com.facebook.imagepipeline.animated.base.AnimatedDrawableFrameInfo");

            Class<Enum> classDisposalMethod = (Class<Enum>) Class.forName("com.facebook.imagepipeline.animated.base.AnimatedDrawableFrameInfo$DisposalMethod");

            Enum[] objectsDisposal = classDisposalMethod.getEnumConstants();

            Class<Enum> classBlendOperation = (Class<Enum>) Class.forName("com.facebook.imagepipeline.animated.base.AnimatedDrawableFrameInfo$BlendOperation");

            Enum[] objectsBlend = classBlendOperation.getEnumConstants();

            Constructor con = classAnimatedDrawableFrameInfo.getConstructor(int.class, int.class, int.class, int.class, int.class, classBlendOperation, classDisposalMethod);

            AnimatedDrawableFrameInfoObj = con.newInstance(p, frameX, frameY, frameWidth, frameHeight, objectsBlend[0], objectsDisposal[dispose_op]);

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }


    }

    private int encode(APNGWriter apngWriter) throws IOException {
        int fileSize = 8 + 13 + 12;

        //prefixChunks
        for (Chunk chunk : prefixChunks) {
            fileSize += chunk.length + 12;
        }

        //imageChunks
        for (Chunk chunk : imageChunks) {
            if (chunk instanceof IDATChunk) {
                fileSize += chunk.length + 12;
            } else if (chunk instanceof FDATChunk) {
                fileSize += chunk.length + 8;
            }
        }
        fileSize += sPNGEndChunk.length;
        apngWriter.reset(fileSize);
        apngWriter.putBytes(sPNGSignatures);
        //IHDR Chunk
        apngWriter.writeInt(13);
        int start = apngWriter.position();
        apngWriter.writeFourCC(IHDRChunk.ID);
        apngWriter.writeInt(frameWidth);
        apngWriter.writeInt(frameHeight);
        apngWriter.putBytes(ihdrData);
        CRC32 crc32 = getCRC32();
        crc32.reset();
        crc32.update(apngWriter.toByteArray(), start, 17);
        apngWriter.writeInt((int) crc32.getValue());

        //prefixChunks
        for (Chunk chunk : prefixChunks) {
            if (chunk instanceof IENDChunk) {
                continue;
            }
            reader.reset();
            reader.skip(chunk.offset);
            reader.read(apngWriter.toByteArray(), apngWriter.position(), chunk.length + 12);
            apngWriter.skip(chunk.length + 12);
        }
        //imageChunks
        for (Chunk chunk : imageChunks) {
            if (chunk instanceof IDATChunk) {
                reader.reset();
                reader.skip(chunk.offset);
                reader.read(apngWriter.toByteArray(), apngWriter.position(), chunk.length + 12);
                apngWriter.skip(chunk.length + 12);
            } else if (chunk instanceof FDATChunk) {
                apngWriter.writeInt(chunk.length - 4);
                start = apngWriter.position();
                apngWriter.writeFourCC(IDATChunk.ID);

                reader.reset();
                // skip to fdat data position
                reader.skip(chunk.offset + 4 + 4 + 4);
                reader.read(apngWriter.toByteArray(), apngWriter.position(), chunk.length - 4);

                apngWriter.skip(chunk.length - 4);
                crc32.reset();
                crc32.update(apngWriter.toByteArray(), start, chunk.length);
                apngWriter.writeInt((int) crc32.getValue());
            }
        }
        //endChunk
        apngWriter.putBytes(sPNGEndChunk);
        return fileSize;
    }


    @Override
    public Bitmap draw(Canvas canvas, Paint paint, int sampleSize, Bitmap reusedBitmap, APNGWriter writer) {
        try {
            int                   length  = encode(writer);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = false;
            options.inSampleSize = sampleSize;
            options.inMutable = true;
            options.inBitmap = reusedBitmap;
            byte[] bytes = writer.toByteArray();
            dataSize = bytes.length;
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, length, options);
            assert bitmap != null;
            canvas.drawBitmap(bitmap, (float) frameX / sampleSize, (float) frameY / sampleSize, paint);
            return bitmap;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Object getFrame() {
        return mAnimatedImageFrameObj;
    }

    public Object getFrameInfo() {
        return AnimatedDrawableFrameInfoObj;
    }
}
