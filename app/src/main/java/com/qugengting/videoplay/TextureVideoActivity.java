package com.qugengting.videoplay;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.qugengting.image.encoder.GIFEncoderWithSingleFrame;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TextureVideoActivity extends BaseActivity implements NumberPicker.OnValueChangeListener, TextureView.SurfaceTextureListener, View.OnClickListener, SeekBar.OnSeekBarChangeListener {
    private static final String TAG = TextureVideoActivity.class.getName();

    private MediaPlayer mMediaPlayer;
    private TextureView mPreview;
    private Button btnScreenShots;
    private Button btnStartPlay;
    private Button btnPausePlay;
    private Button btnResource;
    private Button btnBack;
    private Button btnForward;
    private Button btnReset;
    private NumberPicker npFrameRate;
    private NumberPicker npLength;
    private NumberPicker npCompressRate;
    private SeekBar seekBar;
    private TextView seekBarText;
    private int duration;
    private int progress;
    private String sProgress;
    private String sDuration;
    private boolean isLoad = false;//视频是否已加载
    private Surface surface;
    private String lastUrl = "";

    private static final String[] FRAME_RATES = {"100", "200", "250"};
    private static final String[] LENGS = {"3", "4", "5", "6", "7", "8", "9", "10"};
    private static final String[] COMPRESS_SIZE_DECRIPTION = {"清晰", "普通", "标清"};
    private int[] compressSizeArr = {720, 440, 220};
    private int frameRate = 100;
    private int length = 5;
    private int compressSize = compressSizeArr[1];

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        mUrl = Environment.getExternalStorageDirectory() + File.separator + "jFr8XpNg.mp4";
        setContentView(R.layout.activity_texture_video);
        btnScreenShots = (Button) findViewById(R.id.btn_screenshots);
        btnStartPlay = (Button) findViewById(R.id.btn_start);
        btnPausePlay = (Button) findViewById(R.id.btn_pause);
        btnResource = (Button) findViewById(R.id.btn_set_resource);
        btnBack = (Button) findViewById(R.id.btn_back);
        btnForward = (Button) findViewById(R.id.btn_fast_forward);
        btnReset = (Button) findViewById(R.id.btn_reset);
        btnReset.setOnClickListener(this);
        btnForward.setOnClickListener(this);
        btnBack.setOnClickListener(this);
        btnResource.setOnClickListener(this);
        btnPausePlay.setOnClickListener(this);
        btnStartPlay.setOnClickListener(this);
        btnScreenShots.setOnClickListener(this);

        seekBar = (SeekBar) findViewById(R.id.seekbar);
        seekBar.setOnSeekBarChangeListener(this);
        seekBarText = (TextView) findViewById(R.id.tv_duration);
        seekBar.setVisibility(View.GONE);
        seekBarText.setVisibility(View.GONE);

        npFrameRate = (NumberPicker) findViewById(R.id.np_frame_rate);
        npLength = (NumberPicker) findViewById(R.id.np_length);
        npCompressRate = (NumberPicker) findViewById(R.id.np_compress_size);
        npFrameRate.setDisplayedValues(FRAME_RATES);
        npFrameRate.setMinValue(0);
        npFrameRate.setMaxValue(2);
        npFrameRate.setValue(0);
        npFrameRate.setWrapSelectorWheel(false);
        npFrameRate.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        npFrameRate.setOnValueChangedListener(this);
        npLength.setDisplayedValues(LENGS);
        npLength.setMinValue(0);
        npLength.setMaxValue(7);
        npLength.setValue(2);
        npLength.setWrapSelectorWheel(false);
        npLength.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        npLength.setOnValueChangedListener(this);
        npCompressRate.setDisplayedValues(COMPRESS_SIZE_DECRIPTION);
        npCompressRate.setMinValue(0);
        npCompressRate.setMaxValue(2);
        npCompressRate.setValue(1);
        npCompressRate.setWrapSelectorWheel(false);
        npCompressRate.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        npCompressRate.setOnValueChangedListener(this);

        mPreview = (TextureView) findViewById(R.id.textureView);
        mPreview.post(new Runnable() {
            @Override
            public void run() {
                LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) mPreview.getLayoutParams();
                layoutParams.width = mPreview.getWidth();
                //设置宽高比为16 : 9
                layoutParams.height = mPreview.getWidth() * 9 / 16;
                mPreview.setLayoutParams(layoutParams);
                mPreview.requestLayout();
                mPreview.setSurfaceTextureListener(TextureVideoActivity.this);
            }
        });

    }

    public Bitmap getBitmap() {
        return mPreview.getBitmap();
    }

    /**
     * dp转换px
     */
    public int dip2px(Context context, float dipValue) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, context.getResources()
                .getDisplayMetrics());
    }

    private float videoWidth;
    private float videoHeight;
    private int videoRotation;

    private void initVideoSize() {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            mmr.setDataSource(mUrl);
            String width = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String height = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            String rotation = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
            videoWidth = Float.valueOf(width);
            videoHeight = Float.valueOf(height);
            videoRotation = Integer.valueOf(rotation);
            int w1;
            if (videoRotation == 90) {
                w1 = (int) ((videoHeight / videoWidth) * dip2px(TextureVideoActivity.this, 250));
            } else {
                w1 = (int) (videoWidth / videoHeight * dip2px(TextureVideoActivity.this, 250));
            }
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) mPreview.getLayoutParams();
            layoutParams.width = w1;
            layoutParams.height = mPreview.getHeight();
            mPreview.setLayoutParams(layoutParams);
        } catch (Exception ex) {
            Log.e(TAG, "MediaMetadataRetriever exception " + ex);
        } finally {
            mmr.release();
        }
    }

    private void startPlay() {
        initVideoSize();
        try {
            if (mMediaPlayer == null) {
                mMediaPlayer = new MediaPlayer();
            }
            seekBar.setVisibility(View.VISIBLE);
            seekBarText.setVisibility(View.VISIBLE);
            if (isLoad) {
                mMediaPlayer.stop();
                mMediaPlayer.reset();
                mMediaPlayer.setDataSource(this, Uri.parse(mUrl));
                mMediaPlayer.setSurface(surface);
                mMediaPlayer.setLooping(false);
                mMediaPlayer.prepareAsync();
                mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mediaPlayer) {
                        mediaPlayer.start();
                        if (mUrl.equals(lastUrl)) {//重回界面，播放源不变
                            mediaPlayer.seekTo(progress * 1000);
                        } else {//重回界面，播放源更改
                            duration = mediaPlayer.getDuration() / 1000;
                            sDuration = convert(duration);
                            seekBarText.setText("00:00:00 / " + sDuration);
                            seekBar.setMax(duration);
                            seekBar.setProgress(0);
                        }
                        if (handler == null) {
                            handler = new MyHandler();
                        }
                        handler.sendEmptyMessage(WHAT_PLAY);
                    }
                });
            } else {
                mMediaPlayer.setDataSource(this, Uri.parse(mUrl));
                mMediaPlayer.setSurface(surface);
                mMediaPlayer.setLooping(false);

                // don't forget to call MediaPlayer.prepareAsync() method when you use constructor for
                // creating MediaPlayer
                mMediaPlayer.prepareAsync();
                // Play video when the media source is ready for playback.
                mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mediaPlayer) {
                        mediaPlayer.start();
                        duration = mediaPlayer.getDuration() / 1000;
                        sDuration = convert(duration);
                        seekBarText.setText("00:00:00 / " + sDuration);
                        seekBar.setMax(duration);
                        seekBar.setProgress(0);

                        isLoad = true;
                        if (handler == null) {
                            handler = new MyHandler();
                        }
                        handler.sendEmptyMessage(WHAT_PLAY);
                    }
                });
                lastUrl = mUrl;
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        surface = new Surface(surfaceTexture);
        if (!TextUtils.isEmpty(mUrl)) {
            startPlay();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.btn_screenshots:
                bitmaps.clear();
                bitmapPaths.clear();
                Toast.makeText(this, "开始截图", Toast.LENGTH_SHORT).show();
                if (handler == null) {
                    handler = new MyHandler();
                }
                handler.sendEmptyMessage(WHAT_SCREENSHOTS);
                break;
            case R.id.btn_start:
                if (mMediaPlayer != null) {
                    mMediaPlayer.start();
                    if (handler != null) {
                        progress = mMediaPlayer.getCurrentPosition() / 1000;
                        if (handler.hasMessages(WHAT_PLAY)) {
                            handler.removeMessages(WHAT_PLAY);
                        }
                        handler.sendEmptyMessage(WHAT_PLAY);
                    }
                }
                break;
            case R.id.btn_pause:
                if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                    mMediaPlayer.pause();
                    if (handler.hasMessages(WHAT_PLAY)) {
                        handler.removeMessages(WHAT_PLAY);
                    }
                }
                break;
            case R.id.btn_set_resource:
                goToSystemVideoListActivity();
                break;
            case R.id.btn_back:
                if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                    if (progress - 10 >= 0) {
                        progress -= 10;
                        sProgress = convert(progress);
                        seekBarText.setText(sProgress + " / " + sDuration);
                        mMediaPlayer.seekTo(progress * 1000);
                    }
                }
                break;
            case R.id.btn_fast_forward:
                if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                    if (progress + 10 <= duration) {
                        progress += 10;
                        sProgress = convert(progress);
                        seekBarText.setText(sProgress + " / " + sDuration);
                        mMediaPlayer.seekTo(progress * 1000);
                    }
                }
                break;
            case R.id.btn_reset:
                npFrameRate.setValue(1);
                npLength.setValue(1);
                npCompressRate.setValue(3);
                break;
        }
    }

    @Override
    protected void selectVideoResourceForResult() {
    }

    private static final int WHAT_PLAY = 104;
    private static final int WHAT_SCREENSHOTS = 103;
    private static final int WHAT_ZEROING = 105;
    private static final int DURUATION = 1000;
    private int count = 0;
    List<Bitmap> bitmaps = new ArrayList<>();
    List<String> bitmapPaths = new ArrayList<>();
    private MyHandler handler;

    @Override
    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
        int id = picker.getId();
        switch (id) {
            case R.id.np_frame_rate:
                frameRate = Integer.valueOf(FRAME_RATES[newVal]);
                break;
            case R.id.np_length:
                length = Integer.valueOf(LENGS[newVal]);
                break;
            case R.id.np_compress_size:
                compressSize = compressSizeArr[newVal];
                break;
        }
    }

    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int what = msg.what;
            if (what == WHAT_SCREENSHOTS) {
                if (count >= 1000 / frameRate * length) {
                    count = 0;
                    Toast.makeText(TextureVideoActivity.this, "截图结束，开始转换成gif", Toast.LENGTH_SHORT).show();
                    new MyThread().start();
                    return;
                }
                Bitmap bitmap = getBitmap();
                String path = getExternalCacheDir() + File.separator + String.valueOf(count + 1) + ".jpg";
                bitmapPaths.add(path);
                BitmapSizeUtils.compressSize(bitmap, path, compressSize, 80);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                //转化为RGB_565，使用bitmap.copy(Bitmap.Config.RGB_565, false)无效
                Bitmap bmp = BitmapFactory.decodeFile(path, options);
                //压缩后再添加
                bitmaps.add(bmp);
                count++;
                sendEmptyMessageDelayed(WHAT_SCREENSHOTS, frameRate);
            } else if (what == WHAT_PLAY) {
                progress = mMediaPlayer.getCurrentPosition() / 1000;
                if (progress >= duration) {
                    seekBarText.setText("00:00:00 / " + sDuration);
                    progress = 0;
                } else if (progress == duration - 1) {
                    sProgress = convert(progress);
                    seekBarText.setText(sProgress + " / " + sDuration);
                    sendEmptyMessageDelayed(WHAT_ZEROING, DURUATION);
                } else {
                    sProgress = convert(progress);
                    seekBarText.setText(sProgress + " / " + sDuration);
                    sendEmptyMessageDelayed(WHAT_PLAY, DURUATION);
                }
                seekBar.setProgress(progress);
            } else if (what == WHAT_ZEROING) {
                seekBarText.setText("00:00:00 / " + sDuration);
                progress = 0;
                seekBar.setProgress(progress);
            }
        }
    }

    class MyThread extends Thread {
        @Override
        public void run() {
            super.run();
            //单线程制作gif，速度太慢
//            String fileName = getExternalCacheDir() + String.valueOf(System.currentTimeMillis()) + ".gif";
//            String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + fileName;
//            GIFEncoder encoder = new GIFEncoder();
//            encoder.init(bitmaps.get(0));
//            encoder.setFrameRate(1000 / DURATION);
//            encoder.start(filePath);
//            for (int i = 1; i < bitmaps.size(); i++) {
//                encoder.addFrame(bitmaps.get(i));
//                Log.e(TAG, "总共" + bitmaps.size() + "帧，正在添加第" + (i + 1) + "帧");
//            }
//            encoder.finish();
//            handler.post(new Runnable() {
//                @Override
//                public void run() {
//                    Toast.makeText(TextureVideoActivity.this, "gif生成成功", Toast.LENGTH_SHORT).show();
//                }
//            });

            List<String> fileParts = new ArrayList<>();
            ExecutorService service = Executors.newCachedThreadPool();
            final CountDownLatch countDownLatch = new CountDownLatch(bitmaps.size());
            for (int i = 0; i < bitmaps.size(); i++) {
                final int n = i;
                final String fileName = getExternalCacheDir() + File.separator + (n + 1) + ".partgif";
                fileParts.add(fileName);
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        GIFEncoderWithSingleFrame encoder = new GIFEncoderWithSingleFrame();
                        encoder.setFrameRate(1000 / frameRate / 1.4f);
                        Log.e(TAG, "总共" + bitmaps.size() + "帧，正在添加第" + (n + 1) + "帧");
                        if (n == 0) {
                            encoder.addFirstFrame(fileName, bitmaps.get(n));
                        } else if (n == bitmaps.size() - 1) {
                            encoder.addLastFrame(fileName, bitmaps.get(n));
                        } else {
                            encoder.addFrame(fileName, bitmaps.get(n));
                        }
                        countDownLatch.countDown();
                    }
                };
                service.execute(runnable);
            }
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(TextureVideoActivity.this, "gif初始化成功，准备合并", Toast.LENGTH_SHORT).show();
                }
            });
            SequenceInputStream sequenceInputStream = null;
            FileOutputStream fos = null;
            try {
                Vector<InputStream> streams = new Vector<InputStream>();
                for (String filePath : fileParts) {
                    InputStream inputStream = new FileInputStream(filePath);
                    streams.add(inputStream);
                }
                sequenceInputStream = new SequenceInputStream(streams.elements());
                File file = new File(getExternalCacheDir() + File.separator + System.currentTimeMillis() + ".gif");
                if (!file.exists()) {
                    file.createNewFile();
                }
                fos = new FileOutputStream(file);
                byte[] buffer = new byte[1024];
                int len = 0;
                // byteread表示一次读取到buffers中的数量。
                while ((len = sequenceInputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
                fos.flush();
                fos.close();
                sequenceInputStream.close();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(TextureVideoActivity.this, "gif制作完成", Toast.LENGTH_SHORT).show();
                    }
                });
                for (String filePath : fileParts) {
                    File f = new File(filePath);
                    if (f.exists()) {
                        f.delete();
                    }
                }
                fileParts.clear();
                for (String bitmapPath : bitmapPaths) {
                    File f = new File(bitmapPath);
                    if (f.exists()) {
                        f.delete();
                    }
                }
                bitmapPaths.clear();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (sequenceInputStream != null) {
                    try {
                        sequenceInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        this.progress = progress;
        sProgress = convert(progress);
        seekBarText.setText(sProgress + " / " + sDuration);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        if (handler.hasMessages(WHAT_PLAY)) {
            handler.removeMessages(WHAT_PLAY);
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mMediaPlayer.seekTo(progress * 1000);
        if (!mMediaPlayer.isPlaying()) {
            mMediaPlayer.start();
        }
        handler.sendEmptyMessage(WHAT_PLAY);
    }

    private int hour, munite, second;
    private String sHour, sMunite, sSecond;

    private String convert(int time) {
        String sTime = "";
        hour = time / 3600;
        if (hour == 0) {
            sHour = "00";
        } else {
            sHour = String.valueOf(hour);
        }
        munite = time / 60 % 60;
        if (munite >= 0 && munite < 10) {
            sMunite = "0" + munite;
        } else {
            sMunite = String.valueOf(munite);
        }
        second = time % 60;
        if (second >= 0 && second < 10) {
            sSecond = "0" + second;
        } else {
            sSecond = String.valueOf(second);
        }
        sTime = sHour + ":" + sMunite + ":" + sSecond;
        return sTime;
    }

    @Override
    protected void onResume() {
        super.onResume();
//        if (handler != null) {
//            handler.sendEmptyMessage(WHAT_PLAY);
//            mMediaPlayer.start();
//        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (handler != null) {
            if (handler.hasMessages(WHAT_PLAY)) {
                handler.removeMessages(WHAT_PLAY);
            }
        }
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
        if (mMediaPlayer != null) {
            // Make sure we stop video and release resources when activity is destroyed.
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        isLoad = false;
    }
}
