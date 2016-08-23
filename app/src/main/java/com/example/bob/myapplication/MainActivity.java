package com.example.bob.myapplication;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    ProgressBar progressBar;
    private DownloadServiceConnection serviceConnection;
    private ProgressHandler progressHandler = new ProgressHandler(this);
    private Messenger progressMessenger = new Messenger(progressHandler);
    private Messenger serviceMessenger;
    private Button downloadButton;
    private Button pauseButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        downloadButton = ((Button) findViewById(R.id.downloadButton));
        assert downloadButton != null;
        downloadButton.setOnClickListener(this);
        pauseButton = ((Button) findViewById(R.id.pause_button));
        assert pauseButton != null;
        pauseButton.setOnClickListener(this);

        Intent service = new Intent(this, DownloadService.class);
        startService(service);
        serviceConnection = new DownloadServiceConnection();
        bindService(service, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.downloadButton:
                sendMessage(Constants.START_DOWNLOAD);
                downloadButton.setClickable(false);
                break;
            case R.id.pause_button:
                sendMessage(Constants.PAUSE_DOWNLOAD);
                downloadButton.setClickable(true);
                break;
        }
    }

    private void sendMessage(int command) {
        if (serviceMessenger != null) {
            Message message = Message.obtain();
            message.what = command;
            message.replyTo = progressMessenger;
            try {
                serviceMessenger.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }


    private static class ProgressHandler extends Handler {

        WeakReference<MainActivity> weakReference;
        private final MainActivity mainActivity;

        public ProgressHandler(MainActivity activity) {
            weakReference = new WeakReference<MainActivity>(activity);
            mainActivity = weakReference.get();
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case Constants.SEND_PROGRESS:
                    Bundle data = msg.getData();
                    if (data != null) {
                        long total = data.getLong("total");
                        long current = data.getLong("current");
                        mainActivity.progressBar.setMax((int) total);
                        mainActivity.progressBar.setProgress((int) current);
                    }
                    break;
                case Constants.CONNECTION_ERROR:
                    mainActivity.downloadButton.setClickable(true);
                    break;
            }

        }
    }

    private class DownloadServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            serviceMessenger = new Messenger(service);
            Message message = Message.obtain();
            message.replyTo = progressMessenger;
            try {
                serviceMessenger.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceConnection != null) {
            unbindService(serviceConnection);
        }

        if (progressHandler != null) {
            progressHandler.removeCallbacksAndMessages(null);
        }
    }
}
