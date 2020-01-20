package it.polito.helpenvironmentnow;

import android.Manifest;
import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import it.polito.helpenvironmentnow.Helper.ServiceNotification;
import it.polito.helpenvironmentnow.Storage.MyDb;
import it.polito.helpenvironmentnow.Storage.Position;

// This SERVICE is enabled when the user activates the MOVEMENT MODE and it is used to get
// continuous location updates. The positions are saved into a local database; in this way
// the MovementService can get the saved locations when it needs and can use them to match with the
// measures taken from the Rasperry Pi using the timestamp as matching criteria.
// The SERVICE runs indefinitely until the user stops it by disabling the MOVEMENT MODE.
public class LocationService extends Service {

    private final int SERVICE_ID = 3;
    private String TAG = "LocService"; // String used for debug in Log.d method

    private Looper serviceLooper;
    private ServiceHandler serviceHandler;

    private FusedLocationProviderClient locationClient;
    private LocationCallback locationCallback;

    private MyDb myDb;

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {

        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            // Set the location request in order to receive continuous location updates
            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setInterval(1000);
            locationRequest.setFastestInterval(1000);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setMaxWaitTime(5000);
            locationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (getApplicationContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }
            locationClient.requestLocationUpdates(locationRequest, locationCallback, serviceLooper);
            myDb = new MyDb(getApplicationContext());
        }
    }

    @Override
    public void onCreate() {
        // A foreground service in order to work in Android has to show a notification, as quoted by
        // the official guide: "Foreground services must display a Notification."
        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) // check if Android version is 8 or higher
            notification = ServiceNotification.getMyOwnNotification(this, "loc","HelpEnvironmentNow Service",
                    "MOVEMENT mode is ON", "Continuous location tracking is enabled"); // foreground service notification for Android 8+
        else
            notification =  new Notification(); // foreground service notification for Android 7.x or below
        startForeground(SERVICE_ID, notification);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }

                List<Position> positions = new ArrayList<>();
                for (Location location : locationResult.getLocations()) {
                    int timestamp = ((Long)TimeUnit.MILLISECONDS.toSeconds(location.getTime())).intValue();
                    Position currentPosition = new Position();
                    currentPosition.timestamp = timestamp;
                    currentPosition.latitude = location.getLatitude();
                    currentPosition.longitude = location.getLongitude();
                    currentPosition.altitude = location.getAltitude();
                    positions.add(currentPosition);
                }
                myDb.insertPositions(positions);
            }
        };

        // Start up the thread running the service. Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.
        HandlerThread thread = new HandlerThread("LocationServiceHT",
                Process.THREAD_PRIORITY_BACKGROUND); // HandlerThread: Helper class that builds a
        // secondary thread that incorporates a Looper and a MessageQueue
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);
    }

    // Called by the system every time a client explicitly starts the service by calling
    // startService(Intent), providing the arguments it supplied and a unique integer token
    // representing the start request. Do not call this method directly.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = serviceHandler.obtainMessage();
        msg.arg1 = startId;
        serviceHandler.sendMessage(msg);

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        locationClient.removeLocationUpdates(locationCallback);
        myDb.closeDb();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
