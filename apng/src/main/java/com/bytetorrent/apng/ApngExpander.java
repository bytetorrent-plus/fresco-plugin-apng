package com.bytetorrent.apng;

import android.util.Log;

import com.facebook.imageformat.DefaultImageFormats;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.core.ImagePipelineFactory;
import com.facebook.imagepipeline.decoder.DecodeException;
import com.facebook.imagepipeline.decoder.DefaultImageDecoder;
import com.facebook.imagepipeline.decoder.ImageDecoder;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.image.QualityInfo;

import java.lang.reflect.Field;

public class ApngExpander {

    static DefaultImageDecoder decoderInstance;

    static ImageDecoder decoder = new ImageDecoder() {
        @Override
        public CloseableImage decode(EncodedImage encodedImage, int length, QualityInfo qualityInfo, ImageDecodeOptions options) {

            ImageFormat imageFormat = encodedImage.getImageFormat();

            Log.d("decode","apng-decoder>>>type:" + imageFormat.toString() + "," + length);

            if (imageFormat == DefaultImageFormats.PNG){

                CloseableImage closeableImage = ApngImage.decode(encodedImage, length, qualityInfo, options);

                return closeableImage == null ? decoderInstance.decodeStaticImage(encodedImage, options) : closeableImage;
            } else if (imageFormat == DefaultImageFormats.JPEG) {
                return decoderInstance.decodeJpeg(encodedImage, length, qualityInfo, options);
            } else if (imageFormat == DefaultImageFormats.GIF) {
                return decoderInstance.decodeGif(encodedImage, length, qualityInfo, options);
            } else if (imageFormat == DefaultImageFormats.WEBP_ANIMATED) {
                return decoderInstance.decodeAnimatedWebp(encodedImage, length, qualityInfo, options);
            } else if (imageFormat == ImageFormat.UNKNOWN) {
                throw new DecodeException("unknown image format", encodedImage);
            }
            return decoderInstance.decodeStaticImage(encodedImage, options);
        }
    };

    public static void init(){

        try {
            Field sInstanceField = ImagePipelineFactory.class.getDeclaredField("sInstance");

            sInstanceField.setAccessible(true);

            Field mImageDecoderField = ImagePipelineFactory.class.getDeclaredField("mImageDecoder");

            mImageDecoderField.setAccessible(true);

            ImagePipelineFactory sInstance = (ImagePipelineFactory) sInstanceField.get(null);

            DefaultImageDecoder defaultImageDecoder = (DefaultImageDecoder) mImageDecoderField.get(sInstance);

            Field mDefaultDecoderField = DefaultImageDecoder.class.getDeclaredField("mDefaultDecoder");

            mDefaultDecoderField.setAccessible(true);

            decoderInstance = defaultImageDecoder;

            mDefaultDecoderField.set(defaultImageDecoder, decoder);

        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

    }

}
