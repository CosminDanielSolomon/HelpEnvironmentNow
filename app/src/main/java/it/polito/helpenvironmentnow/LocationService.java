package it.polito.helpenvironmentnow;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.concurrent.TimeUnit;

public class LocationService extends Service {
    private String TAG = "LocService";

    private Looper serviceLooper;
    private ServiceHandler serviceHandler;

    private FusedLocationProviderClient locationClient;
    private LocationCallback locationCallback;

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {

        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {

            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setInterval(5000);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationClient = LocationServices.
                    getFusedLocationProviderClient(getApplicationContext());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (getApplicationContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED
                        && getApplicationContext().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }
            locationClient.requestLocationUpdates(locationRequest, locationCallback, serviceLooper);
            Log.d(TAG, "requestLocationUpdates executed");

            // Normally we would do some work here, like download a file.
            // For our sample, we just sleep for 5 seconds.
            /*try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                // Restore interrupt status.
                Thread.currentThread().interrupt();
            }*/
        }
    }

    @Override
    public void onCreate() {
        // A foreground service in order to work in Android has to show a notification, as quoted by
        // the official guide: "Foreground services must display a Notification."
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) // check if Android version is 8 or higher
            startMyOwnForeground(); // put the service in a foreground state - for Android 8+
        else
            startForeground(1, new Notification()); // put the service in a foreground state - for Android 7 or below

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    Log.d(TAG, "location result is NULL");
                    return;
                }
                Log.d(TAG, "Number:" + locationResult.getLocations().size());
                for (Location location : locationResult.getLocations()) {
                    long seconds = TimeUnit.MILLISECONDS.toSeconds(location.getTime());
                    Log.d(TAG, "t:" + seconds + " lat:" + location.getLatitude() + " long:"
                            + location.getLongitude() + " alt:" + location.getAltitude());
                }
            }
        };

        // Start up the thread running the service. Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.
        HandlerThread thread = new HandlerThread("LocationServiceHT",
                Process.THREAD_PRIORITY_DEFAULT); // HandlerThread: Helper class that builds a
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
        Log.d(TAG, "onDestroy called");
        locationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startMyOwnForeground(){
        String NOTIFICATION_CHANNEL_ID = "it.polito.helpenvironmentnow";
        String channelName = "Background HelpEnvironmentNow Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("MOVEMENT mode is ON")
                .setContentText("Continuous location tracking is enabled")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(1, notification);
    }
}
