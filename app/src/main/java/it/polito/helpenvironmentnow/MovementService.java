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
import it.polito.helpenvironmentnow.Storage.JsonTypes;
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
        // TODO remove
//        MyDb myDb = new MyDb(getApplicationContext());
//        Position[] positions = myDb.selectPositions(1579089600, 1579096800);
//        for(Position p : positions) {
//            Log.d("TEST", p.timestamp + " lat:" + p.latitude + " lon:" + p.longitude + " alt:" + p.altitude);
//        }
//        myDb.closeDb();
        // TODO remove

        /* the remote device is the the Raspberry Pi */
        String remoteDeviceMacAddress = intent.getStringExtra("remoteMacAddress");
        RaspberryPi rPi = new RaspberryPi();
        boolean readResult = rPi.connectAndRead(remoteDeviceMacAddress);
        if(readResult) {
            /* readResult = true -> Data has been read correctly from Raspberry Pi */
            Parser parser = new Parser();
            ParsedData parsedData = parser.parseEnvironmentalData(rPi.getDhtMetaData(),
                rPi.getDhtFixedData(), rPi.getDhtVariableData(), rPi.getPmMetaData(), rPi.getPmVariableData());
            Map<Integer, Position> positionsMap = getMapOfPositions(parsedData);
            Matcher matcher = new Matcher();
            MatchedData matchedData = matcher.matchMeasuresAndPositions(parsedData, positionsMap);
            /* Build the json object filling it with data from Raspberry Pi and location data */
            JsonBuilder jsonBuilder = new JsonBuilder();
            JSONObject dataBlock = jsonBuilder.buildMovementJson(matchedData);
            if(NetworkInfo.isNetworkAvailable(this)) {
                /* Send json object to the server */
                Log.d("MovementService", "Network available!");
                HeRestClient heRestClient = new HeRestClient(getApplicationContext());
                heRestClient.sendToServer(dataBlock);
            } else {
                /* Network is not available, I store it in local database and I enqueue a work that
                 * the Worker Manager will execute when network became available */
                Log.d("MovementService", "Network NOT available!");
                MyDb myDb = new MyDb(getApplicationContext());
                myDb.storeJsonObject(dataBlock, JsonTypes.MOVEMENT);
                myDb.closeDb();
                MyWorkerManager.enqueueNetworkWorker(getApplicationContext());
            }
            Log.d("MovementService", "All executed!");
        }
    }

    private Map<Integer, Position> getMapOfPositions(ParsedData parsedData) {
        int[] timestamps = getMinMaxTimestamp(parsedData);
        MyDb myDb = new MyDb(getApplicationContext());
        Position[] positions = myDb.selectPositions(timestamps[0], timestamps[1]);
        Map<Integer, Position> positionsMap = new HashMap<>();
        for(Position p : positions)
            positionsMap.put(p.timestamp, p);

        return positionsMap;
    }

    // Gets the min and max timestamp from the received environmental data
    // min and max timestamps are then used to extract locations from local db to match them together
    private int[] getMinMaxTimestamp(ParsedData parsedData) {
        int[] result = new int[2]; // index 0 for min and 1 for max

        int minDht = (Collections.min(parsedData.getDhtMeasures())).getTimestamp();
        int maxDht = (Collections.max(parsedData.getDhtMeasures())).getTimestamp();
        int minPm = (Collections.min(parsedData.getPmMeasures())).getTimestamp();
        int maxPm = (Collections.max(parsedData.getPmMeasures())).getTimestamp();

        result[0] = minDht <= minPm ? minDht : minPm;
        result[1] = maxDht >= maxPm ? maxDht : maxPm;

        return result;
    }
}
