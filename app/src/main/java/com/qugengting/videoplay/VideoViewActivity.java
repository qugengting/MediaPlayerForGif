package com.qugengting.videoplay;

import android.os.Bundle;
import android.view.View;
import android.widget.MediaController;
import android.widget.VideoView;

public class VideoViewActivity extends BaseActivity {

    private VideoView videoView;
    private MediaController mediaController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_videoview);
        videoView = (VideoView) findViewById(R.id.videoview);
        mediaController = new MediaController(this);
    }

    @Override
    protected void selectVideoResourceForResult() {
        videoView.setVideoPath(mUrl);
        videoView.setMediaController(mediaController);
        mediaController.setMediaPlayer(videoView);
        videoView.requestFocus();
        videoView.start();
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_set_resource:
                goToSystemVideoListActivity();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
