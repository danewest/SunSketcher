package com.wkuxr.eclipsetotality.activities;

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

import com.wkuxr.eclipsetotality.location.LocationAccess;
import com.wkuxr.eclipsetotality.location.LocToTime;
import com.wkuxr.eclipsetotality.location.Sunset;
import com.wkuxr.eclipsetotality.databinding.ActivityMainBinding;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    public static MainActivity singleton;   //having a singleton instance of the MainActivity makes so many things significantly easier because it gives us a definite, central reference point for non-static and activity-related functions
    static ActivityMainBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        singleton = this;

        reqPerm(new String[]{"android.permission.CAMERA"});
        reqPerm(new String[]{"android.permission.ACCESS_FINE_LOCATION"});



        SharedPreferences prefs = getSharedPreferences("eclipseDetails", Context.MODE_PRIVATE);
        int hasConfirmDeny = prefs.getInt("upload", -1);
        switch(hasConfirmDeny){
            case -2:
                Intent intent = new Intent(this, SendConfirmationActivity.class);
                this.startActivity(intent);
                break;
            case 0:
            case 1:
                intent = new Intent(this, FinishedInfoActivity.class);
                this.startActivity(intent);
                break;
            default:
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = getSharedPreferences("eclipseDetails", Context.MODE_PRIVATE);
        int hasConfirmDeny = prefs.getInt("upload", -1);
        switch(hasConfirmDeny){
            case -2:
                Intent intent = new Intent(this, SendConfirmationActivity.class);
                this.startActivity(intent);
                break;
            case 0:
            case 1:
                intent = new Intent(this, FinishedInfoActivity.class);
                this.startActivity(intent);
                break;
            default:
        }
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

            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            LocationAccess locAccess = new LocationAccess(this);
            locAccess.getCurrentLocation(new LocationAccess.LocationResultCallback() {
                @Override
                public void onLocationResult(Location location) {
                    double lat = location.getLatitude();
                    double lon = location.getLongitude();
                    double alt = location.getAltitude();

                    //get actual device location TODO: use for actual app releases
                    //String[] eclipseData = LocToTime.calculatefor(lat, lon, alt);

                    //spoof location for testing; TODO: remove for actual app releases
                    //String[] eclipseData = LocToTime.calculatefor(37.60786, -91.02687, 0);

                    //get actual device location for sunset timing (test stuff) TODO: remove for actual app releases
                    long sunsetTimeUnix = Sunset.calcSun(lat, -lon); //make longitude negative as the sunset calculations use a positive westward latitude as opposed to the eclipse calculations using a positive eastward latitude
                    long[] times = new long[]{sunsetTimeUnix, sunsetTimeUnix};

                    if(/*!eclipseData[0].equals("N/A")*/ true) {
                        //long[] times = convertTimes(eclipseData);

                        //--------to make it visible that something is happening--------
                        //for the final app, might want to replace this code with something that makes a countdown timer on screen tick down
                        Calendar[] timeCals = new Calendar[2];
                        timeCals[0] = Calendar.getInstance();
                        timeCals[0].setTimeInMillis(times[0] * 1000);
                        timeCals[1] = Calendar.getInstance();
                        timeCals[1].setTimeInMillis(times[1] * 1000);

                        //String details = "lat: " + lat + "; lon: " + lon + "; Eclipse Time: " + timeCals[0].getTime(); //TODO: use for actual app releases
                        String details = "lat: " + lat + "; lon: " + lon + "; Sunset Time: " + timeCals[0].getTime(); //TODO: remove for actual app releases
                        Log.d("Timing", details);

                        button.setText(details);
                        //--------made it visible that something is happening--------

                        //store the unix time for the start and end of totality in SharedPreferences
                        SharedPreferences.Editor prefs = getSharedPreferences("eclipseDetails", Context.MODE_PRIVATE).edit();
                        prefs.putLong("startTime",times[0] * 1000);
                        prefs.putLong("endTime",times[1] * 1000);
                        prefs.putFloat("lat", (float)lat);
                        prefs.putFloat("lon", (float)lon);
                        prefs.putFloat("alt", (float)alt);
                        prefs.apply();

                        //go to camera 15 seconds prior, start taking images 7 seconds prior to 3 seconds after, and then at end of eclipse 3 seconds before and 7 after TODO: also for the sunset timing
                        Date date = new Date((times[0] - 15) * 1000);
                        //the next line is a testcase to make sure functionality works for eclipse timing
                        //Date date = new Date((System.currentTimeMillis()) + 5000);
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