package it.polito.helpenvironmentnow;

import android.app.IntentService;
import android.app.Notification;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import it.polito.helpenvironmentnow.Helper.ServiceNotification;
import it.polito.helpenvironmentnow.MyWorker.MyWorkerManager;
import it.polito.helpenvironmentnow.Storage.MyDb;

public class MovementService extends IntentService {

    public MovementService() {
        super("MovementService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // A foreground service in order to work in Android has to show a notification, as quoted by
        // the official guide: "Foreground services must display a Notification."
        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) // check if Android version is 8 or higher
            notification = ServiceNotification.getMyOwnNotification(this, "Temporary HelpEnvironmentNow Service",
                    "Background Temporary Service(movement mode)", "Environmental data exchange"); // foreground service notification for Android 8+
        else
            notification =  new Notification(); // foreground service notification for Android 7.x or below
        startForeground(1, notification);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        // Open the db connection. It will be used inside Raspberry Pi object
        MyDb myDb = new MyDb(getApplicationContext());

        /* the remote device is the the Raspberry Pi */
        String remoteDeviceMacAddress = intent.getStringExtra("remoteMacAddress");
        RaspberryPi rPi = new RaspberryPi();
        long insertions = rPi.connectAndRead(remoteDeviceMacAddress, null, myDb);
        // Close the db connection as it is not used anymore
        myDb.closeDb();
        if(insertions > 0) {
            /* I enqueue a work that the Worker Manager will execute when network became available */
            MyWorkerManager.enqueueNetworkWorker(getApplicationContext());
            Log.d("ClassicService", "Service Completed");
        }

        Log.d("MovementService", "Service completed!");
    }
}
