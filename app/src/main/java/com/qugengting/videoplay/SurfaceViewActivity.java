package com.qugengting.videoplay;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class SurfaceViewActivity extends BaseActivity implements SeekBar.OnSeekBarChangeListener, IMDisplay {
    private static final String TAG = SurfaceViewActivity.class.getSimpleName();
    private SeekBar seekBar;
    private TextView seekBarText;
    private SurfaceView mPlayerView;
    private MPlayer player;

    /**
     * 总时长
     */
    private int duration;
    private String sDuration;
    private int progress;
    private String sProgress;
    private boolean isStarting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        mPlayerView = (SurfaceView) findViewById(R.id.mPlayerView);
        seekBar = (SeekBar) findViewById(R.id.seekbar);
        seekBarText = (TextView) findViewById(R.id.tv_duration);
        seekBar.setVisibility(View.INVISIBLE);
        seekBarText.setVisibility(View.INVISIBLE);
        seekBar.setOnSeekBarChangeListener(this);
        initPlayer();
    }

    private void initPlayer() {
        player = new MPlayer();
        player.setDisplay(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        player.onResume();
        if (handler != null) {
            handler.sendEmptyMessage(WHAT_PLAY);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        player.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        player.onDestroy();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
    }

    @Override
    protected void selectVideoResourceForResult() {
        try {
            player.setSource(mUrl);
            player.play();
        } catch (MPlayerException e) {
            e.printStackTrace();
        }
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.mPlay:
                goToSystemVideoListActivity();
                break;
            case R.id.mPlayerView:
                if (player.isPlaying()) {
                    player.pause();
                } else {
                    try {
                        player.play();
                        if (handler != null) {
                            progress = player.getCurrentPostion() / 1000;
                            handler.removeCallbacksAndMessages(null);
                            handler.sendEmptyMessage(WHAT_PLAY);
                        }
                    } catch (MPlayerException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case R.id.mType:
                player.setCrop(!player.isCrop());
                break;
            case R.id.gotovideoview:
                Intent intent = new Intent(this, VideoViewActivity.class);
                startActivity(intent);
                break;
            case R.id.gotoTextureActivity:
                Intent intent1 = new Intent(this, TextureVideoActivity.class);
                startActivity(intent1);
                break;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        this.progress = progress;
        String sProgress = convert(progress);
        seekBarText.setText(sProgress + " / " + sDuration);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        Log.e(TAG, "开始拖动");
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        Log.e(TAG, "结束拖动");
        player.seekToPosition(progress * 1000);
        if (!player.isPlaying()) {
            try {
                isStarting = true;
                player.play();
            } catch (MPlayerException e) {
                e.printStackTrace();
            }
        }
        handler.sendEmptyMessage(WHAT_PLAY);
    }

    @Override
    public void onStart(IMPlayer player) {
        seekBar.setVisibility(View.VISIBLE);
        seekBarText.setVisibility(View.VISIBLE);
    }

    /**
     * 生成视图的预览
     *
     * @param activity
     * @param v
     * @return 视图生成失败返回null
     * 视图生成成功返回视图的绝对路径
     */
    public String saveImage(Activity activity, View v) {
        Bitmap bitmap;
        String path = getExternalCacheDir() + "/image" + "preview.jpg";
        View view = activity.getWindow().getDecorView();
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();
        bitmap = view.getDrawingCache();
        Rect frame = new Rect();
        activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
        int[] location = new int[2];
        v.getLocationOnScreen(location);
        try {
            bitmap = Bitmap.createBitmap(bitmap, location[0], location[1], v.getWidth(), v.getHeight());
            FileOutputStream fout = new FileOutputStream(path);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fout);
            Log.e(TAG, "生成图片成功");
            File file = new File(path);
            boolean b = file.exists();
            Log.e(TAG, "图片是否存在：" + b + ", 路径是：" + path);
            return path;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
        } finally {
            // 清理缓存
            view.destroyDrawingCache();
        }
        return null;

    }

    SurfaceHolder surfaceHolder;
    int width, height;

    @Override
    public void onPause(IMPlayer player) {
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
//            if (surfaceHolder == null) {
//                surfaceHolder = mPlayerView.getHolder();
//            }
//            saveImage(SurfaceViewActivity.this, mPlayerView);

            width = mPlayerView.getWidth();
            height = mPlayerView.getHeight();
//            SurfaceHolder: Exception locking surface
//            java.lang.IllegalArgumentException

//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    try {
//                        Thread.sleep(1000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//                    Canvas canvas = surfaceHolder.lockCanvas(null);
//                    canvas.drawBitmap(bitmap, 0, 0, null);
//                    surfaceHolder.unlockCanvasAndPost(canvas);
//                    File file = new File(getExternalCacheDir() + "/image" + System.currentTimeMillis() + ".jpg");
//                    FileOutputStream fos = null;
//                    try {
//                        fos = new FileOutputStream(file);
//                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
//                    } catch (FileNotFoundException e) {
//                        e.printStackTrace();
//                    } finally {
//                        if (fos != null) {
//                            try {
//                                fos.close();
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    }
//                }
//            });

//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    BitmapExtractor extractor = new BitmapExtractor();
//                    extractor.setFPS(2);
//                    extractor.setScope(41 * 60 + 40, 41 * 60 + 48);
//                    extractor.setSize(width, height);
//                    extractor.createBitmaps(mUrl);
//                }
//            });

        }
    }

    @Override
    public void onResume(IMPlayer player) {

    }

    @Override
    public void onComplete(IMPlayer player) {
        isStarting = false;
//        if (handler != null) {
//            handler.removeMessages(WHAT_PLAY);
//            seekBarText.setText("00:00:00 / " + sDuration);
//            progress = 0;
//            seekBar.setProgress(progress);
//        }
    }

    @Override
    public View getDisplayView() {
        return mPlayerView;
    }

    @Override
    public SurfaceHolder getHolder() {
        return mPlayerView.getHolder();
    }

    @Override
    public void setDuration(int msec) {
        if (isStarting) {
            return;
        } else {
            isStarting = true;
        }
        duration = msec / 1000;
        seekBar.setMax(duration);
        progress = 0;
        seekBar.setProgress(progress);
        sDuration = convert(duration);
        seekBarText.setText("00:00:00 / " + sDuration);
        player.isPlaying();
        if (handler == null) {
            handler = new MyHandler();
        }
        handler.removeCallbacksAndMessages(null);
        handler.sendEmptyMessage(WHAT_PLAY);
    }

    private MyHandler handler;
    private static final int WHAT_PLAY = 101;
    private static final int WHAT_ZEROING = 102;
    private static final int DURUATION = 1000;

    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int what = msg.what;
            if (what == WHAT_PLAY) {
                progress = player.getCurrentPostion() / 1000;
                if (progress >= duration) {
                    isStarting = false;
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
                isStarting = false;
                seekBarText.setText("00:00:00 / " + sDuration);
                progress = 0;
                seekBar.setProgress(progress);
            }
        }
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
}
