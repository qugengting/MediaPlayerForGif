package com.qugengting.videoplay;

import java.io.FileDescriptor;

public interface IMPlayer {

    /**
     * 设置资源
     *
     * @param fileDescriptor 文件描述符
     * @throws MPlayerException
     */
    void setSource(FileDescriptor fileDescriptor) throws MPlayerException;

    /**
     * 设置显示视频的载体
     *
     * @param display 视频播放的载体及相关界面
     */
    void setDisplay(IMDisplay display);

    /**
     * 播放视频
     *
     * @throws MPlayerException
     */
    void play() throws MPlayerException;

    /**
     * 暂停视频
     */
    void pause();

    /**
     * 结束视频
     */
    void stop();

    void seekToPosition(int position);

    int getCurrentPostion();

    void onPause();

    void onResume();

    void onDestroy();

}
