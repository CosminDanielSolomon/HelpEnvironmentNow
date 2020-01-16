package it.polito.helpenvironmentnow;

import android.app.IntentService;
import android.app.Notification;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import org.json.JSONObject;

import it.polito.helpenvironmentnow.Helper.JsonBuilder;
import it.polito.helpenvironmentnow.Helper.LocationInfo;
import it.polito.helpenvironmentnow.Helper.MyLocationListener;
import it.polito.helpenvironmentnow.Helper.NetworkInfo;
import it.polito.helpenvironmentnow.Helper.ParsedData;
import it.polito.helpenvironmentnow.Helper.Parser;
import it.polito.helpenvironmentnow.Helper.ServiceNotification;
import it.polito.helpenvironmentnow.MyWorker.MyWorkerManager;
import it.polito.helpenvironmentnow.Storage.JsonTypes;
import it.polito.helpenvironmentnow.Storage.MyDb;

public class ClassicService extends IntentService implements MyLocationListener {

    private Location curLocation;
    private boolean curLocationReady = false;

    public ClassicService() {
        super("ClassicService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // A foreground service in order to work in Android has to show a notification, as quoted by
        // the official guide: "Foreground services must display a Notification."
        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) // check if Android version is 8 or higher
            notification = ServiceNotification.getMyOwnNotification(this, "Temporary HelpEnvironmentNow Service",
                    "Background Temporary Service(classic mode)", "Environmental data exchange"); // foreground service notification for Android 8+
        else
            notification =  new Notification(); // foreground service notification for Android 7.x or below
        startForeground(1, notification);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        /* the remote device is the the Raspberry Pi */
        String remoteDeviceMacAddress = intent.getStringExtra("remoteMacAddress");
        RaspberryPi rPi = new RaspberryPi();
        boolean readResult = rPi.connectAndRead(remoteDeviceMacAddress);
        if(readResult) {
            /* readResult = true -> Data has been read correctly from Raspberry Pi */
            LocationInfo.getCurrentLocation(getApplicationContext(), this);
            /* Wait until the device current location is returned. When location is ready, locationCompleted(...)
            * is called and sets curLocationReady to true and so the while cycle will be interrupted
            * and the field curLocation will contain latitude, longitude, altitude */
            while(!curLocationReady) {
                int WAIT_LOCATION_MS = 1000;
                SystemClock.sleep(WAIT_LOCATION_MS);
            }

            /* Build the json object filling it with data from Raspberry Pi and location data */
            Parser parser = new Parser();
            ParsedData parsedData = parser.parseEnvironmentalData(rPi.getDhtMetaData(),
                    rPi.getDhtFixedData(), rPi.getDhtVariableData(), rPi.getPmMetaData(), rPi.getPmVariableData());
            JsonBuilder jsonBuilder = new JsonBuilder();
            JSONObject dataBlock = jsonBuilder.buildClassicJson(curLocation, parsedData);

            if(NetworkInfo.isNetworkAvailable(this)) {
                /* Send json object to the server */
                Log.d("ClassicService", "Network available!");
                HeRestClient heRestClient = new HeRestClient(getApplicationContext());
                heRestClient.sendToServer(dataBlock, JsonTypes.CLASSIC);
            } else {
                /* Network is not available, I store it in local database and I enqueue a work that
                 * the Worker Manager will execute when network became available */
                Log.d("ClassicService", "Network NOT available!");
                MyDb myDb = new MyDb(getApplicationContext());
                myDb.storeJsonObject(dataBlock, JsonTypes.CLASSIC);
                myDb.closeDb();
                MyWorkerManager.enqueueNetworkWorker(getApplicationContext());
            }
            Log.d("ClassicService", "Service Completed");
        }
    }

    @Override
    public void locationCompleted(Location location) {
        curLocation = location;
        curLocationReady = true;
    }
}
