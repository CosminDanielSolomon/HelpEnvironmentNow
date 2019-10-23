package it.polito.helpenvironmentnow.Helper;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

public class LocationInfo {

    private static final double defaultAltitude = 245; // medium altitude of Torino city
    private static final double defaultLatitude = 45.062435; // latitude near Politecnico di Torino
    private static final double defaultLongitude = 7.662321; // longitude near Politecnico di Torino

    public static void getCurrentLocation(Context context, final MyLocationListener myLocationListener) {
        FusedLocationProviderClient locationClient = LocationServices.getFusedLocationProviderClient(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }
        locationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                // Got last known location. In some rare situations this can be null.
                if (location != null) {
                    if(!location.hasAltitude())
                        location.setAltitude(defaultAltitude);
                } else {
                    Location defaultLocation = new Location(LocationManager.GPS_PROVIDER);
                    Log.d("LocationInfo", "lat" + defaultLocation.getLatitude()+"long"+defaultLocation.getLongitude()+"alt"+defaultLocation.getAltitude());
                    location.setLatitude(defaultLatitude);
                    location.setLongitude(defaultLongitude);
                    location.setAltitude(defaultAltitude);
                }
                myLocationListener.locationCompleted(location); // fires the callback in MainService
            }
        });
    }
}
