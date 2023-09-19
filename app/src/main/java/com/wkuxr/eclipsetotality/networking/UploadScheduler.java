package com.wkuxr.eclipsetotality.networking;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.Nullable;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class UploadScheduler extends Service {
    static UploadScheduler singleton;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        singleton = this;

        //schedule the ping chain
        Date pingTime = new Date(System.currentTimeMillis() + 1000);
        transferAttemptChainTimer = new Timer();
        transferAttemptChainTimer.schedule(new TransferAttemptChain(), pingTime);

        return super.onStartCommand(intent, flags, startId);
    }

    Timer transferAttemptChainTimer = null;
    Handler sequenceHandler = new Handler();
    Runnable tryServerConnectionRunnable = new Runnable(){
        @Override
        public void run(){
            if(!pingServer())
                sequenceHandler.postDelayed(this, 15 * 60 * 1000);//ping server to see if it can upload once every 15 minutes
        }
    };

    static class TransferAttemptChain extends TimerTask {
        public void run(){
            singleton.sequenceHandler.postDelayed(singleton.tryServerConnectionRunnable, 15 * 60 * 1000);//ping server to see if it can upload once every 15 minutes
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    boolean pingServer(){
        //server ping code
        boolean successful = false;
        //connect to server and see if it's available
        //if available
            //do transfer
            //successful = true;
        return successful;
    }
}
