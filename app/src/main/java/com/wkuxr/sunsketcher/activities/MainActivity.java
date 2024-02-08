package com.wkuxr.sunsketcher.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.wkuxr.sunsketcher.App;
import com.wkuxr.sunsketcher.location.LocationAccess;
import com.wkuxr.sunsketcher.location.Sunset;
import com.wkuxr.sunsketcher.databinding.ActivityMainBinding;
import com.wkuxr.sunsketcher.networking.IDRequest;

import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    public static MainActivity singleton;   //having a singleton instance of the MainActivity makes so many things significantly easier because it gives us a definite, central reference point for non-static and activity-related functions
    ActivityMainBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        singleton = this;
        App.setContext(this);

        reqPerm(new String[]{"android.permission.CAMERA","android.permission.ACCESS_FINE_LOCATION","android.permission.WRITE_EXTERNAL_STORAGE","android.permission.INTERNET","android.permission.POST_NOTIFICATIONS"});

        SharedPreferences prefs = getSharedPreferences("eclipseDetails", Context.MODE_PRIVATE);
        int hasConfirmDeny = prefs.getInt("upload", -1); //if -1, hasn't taken images yet
        Intent intent = null;
        switch(hasConfirmDeny){
            case -2: //not yet confirmed or denied
                intent = new Intent(this, SendConfirmationActivity.class);
                break;
            case 0: //denied upload
                intent = new Intent(this, FinishedInfoDenyActivity.class);
                break;
            case 1: //allowed upload
                if(prefs.getBoolean("uploadSuccessful", false)){ //allowed upload and upload already finished
                    intent = new Intent(this, FinishedCompleteActivity.class);
                } else {
                    intent = new Intent(this, FinishedInfoActivity.class);
                }
                break;
            default:
        }
        if(intent != null) {
            this.startActivity(intent);
        }

        long clientID = prefs.getLong("clientID", -1);
        //if the app has not yet gotten a clientID from the server, get one
        if(clientID == -1){
            //connect to server to get ID and upload time
            Thread idTimeThread = new Thread(() -> {
                try {
                    IDRequest.clientTransferSequence();
                    Log.d("ClientID", "ClientID: " + prefs.getLong("clientID", -1));
                } catch (Exception e) {
                    Log.e("ClientID", "Could not connect to server to obtain client ID.");
                    e.printStackTrace();
                }
            });
            idTimeThread.start();
        }
        //binding.clientIDText.setText("ClientID: " + clientID);
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = getSharedPreferences("eclipseDetails", Context.MODE_PRIVATE);
        int hasConfirmDeny = prefs.getInt("upload", -1);
        Intent intent = null;
        switch(hasConfirmDeny){
            case -2: //not yet confirmed or denied
                intent = new Intent(this, SendConfirmationActivity.class);
                break;
            case 0: //denied upload
                intent = new Intent(this, FinishedInfoDenyActivity.class);
                break;
            case 1: //allowed upload
                if(prefs.getBoolean("uploadSuccessful", false)){ //allowed upload and upload already finished
                    intent = new Intent(this, FinishedCompleteActivity.class);
                } else {
                    intent = new Intent(this, FinishedInfoActivity.class);
                }
                break;
            default:
        }
        if(intent != null) {
            this.startActivity(intent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(timer != null){
            timer.cancel();
        }
    }

    Timer timer = null;

    public void start(View v){
        SharedPreferences prefs = getSharedPreferences("eclipseDetails", Context.MODE_PRIVATE);

        if (prefs.getBoolean("completedTutorial", false)) {
            //tutorial has been completed, go to location prompt
            Intent intent = new Intent(this, LocationPromptActivity.class);
            startActivity(intent);
        } else {
            //tutorial has not been completed, suggest viewing tutorial
            Intent intent = new Intent(this, TutorialPromptActivity.class);
            startActivity(intent);
        }
    }

    public void tutorial(View v){
        SharedPreferences prefs = getSharedPreferences("eclipseDetails", Context.MODE_PRIVATE);
        SharedPreferences.Editor prefEdit = prefs.edit();
        prefEdit.putInt("next", 0);
        prefEdit.apply();

        Intent intent = new Intent(this, TutorialActivity.class);
        startActivity(intent);
    }

    public void learnMore(View v){
        Intent intent = new Intent(this, LearnMoreActivity.class);
        startActivity(intent);
    }

    public void tac(View v){
        Intent intent = new Intent(this, TermsAndConditionsActivity.class);
        startActivity(intent);
    }

    public void reqPerm(String[] permissions){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(permissions, 1);
        }
    }

    //the rest of these are just overrides necessary for superclass implementation
    @Override
    public void onStart(){
        super.onStart();
    }

    @Override
    public void onStop(){
        super.onStop();
    }
}