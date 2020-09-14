package com.bytetorrent.apng;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;

import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.references.ResourceReleaser;
import com.facebook.imagepipeline.animated.factory.AnimatedFactory;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.core.ImagePipelineFactory;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.image.QualityInfo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static java.lang.Thread.sleep;

class ApngImage {

    static APNGDrawable apngDrawablePlaying;

    static class AnimatedImageHandler implements InvocationHandler {

        APNGDrawable apngDrawable;

        public AnimatedImageHandler(APNGDrawable apngDrawable) {
            this.apngDrawable = apngDrawable;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            Log.d("AnimatedImageHandler", method.getName());

            if(apngDrawable == null || apngDrawable.getFrameSeqDecoder() == null || apngDrawable.getFrameSeqDecoder().frames.size() == 0){
                return null;
            }

            try {

                if(method.getName().equals("getWidth")){

                    Log.d("AnimatedImageHandler", method.getName() + ":" + apngDrawable.getIntrinsicWidth());

                    return apngDrawable.getIntrinsicWidth();
                }

                if(method.getName().equals("getHeight")){

                    Log.d("AnimatedImageHandler", method.getName() + ":" + apngDrawable.getIntrinsicHeight());

                    return apngDrawable.getIntrinsicHeight();
                }

                if(method.getName().equals("getFrameCount")){

                    Log.d("AnimatedImageHandler", method.getName() + ":" + apngDrawable.getFrameSeqDecoder().frames.size());

                    return apngDrawable.getFrameSeqDecoder().frames.size();
                }

                if(method.getName().equals("getDuration")){

                    int duration = 0;

                    for(Object frame : apngDrawable.getFrameSeqDecoder().frames){
                        duration += ((APNGFrame)frame).frameDuration;
                    }

                    Log.d("AnimatedImageHandler", method.getName() + ":" + duration);


                    return duration;
                }

                if(method.getName().equals("getFrameDurations")){

                    int l = apngDrawable.getFrameSeqDecoder().frames.size();
                    int[] duration = new int[l];

                    for(int i = 0; i < l; i++){
                        duration[i] = ((APNGFrame) apngDrawable.getFrameSeqDecoder().frames.get(i)).frameDuration;
                    }

                    Log.d("AnimatedImageHandler", method.getName() + ":" + duration);


                    return duration;
                }

                if(method.getName().equals("getLoopCount")){

                    Log.d("AnimatedImageHandler", method.getName() + ":" + 0);

                    return 0;
                }

                if(method.getName().equals("doesRenderSupportScaling")){

                    Log.d("AnimatedImageHandler", method.getName());

                    return false;
                }

                if(method.getName().equals("getSizeInBytes")){

                    int l = apngDrawable.getFrameSeqDecoder().frames.size();
                    long dataSize = 0;

                    for(int i = 0; i < l; i++){
                        dataSize += ((APNGFrame) apngDrawable.getFrameSeqDecoder().frames.get(i)).dataSize;
                    }

                    Log.d("AnimatedImageHandler", method.getName() + ":" + dataSize);

                    return dataSize;
                }

                if(method.getName().equals("getFrame")){

                    Log.d("AnimatedImageHandler", method.getName() + ":" + args[0]);


                    return ((APNGFrame)apngDrawable.getFrameSeqDecoder().frames.get(((Integer) args[0]).intValue())).getFrame();
                }

                if(method.getName().equals("getFrameInfo")){

                    Log.d("AnimatedImageHandler", method.getName() + ":" + args[0]);

                    return ((APNGFrame)apngDrawable.getFrameSeqDecoder().frames.get(((Integer) args[0]).intValue())).getFrameInfo();
                }

                if(method.getName().equals("dispose")){

                    Log.d("AnimatedImageHandler", method.getName());

                    apngDrawable.stop();

                    apngDrawable = null;

                    return null;
                }
            } catch (Throwable throwable){
                throwable.printStackTrace();
            }
            return null;
        }
    }


