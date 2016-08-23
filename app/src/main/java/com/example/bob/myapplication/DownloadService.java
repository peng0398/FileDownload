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
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
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
    private String FILE_URL = "http://10.0.2.2:8080/ota_update.zip";
    private HttpHandler<File> httpHandler;
    private DownloadHandler downloadHandler = new DownloadHandler();
    private Messenger messenger = new Messenger(downloadHandler);
    NotificationManagerCompat mNotificationManager;
    NotificationCompat.Builder mBuilder;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mNotificationManager = NotificationManagerCompat.from(getApplicationContext());
        mBuilder = new NotificationCompat.Builder(getApplicationContext());
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
                        mBuilder.setSmallIcon(R.mipmap.ic_launcher)
                                .setContentTitle("start download ota.zip")
                                .setProgress(100, 0, false);
                        updateNotification();
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

                        // update the notification
                        mBuilder.setSmallIcon(R.mipmap.ic_launcher)
                                .setContentTitle("Downloading ..........")
                                .setProgress((int) total, (int) current, false);
                        updateNotification();
                    }

                    @Override
                    public void onSuccess(ResponseInfo<File> responseInfo) {
                        // update the notification
                        mBuilder.setSmallIcon(R.mipmap.ic_launcher)
                                .setContentTitle("Success ..........");
                        updateNotification();
                        Toast.makeText(getApplicationContext(), "downloaded:"
                                + responseInfo.result.getPath(), Toast.LENGTH_SHORT).show();
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
                        // update the notification
                        mBuilder.setSmallIcon(R.mipmap.ic_launcher)
                                .setContentTitle("Error ..........");
                        updateNotification();
                    }
                });
    }

    private void updateNotification() {
        mNotificationManager.notify(R.id.progressBar, mBuilder.build());
    }

    /**
     * Pause Download
     */
    private void pauseDownload() {
        if (httpHandler != null) {
            httpHandler.cancel();
            mBuilder.setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Pause ..........");
            updateNotification();
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
