package com.qugengting.videoplay;

public interface IMPlayListener {

    void onStart(IMPlayer player);

    void onPause(IMPlayer player);

    void onResume(IMPlayer player);

    void onComplete(IMPlayer player);

}
