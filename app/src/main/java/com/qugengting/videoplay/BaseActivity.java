package com.qugengting.videoplay;

import android.app.Activity;
import android.content.Intent;

public abstract class BaseActivity extends Activity {
    protected static final int CODE_CHOOSE_VIDEO = 100;
    protected String mUrl = "";

    protected void goToSystemVideoListActivity() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, CODE_CHOOSE_VIDEO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == CODE_CHOOSE_VIDEO) {
                mUrl = UriUtils.getFileAbsolutePath(this, data.getData());
                selectVideoResourceForResult();
            }
        }
    }

    protected abstract void selectVideoResourceForResult();
}
