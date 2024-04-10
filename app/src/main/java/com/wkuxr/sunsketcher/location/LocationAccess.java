package com.wkuxr.sunsketcher.location;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;


//simple adapter for the FusedLocationProviderClient
public class LocationAccess {
    private final Context context;
    private final FusedLocationProviderClient fusedLocationProviderClient;

    //constructor
    public LocationAccess(Context context) {
        this.context = context;
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
    }

    //callback interface for whenever a location result is obtained or failed
    public interface LocationResultCallback {
        void onLocationResult(Location location);
        void onLocationFailed();
    }

    //creates a location request for the FusedLocationProviderClient
    public void getCurrentLocation(final LocationResultCallback callback) {
        //check if the necessary permissions are given
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Handle the case where location permissions are not granted
            if (callback != null) {
                callback.onLocationFailed();
            }
            return;
        }

        //create a location request
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY) // Update interval in milliseconds
                .setInterval(10000)
                .setFastestInterval(5000);
        Log.d("LocationAccess", "Created location request.");

        //pass the created locationRequest to the FusedLocationProviderClient along with a callback for when a location is obtained
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            //called once a location is received
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Log.d("LocationAccess", "Location received.");
                //get the location from the result
                Location lastLocation = locationResult.getLastLocation();
                if (callback != null) {
                    //pass the location result to the callback function
                    callback.onLocationResult(lastLocation);
                }
                //prevent location from being accessed again (this works more consistently than trying to set the locationRequest to only access location once, for some reason
                fusedLocationProviderClient.removeLocationUpdates(this);
            }
        }, Looper.getMainLooper());
    }
}
