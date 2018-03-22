#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <jni.h>
#include <android/bitmap.h>
#include <android/log.h>
#include "com_qugengting_image_jni_ImageUtils.h"
#ifndef eprintf
#define eprintf(...) __android_log_print(ANDROID_LOG_ERROR,"@",__VA_ARGS__)
#endif
#define RGB565_R(p) ((((p) & 0xF800) >> 11) << 3)
#define RGB565_G(p) ((((p) & 0x7E0 ) >> 5)  << 2)
#define RGB565_B(p) ( ((p) & 0x1F  )        << 3)
#define MAKE_RGB565(r,g,b) ((((r) >> 3) << 11) | (((g) >> 2) << 5) | ((b) >> 3))

#define RGBA_A(p) (((p) & 0xFF000000) >> 24)
#define RGBA_R(p) (((p) & 0x00FF0000) >> 16)
#define RGBA_G(p) (((p) & 0x0000FF00) >>  8)
#define RGBA_B(p) (((p) & 0x000000FF) >>  0)
#define MAKE_RGBA(r,g,b,a) (((a) << 24) | ((r) << 16) | ((g) << 8) | (b))

JNIEXPORT jbyteArray JNICALL Java_com_qugengting_image_jni_ImageUtils_getImagePixels
        (JNIEnv *env, jclass clazz, jobject zBitmap) {
    JNIEnv J = *env;
    // Get bitmap info
    AndroidBitmapInfo info;
    memset(&info, 0, sizeof(info));
    AndroidBitmap_getInfo(env, zBitmap, &info);
    // Check format, only RGB565 & RGBA are supported
    if (info.width <= 0 || info.height <= 0 ||
        (info.format != ANDROID_BITMAP_FORMAT_RGB_565 && info.format != ANDROID_BITMAP_FORMAT_RGBA_8888)) {
        eprintf("invalid bitmap\n");
        return NULL;
    }
    // Lock the bitmap to get the buffer
    void * pixels = NULL;
    int res = AndroidBitmap_lockPixels(env, zBitmap, &pixels);
    if (pixels == NULL) {
        eprintf("fail to lock bitmap: %d\n", res);
        return NULL;
    }
    eprintf("Effect: %dx%d, %d\n", info.width, info.height, info.format);

    // int w = bitmap.getWidth();
    // int h = bitmap.getHeight();
    // pixels = new byte[w * h * 3];
    // for (int i = 0; i < h; i++) {
    //     int stride = w * 3 * i;
    //     for (int j = 0; j < w; j++) {
    //         int p = bitmap.getPixel(j, i);
    //         int step = j * 3;
    //         int offset = stride + step;
    //         pixels[offset + 0] = (byte) ((p & 0x000000FF) >> 0); // blue
    //         pixels[offset + 1] = (byte) ((p & 0x0000FF00) >> 8); // green
    //         pixels[offset + 2] = (byte) ((p & 0x00FF0000) >> 16); // red
    //     }
    //                    #define RGBA_R(p) (((p) & 0x00FF0000) >> 16)
    //                    #define RGBA_G(p) (((p) & 0x0000FF00) >>  8)
    //                    #define RGBA_B(p)  ((p) & 0x000000FF)
    // }
    int x = 0, y = 0;
    int w = info.width;
    int h = info.height;
    jbyteArray jbarr = env->NewByteArray(w * h * 3);
    jbyte buf[w * h * 3];
    // From top to bottom
    for (y = 0; y < h; y++) {
        int stride = w * 3 * y;
        // From left to right
        for (x = 0; x < w; x++) {
            int a = 0, r = 0, g = 0, b = 0;
            int step = x * 3;
            int offset = stride + step;
            void *pixel = NULL;
            // Get each pixel by format
            if (info.format == ANDROID_BITMAP_FORMAT_RGB_565) {
                pixel = ((uint16_t *)pixels) + y * info.width + x;
                uint16_t v = *(uint16_t *)pixel;
                r = RGB565_R(v);
                g = RGB565_G(v);
                b = RGB565_B(v);
            } else {// ARGB_8888
                //如果是ARGB_8888，图片颜色被弱化，几近黑白，不明白为什么
                pixel = ((uint32_t *)pixels) + y * info.width + x;
                uint32_t v = *(uint32_t *)pixel;
                a = RGBA_A(v);
                r = RGBA_R(v);
                g = RGBA_G(v);
                b = RGBA_B(v);
            }
            buf[offset + 0] = (jbyte)b;
            buf[offset + 1] = (jbyte)g;
            buf[offset + 2] = (jbyte)r;
        }
    }
    AndroidBitmap_unlockPixels(env, zBitmap);
    //赋值
    env->SetByteArrayRegion(jbarr, 0, w * h * 3, buf);
    return jbarr;
}


