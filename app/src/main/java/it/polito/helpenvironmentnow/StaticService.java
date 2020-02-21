package it.polito.helpenvironmentnow;

import android.app.IntentService;
import android.app.Notification;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import it.polito.helpenvironmentnow.Helper.ServiceNotification;
import it.polito.helpenvironmentnow.MyWorker.MyWorkerManager;
import it.polito.helpenvironmentnow.Storage.MyDb;

public class StaticService extends IntentService /*implements MyLocationListener*/ {

    private final int SERVICE_ID = 1;

//    private Location curLocation;
    private boolean curLocationReady = false;

    public StaticService() {
        super("StaticService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // A foreground service in order to work in Android has to show a notification, as quoted by
        // the official guide: "Foreground services must display a Notification."
        ServiceNotification.notifyForeground(this, SERVICE_ID, "Background Temporary Service",
                "Receiving environmental data from Pi device");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

//        LocationInfo.getCurrentLocation(getApplicationContext(), this);
//        /* Wait until the device current location is returned. When location is ready, locationCompleted(...)
//         * is called and sets curLocationReady to true and so the while cycle will be interrupted
//         * and the field curLocation will contain latitude, longitude, altitude */
//        while(!curLocationReady) {
//            int WAIT_LOCATION_MS = 500;
//            SystemClock.sleep(WAIT_LOCATION_MS);
//        }

        // Open the db connection. It will be used inside StaticRaspberryPi object
        MyDb myDb = new MyDb(getApplicationContext());

        /* the remote device is the the Raspberry Pi device */
        String remoteDeviceMacAddress = intent.getStringExtra("remoteMacAddress");
        StaticRaspberryPi rPi = new StaticRaspberryPi();
        long insertions = rPi.connectAndRead(remoteDeviceMacAddress, myDb);
        // Close the db connection as it is not used anymore
        myDb.closeDb();
        if(insertions > 0) {
            /* I enqueue a work that the Worker Manager will execute when network became available */
            MyWorkerManager.enqueueNetworkWorker(getApplicationContext());
        }
        Log.d("StaticService", "Service Completed");
    }

//    @Override
//    public void locationCompleted(Location location) {
//        curLocation = location;
//        curLocationReady = true;
//    }
}
