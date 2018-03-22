package com.qugengting.image.jni;

import android.graphics.Bitmap;

/**
 * Created by xuruibin on 2018/3/22.
 * 描述：
 */

public class ImageUtils {

    static {
        System.loadLibrary("image-lib");
    }

    public static native byte[] getImagePixels(Bitmap bitmap);
}
