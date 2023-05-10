package com.wkuxr.eclipsetotality.Location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import android.location.Location;

import android.app.Activity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.OnTokenCanceledListener;
import com.google.android.gms.tasks.Task;
import com.wkuxr.eclipsetotality.MainActivity;
import com.wkuxr.eclipsetotality.R;

public class LocationAccess implements com.google.android.gms.location.LocationListener {
    public static FusedLocationProviderClient fusedLocationProviderClient;
    @SuppressLint("SetTextI18n")
    public static Location getLocation() {

        //The following CancellationToken is blank as it's necessary to request the current location; it can be left blank because we should never need to cancel it.
        CancellationToken token = new CancellationToken() {
            @NonNull
            @Override
            public CancellationToken onCanceledRequested(@NonNull OnTokenCanceledListener onTokenCanceledListener) {
                return null;
            }

            @Override
            public boolean isCancellationRequested() {
                return false;
            }
        };

        //Any function that requires user-enabled permissions on android freaks out if you don't have a failsafe to check for and request permissions before it, this is just to circumvent that compile error
        if (ActivityCompat.checkSelfPermission(MainActivity.singleton, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.singleton, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            MainActivity.singleton.rPerm(new String[]{"android.permission.ACCESS_FINE_LOCATION"});
        }

        startLocationUpdates();

        final Location[] loc = new Location[1];     //tasks don't allow external non-final values to be accessed inside of them, so we create a final array to cheat the system and modify an external value
        Task<Location> task = fusedLocationProviderClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token);   //gets the current location in a different thread
        task.addOnSuccessListener(location -> {     //because this happens in a different thread, we can't just directly pull the value back immediately, so we use this function to wait until the thread completes and then bring the necessary location value back
            loc[0] = location;
        });

        return loc[0];
    }

    //In effect, this is the same as connecting to the GoogleApiClient in the old program, but all of that is deprecated and this is much simpler to code
    protected static void startLocationUpdates() {
        LocationRequest mLocationRequest = LocationRequest.create().setPriority(Priority.PRIORITY_HIGH_ACCURACY).setInterval(500).setExpirationDuration(10000);      //creating a location process (LocationRequest) that has high accuracy and updates every 500ms

        //confirming that the app has permissions to access location
        if (ActivityCompat.checkSelfPermission(MainActivity.singleton, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.singleton, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            MainActivity.singleton.rPerm(new String[]{"android.permission.ACCESS_FINE_LOCATION"});      //request location permissions if they haven't been granted
            return;
        }

        //turns on the GPS location system; it's normally disabled to save power so either it needs to be turned on here or some other program that also accesses GPS needs to be open simultaneously
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MainActivity.singleton);
        fusedLocationProviderClient.requestLocationUpdates(mLocationRequest, new LocationCallback(){}, null);

    }

    @Override
    public void onLocationChanged(@NonNull android.location.Location location) {

    }
}
