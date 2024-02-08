package com.wkuxr.sunsketcher.networking;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;

import com.wkuxr.sunsketcher.App;
import com.wkuxr.sunsketcher.R;
import com.wkuxr.sunsketcher.activities.FinishedCompleteActivity;
import com.wkuxr.sunsketcher.activities.FinishedInfoActivity;

import java.io.IOException;

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
        Intent finishedInfoIntent = new Intent(App.getContext(), FinishedInfoActivity.class);
        PendingIntent pendingInProgressIntent = PendingIntent.getActivity(App.getContext(), 0, finishedInfoIntent, PendingIntent.FLAG_IMMUTABLE);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
        Notification.Builder notification = new Notification.Builder(this, CHANNELID)
                .setContentText("Waiting to upload images, please do not force close.")
                .setContentTitle("SunSketcher Upload Scheduler")
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentIntent(pendingInProgressIntent);

        //start the foreground service
        startForeground(1001, notification.build());

        //grab client ID, later used to determine first upload attempt time
        SharedPreferences prefs = getSharedPreferences("eclipseDetails", Context.MODE_PRIVATE);
        final long[] clientID = {prefs.getLong("clientID", -1)};

        Thread thread = new Thread(() -> {
            if (!prefs.getBoolean("uploadSuccessful", false)) {
                Log.d("UploadScheduler", "This device has not yet successfully uploaded. Scheduling...");
                boolean successful = false;

                //keep attempting every 15 minutes indefinitely until upload finishes successfully
                while (!successful) {
                    try {
                        if (clientID[0] != -1) {
                            successful = pingServer();
                        } else {
                            IDRequest.clientTransferSequence();
                            clientID[0] = prefs.getLong("clientID", -1);
                        }
                    } catch (Exception e) {
                        Log.w("UploadScheduler", "Connection failed. Trying again in 15 minutes.");
                    }
                    //if unsuccessful, sleep again for 15 minutes
                    if (!successful) {
                        try {
                            Thread.sleep(1 * 60 * 1000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }

            Log.d("UploadScheduler", "Upload successful. Stopping UploadScheduler foreground service.");

            SharedPreferences.Editor prefEdit = prefs.edit();
            prefEdit.putBoolean("uploadSuccessful", true);
            prefEdit.apply();

            //create a push notification that says that the user's images have been uploaded, and direct it to FinishedInfoActivity
            createNotificationChannel();
            Intent finishedCompleteIntent = new Intent(App.getContext(), FinishedCompleteActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(App.getContext(), 0, finishedCompleteIntent, PendingIntent.FLAG_IMMUTABLE);
            final Notification.Builder doneNotification = new Notification.Builder(this, "UploadInfo")
                    .setContentText("Your images have been uploaded! Feel free to delete the SunSketcher app.")
                    .setContentTitle("SunSketcher")
                    .setSmallIcon(R.drawable.ic_stat_name)
                    .setContentIntent(pendingIntent);
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                //theoretically it's impossible for it to not have this permission at this point, so just ignore
            }
            notificationManager.notify(1002, doneNotification.build());

            //stop the foreground service
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
        Log.d("UploadScheduler", "Pinging server.");
        //attempt connection to server
        return ClientRunOnTransfer.clientTransferSequence();
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is not in the Support Library.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Upload Info";
            String description = "This channel is used to notify the user once their upload is complete.";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("UploadInfo", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this.
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

}
