package it.polito.helpenvironmentnow;

import android.content.Context;
import android.os.SystemClock;
import android.util.JsonReader;
import android.util.Log;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import it.polito.helpenvironmentnow.Helper.BtConnection;
import it.polito.helpenvironmentnow.Helper.Matcher;
import it.polito.helpenvironmentnow.Helper.RfcommChannel;
import it.polito.helpenvironmentnow.Storage.Measure;
import it.polito.helpenvironmentnow.Storage.MyDb;

public class DynamicRaspberryPi {

    private String TAG = "DynamicRaspberryPi";
    private static final int DYNAMIC_CHANNEL = 2;
    private final int SLEEP_MILLI_SEC = 1000;

    private Context context;
    private BtConnection btConn = null;
    private RfcommChannel rfcommChannel = null;
    private AtomicBoolean stopRequested = new AtomicBoolean(false);

    public DynamicRaspberryPi(Context context) {
        this.context = context;
    }

    public boolean connect(String remoteDeviceMacAddress) {
        boolean result = false;
        btConn = new BtConnection();
        rfcommChannel = btConn.establishConnection(remoteDeviceMacAddress, DYNAMIC_CHANNEL);
        if(rfcommChannel != null) {
            result = true;
        }

        return result;
    }

    public void read() throws IOException {
        if(rfcommChannel != null) {
            Matcher matcher = new Matcher();
            MyDb myDb = new MyDb(context);
            InputStreamReader isr = new InputStreamReader(rfcommChannel.getChannelInputStream(),
                    StandardCharsets.UTF_8);
            JsonReader jsonReader = new JsonReader(isr);
            jsonReader.setLenient(true); // it is needed in order to avoid MalformedJsonException before reading the second chunk and the others that follow
            while (true) {
                try {
                    if (isr.ready()) {
                        List<Measure> measures = readChunk(jsonReader);
                        if(measures.size() > 0) {
                            // match measures with dynamic positions
                            matcher.matchMeasuresAndPositions(measures, myDb);
                            // save measures into local database
                            myDb.insertMeasures(measures);
                        }
                        // TODO remove ---
                        int min = Integer.MAX_VALUE, max = 0;
                        for(Measure me : measures) {
                            if(me.timestamp < min)
                                min = me.timestamp;
                            if(me.timestamp > max)
                                max = me.timestamp;
                        }
                        Log.d(TAG, "min: " + min +" max: " + max);
                        // TODO remove ---
                    }
                } catch (IOException e) {
                    myDb.closeDb();
                    jsonReader.close();
                    btConn.closeConnection();
                    throw e;
                }
                if(stopRequested.get()) {
                   break;
                } else {
                    SystemClock.sleep(SLEEP_MILLI_SEC);
                }
            }
            myDb.closeDb();
            jsonReader.close();
        }
    }

    public void stopReading() {
        stopRequested.set(true);
    }

    public void sendLocation() {

    }

    public void closeConnection() {
        btConn.closeConnection();
    }

    // A chunk is a JSON format containing an array of measures
    private List<Measure> readChunk(JsonReader reader) throws IOException {
        List<Measure> measures = new ArrayList<>();

        reader.beginObject(); // consumes the first '{' of the json object
        String name = reader.nextName();
        if (name.equals("m")) {

            boolean valid;
            reader.beginArray(); // consumes the first '[' of the json array
            while (reader.hasNext()) {
                valid = true;
                Measure m = new Measure();
                reader.beginObject();
                name = reader.nextName();
                if (name.equals("sensorID")) {
                    m.sensorId = reader.nextInt();
                } else
                    valid = false;
                name = reader.nextName();
                if (name.equals("timestamp")) {
                    m.timestamp = reader.nextInt();
                } else
                    valid = false;
                name = reader.nextName();
                if (name.equals("data")) {
                    m.data = reader.nextDouble();
                } else
                    valid = false;
                reader.endObject();
                if (valid)
                    measures.add(m);
            }
            reader.endArray();
            reader.endObject();

        }

        return measures;
    }
}
