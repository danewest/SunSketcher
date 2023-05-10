package com.wkuxr.eclipsetotality;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.wkuxr.eclipsetotality.Location.LocationAccess;

public class MainActivity extends AppCompatActivity {

    public static MainActivity singleton;   //having a singleton instance of the MainActivity makes so many things significantly easier because it gives us a definite, central reference point for non-static and activity-related functions
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        singleton = this;
        rPerm(new String[]{"android.permission.ACCESS_FINE_LOCATION"});
    }

    @SuppressLint("SetTextI18n")
    public void getLocation(View v) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.ACCESS_FINE_LOCATION"}, 1);
        } else {
            v.setEnabled(false);
            Button button = (Button) v;
            button.setText("Getting GPS Location");

            Location location = LocationAccess.getLocation();

            double lon = location.getLongitude();
            double lat = location.getLatitude();
            String loc = "lat: " + lat + "; lon: " + lon;

            button.setText(loc);
        }
    }

    public void rPerm(String[] permissions){
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