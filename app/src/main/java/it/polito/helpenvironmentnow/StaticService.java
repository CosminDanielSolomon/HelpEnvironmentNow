package it.polito.helpenvironmentnow;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import it.polito.helpenvironmentnow.Helper.ServiceNotification;
import it.polito.helpenvironmentnow.MyWorker.MyWorkerManager;
import it.polito.helpenvironmentnow.Storage.MyDb;

public class StaticService extends IntentService {

    private final int SERVICE_ID = 1;

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
}
