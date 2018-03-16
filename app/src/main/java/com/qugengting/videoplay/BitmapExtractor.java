package com.qugengting.videoplay;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class BitmapExtractor {
    private static final String TAG = BitmapExtractor.class.getSimpleName();
    private static final long US_OF_S = 1000 * 1000;

    private List<Bitmap> bitmaps = new ArrayList<>();
    private int width = 0;
    private int height = 0;
    private long begin = 0;
    private long end = 0;
    private long fps = 5;


    @TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
    public List<Bitmap> createBitmaps(String path) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(path);
        String fileLength = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        Log.e(TAG, "视频总长度是：" + fileLength);//6633964
        double inc = US_OF_S / fps;
        String s = Environment.getExternalStorageDirectory() + File.separator + "zzzTemp" + File.separator;
        for (long i = begin * US_OF_S; i < end * US_OF_S; i += inc) {
            long n = i / 1000 / 1000 / 60 % 60;
            long m = i / 1000 / 1000 % 60;
            Log.e(TAG, "时间是：" + n + "分" + m + "秒");
            Bitmap frame = mmr.getFrameAtTime(i);
            File file = new File(s);
            File file1 = new File(s + i + ".jpg");
            boolean b = saveBitmap(frame, file, file1);
            if (b) Log.e(TAG, "图片" + i + ".jpg保存成功");
            if (frame != null) {
                bitmaps.add(scale(frame));
            }
        }

        return bitmaps;
    }

    public static boolean saveBitmap(Bitmap bitmap, File file, File path) {
        boolean success = false;
        byte[] bytes = bitmapToBytes(bitmap, 70);
        OutputStream out = null;
        try {
            if (!file.exists() && file.isDirectory()) {
                file.mkdirs();
            }
            out = new FileOutputStream(path);
            out.write(bytes);
            out.flush();
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return success;
    }

    /**
     * 将bitmap转换成bytes
     */
    public static byte[] bitmapToBytes(Bitmap bitmap, int quality) {
        if (bitmap == null) {
            return null;
        }
        int size = bitmap.getWidth() * bitmap.getHeight() * 4;
        ByteArrayOutputStream out = new ByteArrayOutputStream(size);
        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, quality, out);
            out.flush();
            out.close();
            return out.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void setScope(int begin, int end) {
        this.begin = begin;
        this.end = end;
    }

    public void setFPS(int fps) {
        this.fps = fps;
    }

    private Bitmap scale(Bitmap bitmap) {
        return Bitmap.createScaledBitmap(bitmap,
                width > 0 ? width : bitmap.getWidth(),
                height > 0 ? height : bitmap.getHeight(),
                true);
    }
}
