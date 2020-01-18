package it.polito.helpenvironmentnow.Helper;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.util.Log;

import com.fonfon.geohash.GeoHash;
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
                    myLocationListener.locationCompleted(location); // fires the callback in ClassicService
                } else {
                    Location defaultLocation = new Location(LocationManager.GPS_PROVIDER);
                    Log.d("LocationInfo", "lat" + defaultLocation.getLatitude()+"long"+defaultLocation.getLongitude()+"alt"+defaultLocation.getAltitude());
                    defaultLocation.setLatitude(defaultLatitude);
                    defaultLocation.setLongitude(defaultLongitude);
                    defaultLocation.setAltitude(defaultAltitude);
                    myLocationListener.locationCompleted(defaultLocation); // fires the callback in ClassicService
                }
            }
        });
    }

    public static String encodeLocation(Location location) {
        final int numberOfChars = 12; // the same size as the corresponding remote database field - varchar(12)
        GeoHash hash = GeoHash.fromLocation(location, numberOfChars);
        return hash.toString();
    }
}
