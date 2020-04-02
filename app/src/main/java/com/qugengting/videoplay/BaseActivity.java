package com.qugengting.videoplay;

import android.content.Intent;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import androidx.appcompat.app.AppCompatActivity;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public abstract class BaseActivity extends AppCompatActivity {
    private static final String TAG = BaseActivity.class.getSimpleName();
    protected static final int CODE_CHOOSE_VIDEO = 100;
    protected String mUrl = "";
    protected FileDescriptor mFileDescriptor;

    protected void goToSystemVideoListActivity() {
        //参考https://developer.android.google.cn/guide/topics/providers/document-provider#java
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("video/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, CODE_CHOOSE_VIDEO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == CODE_CHOOSE_VIDEO) {
                Observable.create(new ObservableOnSubscribe<FileDescriptor>() {
                    @Override
                    public void subscribe(ObservableEmitter<FileDescriptor> subscriber) {
                        //参考：https://developer.android.google.cn/guide/topics/providers/document-provider#java
                        Uri uri = data.getData();
                        ParcelFileDescriptor parcelFileDescriptor = null;
                        try {
                            parcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "r");
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                            subscriber.onNext(null);
                        }
                        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                        subscriber.onNext(fileDescriptor);
                    }
                }).subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Observer<FileDescriptor>() {
                            @Override
                            public void onSubscribe(Disposable d) {

                            }

                            @Override
                            public void onComplete() {

                            }

                            @Override
                            public void onError(Throwable e) {

                            }

                            @Override
                            public void onNext(FileDescriptor s) {
                                mFileDescriptor = s;
                                selectVideoResourceForResult();
                            }
                        });

                mUrl = UriUtils.getFileAbsolutePath(this, data.getData());
            }
        }
    }

    protected abstract void selectVideoResourceForResult();
}