    public static CloseableImage decode(final EncodedImage encodedImage, int length, QualityInfo qualityInfo, ImageDecodeOptions options){

//        ImagePipelineConfig
//        AnimatedImage
//        getCloseableImage
        
        Object closeableImageObj = null;

        PooledByteBuffer byteBuffer = encodedImage.getByteBufferRef().get();
        int size = byteBuffer.size();
        byte[] bytes = new byte[size];
        byteBuffer.read(0, bytes, 0, size);

        final InputStream inputStream = new ByteArrayInputStream(bytes);

//        try {
//            if(apngDrawable != null){
//                apngDrawable.stop();
//            }
//        } catch (Throwable throwable){
//            throwable.printStackTrace();
//        }



        APNGDrawable apngDrawable = new APNGDrawable(new StreamLoader() {
            @Override
            protected InputStream getInputStream() throws IOException {
                return inputStream;
            }
        });

        apngDrawable.start();


        try {
            while (apngDrawable == null || apngDrawable.getFrameSeqDecoder() == null || apngDrawable.getFrameSeqDecoder().frames.size() == 0){
                sleep(30);
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(apngDrawable.getFrameSeqDecoder().frames.size() == 1){

            Bitmap bitmap = ((StillFrame)apngDrawable.getFrameSeqDecoder().frames.get(0)).draw(new Canvas(), new Paint(), 1, null, new APNGWriter());

            CloseableStaticBitmap closeableStaticBitmap = new CloseableStaticBitmap(bitmap, new ResourceReleaser<Bitmap>() {
                @Override
                public void release(Bitmap value) {
                    value.recycle();
                }
            }, qualityInfo, 0);

            apngDrawable.stop();

            return closeableStaticBitmap;
        }

        AnimatedImageHandler handler = new AnimatedImageHandler(apngDrawable);

        try {
            Field sInstanceField = ImagePipelineFactory.class.getDeclaredField("sInstance");

            Field mConfig = ImagePipelineFactory.class.getDeclaredField("mConfig");

            mConfig.setAccessible(true);

            sInstanceField.setAccessible(true);

            Field mAnimatedFactoryField = ImagePipelineFactory.class.getDeclaredField("mAnimatedFactory");

            mAnimatedFactoryField.setAccessible(true);

            ImagePipelineFactory sInstance = (ImagePipelineFactory) sInstanceField.get(null);

            Bitmap.Config config = ((ImagePipelineConfig)(mConfig.get(sInstance))).getBitmapConfig();

            AnimatedFactory animatedFactory = (AnimatedFactory) mAnimatedFactoryField.get(sInstance);

            Class mAnimatedFactoryV2Impl = Class.forName("com.facebook.fresco.animation.factory.AnimatedFactoryV2Impl");

            Method mAnimatedImageFactory = mAnimatedFactoryV2Impl.getDeclaredMethod("getAnimatedImageFactory");

            mAnimatedImageFactory.setAccessible(true);

            Object mAnimatedImageFactoryObj = mAnimatedImageFactory.invoke(animatedFactory);

            Class mAnimatedImage = Class.forName("com.facebook.imagepipeline.animated.base.AnimatedImage");

            Class mAnimatedImageFactoryImpl = Class.forName("com.facebook.imagepipeline.animated.factory.AnimatedImageFactoryImpl");

            Method getCloseableImage = mAnimatedImageFactoryImpl.getDeclaredMethod("getCloseableImage", ImageDecodeOptions.class, mAnimatedImage, Bitmap.Config.class);

            getCloseableImage.setAccessible(true);

            Object mAnimatedImageObj = Proxy.newProxyInstance(ApngImage.class.getClassLoader(), new Class[] {mAnimatedImage}, handler);

            closeableImageObj = getCloseableImage.invoke(mAnimatedImageFactoryObj, options, mAnimatedImageObj, config);

        } catch (NoSuchFieldException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }


        return (CloseableImage)closeableImageObj;
    }
}
