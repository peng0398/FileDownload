package com.example.bob.myapplication;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.HttpHandler;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;

import java.io.File;

/**
 * Author:Bob
 * Date: 2016/8/23.
 */
public class DownloadService extends Service {

    private static final String TAG = "DownloadService";
    private static Messenger clientMessenger;
    private String FILE_URL = "http://192.168.30.135:8080/ota_update.zip";
    private HttpHandler<File> httpHandler;
    private DownloadHandler downloadHandler = new DownloadHandler();
    private Messenger messenger = new Messenger(downloadHandler);

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Start download
     *
     * @param url The target file url.
     */
    private void startDownload(String url) {

        HttpUtils http = new HttpUtils();
        httpHandler = http.download(url, Environment.getExternalStorageDirectory() + "/OAT.zip",
                true, // If the server support range ,it will continue download,otherwise,re-download
                false, // If we can get the filename,auto rename the file.
                new RequestCallBack<File>() {

                    @Override
                    public void onStart() {
                        Toast.makeText(getApplicationContext(), "conn start", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onLoading(long total, long current, boolean isUploading) {
                        if (clientMessenger != null) {
                            Message msg = Message.obtain();
                            msg.what = Constants.SEND_PROGRESS;
                            Bundle data = new Bundle();
                            data.putLong("total", total);
                            data.putLong("current", current);
                            msg.setData(data);
                            try {
                                clientMessenger.send(msg);
                                Log.d(TAG, "-----send progress ------- " + current + " / " + total);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void onSuccess(ResponseInfo<File> responseInfo) {
                        Toast.makeText(getApplicationContext(), "downloaded:" + responseInfo.result.getPath(), Toast.LENGTH_SHORT).show();
                    }


                    @Override
                    public void onFailure(HttpException error, String msg) {
                        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                        Message failmsg = Message.obtain();
                        failmsg.what = Constants.CONNECTION_ERROR;
                        try {
                            clientMessenger.send(failmsg);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    /**
     * Pause Download
     */
    private void pauseDownload() {
        if (httpHandler != null) {
            httpHandler.cancel();
        }
    }


    private class DownloadHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            clientMessenger = msg.replyTo;
            switch (msg.what) {
                case Constants.START_DOWNLOAD:
                    startDownload(FILE_URL);
                    break;
                case Constants.PAUSE_DOWNLOAD:
                    pauseDownload();
                    break;
            }
        }
    }
}
