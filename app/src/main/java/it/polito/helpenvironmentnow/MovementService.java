package it.polito.helpenvironmentnow;

import android.app.IntentService;
import android.app.Notification;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import it.polito.helpenvironmentnow.Helper.JsonBuilder;
import it.polito.helpenvironmentnow.Helper.MatchedData;
import it.polito.helpenvironmentnow.Helper.Matcher;
import it.polito.helpenvironmentnow.Helper.NetworkInfo;
import it.polito.helpenvironmentnow.Helper.ParsedData;
import it.polito.helpenvironmentnow.Helper.Parser;
import it.polito.helpenvironmentnow.Helper.ServiceNotification;
import it.polito.helpenvironmentnow.MyWorker.MyWorkerManager;
import it.polito.helpenvironmentnow.Storage.MyDb;
import it.polito.helpenvironmentnow.Storage.Position;

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

//        Map<Integer, Position> positionsMap = getMapOfPositions(parsedData);
//        Matcher matcher = new Matcher();
//        MatchedData matchedData = matcher.matchMeasuresAndPositions(parsedData, positionsMap);
    }

//    private Map<Integer, Position> getMapOfPositions(ParsedData parsedData) {
//        int[] timestamps = getMinMaxTimestamp(parsedData);
//        MyDb myDb = new MyDb(getApplicationContext());
//        Position[] positions = myDb.selectPositions(timestamps[0], timestamps[1]);
//        Map<Integer, Position> positionsMap = new HashMap<>();
//        for(Position p : positions)
//            positionsMap.put(p.timestamp, p);
//        // I delete all the positions that I extract because I don't need them in the future
//        myDb.deletePositions(timestamps[1]);
//        myDb.closeDb();
//
//        return positionsMap;
//    }
//
//    // Gets the min and max timestamp from the received environmental data
//    // min and max timestamps are then used to extract locations from local db to match them together
//    private int[] getMinMaxTimestamp(ParsedData parsedData) {
//        int[] result = new int[2]; // index 0 for min and 1 for max
//
//        int minDht = (Collections.min(parsedData.getDhtMeasures())).getTimestamp();
//        int maxDht = (Collections.max(parsedData.getDhtMeasures())).getTimestamp();
//        int minPm = (Collections.min(parsedData.getPmMeasures())).getTimestamp();
//        int maxPm = (Collections.max(parsedData.getPmMeasures())).getTimestamp();
//
//        result[0] = minDht <= minPm ? minDht : minPm;
//        result[1] = maxDht >= maxPm ? maxDht : maxPm;
//
//        return result;
//    }
}
