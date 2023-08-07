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
            //request permission again if it wasn't given
            requestPermissions(new String[]{"android.permission.ACCESS_FINE_LOCATION"}, 1);
        } else {
            v.setEnabled(false);
            Button button = (Button) v;
            button.setText("Getting GPS Location");

            //prevent phone from automatically locking
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            LocationAccess locAccess = new LocationAccess(this);
            locAccess.getCurrentLocation(new LocationAccess.LocationResultCallback() {
                @Override
                public void onLocationResult(Location location) {
                    double lat = location.getLatitude();
                    double lon = location.getLongitude();
                    double alt = location.getAltitude();

                    //get actual device location for eclipse timing TODO: use for actual app releases
                    //String[] eclipseData = LocToTime.calculatefor(lat, lon, alt);

                    //spoof location for eclipse testing; TODO: remove for actual app releases
                    //String[] eclipseData = LocToTime.calculatefor(37.60786, -91.02687, 0);

                    //get actual device location for sunset timing (test stuff) TODO: remove for actual app releases
                    String sunsetTime = Sunset.calcSun(lat, -lon); //make longitude negative as the sunset calculations use a positive westward latitude as opposed to the eclipse calculations using a positive eastward latitude

                    if(/*!eclipseData[0].equals("N/A")*/ true) {        //TODO: swap for actual app releases
                        //long[] times = convertTimes(eclipseData);     //TODO: use for actual app releases
                        long[] times = convertSunsetTime(sunsetTime);   //TODO: remove for actual app releases

                        //use the given times to create calendar objects to use in setting alarms
                        Calendar[] timeCals = new Calendar[2];
                        timeCals[0] = Calendar.getInstance();
                        timeCals[0].setTimeInMillis(times[0] * 1000);
                        timeCals[1] = Calendar.getInstance();
                        timeCals[1].setTimeInMillis(times[1] * 1000);

                        //for the final app, might want to add something that makes a countdown timer on screen tick down
                        //String details = "You are at lat: " + lat + ", lon: " + lon + "; The solar eclipse will start at the following time at your current location: " + timeCals[0].getTime(); //TODO: use for actual app releases
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
                        //Date date = new Date((times[0] - 15) * 1000);
                        //the next line is a testcase to make sure functionality works for eclipse timing
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

    //TimerTask subclass that opens the CameraActivity at the specified time
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

    //convert `hh:mm:ss` format string to unix time (this version is specifically for Apr. 8, 2024 eclipse, the first number in startUnix and endUnix will need to be modified to the unix time for the start of Oct. 14, 2023 for that test
    long[] convertTimes(String[] data){
        String[] start = data[0].split(":");
        String[] end = data[1].split(":");

        //we don't need to worry about standard timezones, since the actual eclipse is on 4/8, during daylight savings (same for 10/14 eclipse)
        //actually this code might not even do anything, but it works correctly. TODO: Might want to test printing out the timezone displayname, if the format doesn't match the cases here then the switch can likely be removed entirely
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

    long[] convertSunsetTime(String data){
        //0 -> hour; 1 -> minute; 2 -> second
        String[] start = data.split(":");

        //get current time in seconds, remove a day if it is past UTC midnight for the date that your timezone is currently in
        long currentDateUnix = (System.currentTimeMillis() / 1000);
        long currentTimeUnix = currentDateUnix % 86400;
        if(currentTimeUnix > 0 && currentTimeUnix < 5 * 60 * 60){
            Log.d("SunsetTiming", "Current time is past UTC midnight; Subtracting a day from time estimate");
            currentDateUnix -= 86400;
        }

        long currentDateTimezoneCorrectedUnix = (currentDateUnix - (currentDateUnix % (60 * 60 * 24))) - (-5 * 60 * 60);

        //convert the given time to seconds, add it to the start of the day as calculated by
        long startUnix = currentDateTimezoneCorrectedUnix + (Integer.parseInt(start[0]) * 3600L) + (Integer.parseInt(start[1]) * 60L) + Integer.parseInt(start[2]);

        return new long[]{startUnix, startUnix};
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