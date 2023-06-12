package com.wkuxr.eclipsetotality;

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
import android.widget.Button;

import com.wkuxr.eclipsetotality.Location.LocationAccess;
import com.wkuxr.eclipsetotality.Location.LocToTime;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;

import com.wkuxr.eclipsetotality.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    public static MainActivity singleton;   //having a singleton instance of the MainActivity makes so many things significantly easier because it gives us a definite, central reference point for non-static and activity-related functions
    static ActivityMainBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        singleton = this;
        reqPerm(new String[]{"android.permission.ACCESS_FINE_LOCATION"});
    }


    Timer timer = null;
    @SuppressLint("SetTextI18n")
    public void getLocation(View v) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.ACCESS_FINE_LOCATION"}, 1);
        } else {
            v.setEnabled(false);
            Button button = (Button) v;
            button.setText("Getting GPS Location");

            LocationAccess locAccess = new LocationAccess(this);
            locAccess.getCurrentLocation(new LocationAccess.LocationResultCallback() {
                @Override
                public void onLocationResult(Location location) {
                    double lon = location.getLongitude();
                    double lat = location.getLatitude();
                    double alt = location.getAltitude();

                    //String[] eclipseData = LocToTime.calculatefor(lon, lat, alt);
                    String[] eclipseData = LocToTime.calculatefor(37.60786, -91.02687, 0);
                    if(!eclipseData[0].equals("N/A")) {
                        long[] times = convertTimes(eclipseData);

                        //--------to make it visible that something is happening--------
                        //for the final app, might want to replace this code with something that makes a countdown timer on screen tick down
                        Calendar[] timeCals = new Calendar[2];
                        timeCals[0] = Calendar.getInstance();
                        timeCals[0].setTimeInMillis(times[0] * 1000);
                        timeCals[1] = Calendar.getInstance();
                        timeCals[1].setTimeInMillis(times[1] * 1000);

                        String details = "lat: " + lat + "; lon: " + lon + "; Eclipse Time: " + timeCals[0].getTime();

                        button.setText(details);
                        //--------made it visible that something is happening--------

                        //store the unix time for the start and end of totality in SharedPreferences
                        SharedPreferences.Editor prefs = getSharedPreferences("eclipseDetails", Context.MODE_PRIVATE).edit();
                        prefs.putLong("startTime",times[0] * 1000);
                        prefs.putLong("endTime",times[1] * 1000);
                        prefs.apply();

                        //go to camera 15 seconds prior, start taking images 7 seconds prior to 3 seconds after, and then at end of eclipse 3 seconds before and 7 after
                        //Date date = new Date((times[0] - 15) * 1000);
                        Date date = new Date((System.currentTimeMillis()) + 5000);
                        Log.d("SCHEDULE_CAMERA", date.toString());

                        timer = new Timer();
                        TimeTask cameraActivitySchedulerTask = new TimeTask();
                        timer.schedule(cameraActivitySchedulerTask, date);
                    } else {
                        button.setText("Not in eclipse path.");
                    }
                }

                @Override
                public void onLocationFailed() {
                    button.setText("Unable to get location");
                }
            });
        }
    }

    static class TimeTask extends TimerTask{
        Context context;

        public TimeTask(){
            context = MainActivity.singleton;
        }

        public void run(){
            Intent intent = new Intent(context, CameraActivity.class);
            context.startActivity(intent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(timer != null){
            timer.cancel();
        }

    }

    long[] convertTimes(String[] data){
        String[] start = data[0].split(":");
        String[] end = data[1].split(":");

        //we don't need to worry about standard timezones, since the actual eclipse is on 4/8, during daylight savings
        int timeDiff = 0;
        switch(TimeZone.getDefault().getDisplayName(true, TimeZone.SHORT)){
            case "HST-10:00":
                timeDiff = -10;
                break;
            case "AKDT-8:00":
                timeDiff = -8;
                break;
            case "PDT-7:00":
                timeDiff = -7;
                break;
            case "MDT-6:00":
                timeDiff = -6;
                break;
            case "CDT-5:00":
                timeDiff = -5;
                break;
            case "EDT-4:00":
                timeDiff = -4;
                break;
            default:
                break;
        }

        long startUnix = 1712530800 + ((Integer.parseInt(start[0]) + timeDiff) * 3600L) + (Integer.parseInt(start[1]) * 60L) + Integer.parseInt(start[2]);
        long endUnix = 1712530800 + ((Integer.parseInt(end[0]) + timeDiff) * 3600L) + (Integer.parseInt(end[1]) * 60L) + Integer.parseInt(end[2]);

        return new long[]{startUnix, endUnix};
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