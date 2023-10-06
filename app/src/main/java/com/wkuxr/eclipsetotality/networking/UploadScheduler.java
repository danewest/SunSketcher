package com.wkuxr.eclipsetotality.networking;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
    public int onStartCommand(Intent intent, int flags, int startId) {
        singleton = this;

        Log.d("UploadScheduler", "Starting infrequent pinging.");

        //channel for foreground service notification
        final String CHANNELID = "Upload Scheduler Service";
        NotificationChannel channel = new NotificationChannel(
                CHANNELID,
                CHANNELID,
                NotificationManager.IMPORTANCE_LOW
        );

        //create the foreground service persistent notification
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
        Notification.Builder notification = new Notification.Builder(this, CHANNELID)
                .setContentText("Waiting to upload images, please do not force close.")
                .setContentTitle("SunSketcher Upload Scheduler")
                .setSmallIcon(R.mipmap.ic_launcher_foreground);

        //start the foreground service
        startForeground(1001, notification.build());

        //grab client ID, later used to determine first upload attempt time
        SharedPreferences prefs = getSharedPreferences("eclipseDetails", Context.MODE_PRIVATE);
        long clientID = prefs.getLong("clientID", -1);

        if(clientID != -1){
            Thread thread = new Thread(() -> {
                boolean successful = false;
                try {
                    //sleep until time for first upload attempt
                    //long firstScheduleTime = (1712562431000L + (clientID * (15 * 60 * 1000))) - System.currentTimeMillis();
                    long firstScheduleTime = (clientID * (15 * 60 * 1000)) + (60 * 60 * 1000);
                    Thread.sleep(firstScheduleTime);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                //keep attempting every 15 minutes indefinitely until upload finishes successfully
                while (!successful) {
                    try {
                        successful = pingServer();
                    } catch (IOException e) {
                        Log.w("UploadScheduler", "Connection failed. Trying again in 15 minutes.");
                    }
                    //if unsuccessful, sleep again for 15 minutes
                    if (!successful) {
                        try {
                            Thread.sleep(15 * 60 * 1000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }

                Log.d("UploadScheduler", "Upload successful. Stopping UploadScheduler foreground service.");

                //create a push notification that says that the user's images have been uploaded, and direct it to FinishedInfoActivity
                Intent finishedInfoIntent = new Intent(App.getContext(), FinishedInfoActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(App.getContext(), 0, finishedInfoIntent, PendingIntent.FLAG_IMMUTABLE);
                NotificationChannel defChannel = new NotificationChannel(NotificationChannel.DEFAULT_CHANNEL_ID, NotificationChannel.DEFAULT_CHANNEL_ID, NotificationManager.IMPORTANCE_DEFAULT);
                getSystemService(NotificationManager.class).createNotificationChannel(defChannel);
                final Notification.Builder doneNotification = new Notification.Builder(this, defChannel.getId())
                        .setContentText("Your images have been uploaded! Feel free to delete the SunSketcher app.")
                        .setContentTitle("SunSketcher")
                        .setSmallIcon(R.mipmap.ic_launcher_foreground)
                        .setContentIntent(pendingIntent);
                doneNotification.build();

                SharedPreferences.Editor prefEdit = prefs.edit();
                prefEdit.putBoolean("uploadSuccessful", true);

                //stop the foreground service
                stopSelf();
            });
            thread.start();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    boolean pingServer() throws IOException {
        Log.d("UploadScheduler", "Pinging server.");
        //attempt connection to server
        return ClientRunOnTransfer.clientTransferSequence();
    }
}
