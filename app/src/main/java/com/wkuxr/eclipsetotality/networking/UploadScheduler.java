package com.wkuxr.eclipsetotality.networking;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.wkuxr.eclipsetotality.App;
import com.wkuxr.eclipsetotality.R;
import com.wkuxr.eclipsetotality.activities.FinishedInfoActivity;

import java.io.IOException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class UploadScheduler extends Service {
    static UploadScheduler singleton;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        singleton = this;

        Log.d("UploadScheduler", "Starting infrequent pinging.");

        final String CHANNELID = "Foreground Service ID";
        NotificationChannel channel = new NotificationChannel(
                CHANNELID,
                CHANNELID,
                NotificationManager.IMPORTANCE_LOW
        );

        getSystemService(NotificationManager.class).createNotificationChannel(channel);
        Notification.Builder notification = new Notification.Builder(this, CHANNELID)
                .setContentText("Service is running")
                .setContentTitle("SunSketcher Upload Scheduler")
                .setSmallIcon(R.drawable.ic_launcher_background);

        startForeground(1001, notification.build());

        SharedPreferences prefs = getSharedPreferences("eclipseDetails", Context.MODE_PRIVATE);
        long clientID = prefs.getLong("clientID", 9999999);

        Thread thread = new Thread(() -> {
            boolean successful = false;
            try {
                //long firstScheduleTime = (1712562431000L + (clientID * (15 * 60 * 1000))) - System.currentTimeMillis();
                long firstScheduleTime = (clientID * (15 * 60 * 1000));// + (60 * 60 * 1000);
                Thread.sleep(firstScheduleTime);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            while(!successful){
                try {
                    successful = pingServer();
                } catch (Exception e) {
                    Log.w("UploadScheduler","Connection failed. Trying again in 15 minutes.");
                }
                try {
                    Thread.sleep(15 * 60 * 1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            stopSelf();
        });
        thread.start();

        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    boolean pingServer() throws Exception {
        //server ping code
        Log.d("UploadScheduler", "Pinging server.");
        boolean successful = false;
        //connect to server and see if it's available
        //if available
            //do transfer
            //successful = true;
        successful = ClientRunOnTransfer.clientTransferSequence(App.getContext());
        return successful;
    }
}
