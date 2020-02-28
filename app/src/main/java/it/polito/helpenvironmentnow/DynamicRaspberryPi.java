package it.polito.helpenvironmentnow;

import android.content.Context;
import android.location.Location;
import android.os.SystemClock;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import it.polito.helpenvironmentnow.Helper.BtConnection;
import it.polito.helpenvironmentnow.Helper.LocationInfo;
import it.polito.helpenvironmentnow.Helper.Matcher;
import it.polito.helpenvironmentnow.Helper.MyLocationListener;
import it.polito.helpenvironmentnow.Helper.RfcommChannel;
import it.polito.helpenvironmentnow.Storage.Measure;
import it.polito.helpenvironmentnow.Storage.MyDb;

public class DynamicRaspberryPi implements MyLocationListener {

    private String TAG = "DynamicRaspberryPi";
    private static final int DYNAMIC_CHANNEL = 2;
    private final int LOC_SLEEP_MILLI_SEC = 100;

    private Context context;
    private Matcher matcher;
    private MyDb myDb;
    private RfcommChannel rfcommChannel;
    private AtomicBoolean locationReady = new AtomicBoolean(false);
    private Location lastLocation;

    public DynamicRaspberryPi(Context context) {
        this.context = context;
        this.matcher = new Matcher();
        this.myDb = new MyDb(context);
    }

    public boolean connect(String remoteDeviceMacAddress) {
        boolean result = false;
        BtConnection btConn = new BtConnection();
        rfcommChannel = btConn.establishConnection(remoteDeviceMacAddress, DYNAMIC_CHANNEL);
        if(rfcommChannel != null) {
            result = true;
        }

        return result;
    }

    public boolean requestData() {
        if(rfcommChannel != null) {
            JsonWriter jsonWriter = rfcommChannel.getJsonWriter();
            try {
                writeActionGET(jsonWriter);
                return true;
            } catch (IOException e) {
                releaseResources();
                return false;
            }
        }
        return false;
    }

    private void writeActionGET(JsonWriter jsonWriter) throws IOException {
        final String ACTION = "action";
        final String GET = "get";

        jsonWriter.beginObject()
                .name(ACTION).value(GET)
                .endObject()
                .flush();
    }

    public boolean read() {
        if(rfcommChannel != null) {
            JsonReader jsonReader = rfcommChannel.getJsonReader();
            try {
                List<Measure> measures = readChunk(jsonReader);
                if(measures.size() > 0) {
                    // match measures with dynamic positions
                    matcher.matchMeasuresAndPositions(measures, myDb);
                    // save measures into local database
                    myDb.insertMeasures(measures);

                    // TODO remove ---
                    int min = Integer.MAX_VALUE, max = 0;
                    for(Measure me : measures) {
                        if(me.timestamp < min)
                            min = me.timestamp;
                        if(me.timestamp > max)
                            max = me.timestamp;
                        Log.d(TAG, me.sensorId + " " + me.timestamp + " " + me.data + " " + me.geoHash + " " + me.altitude);
                    }
                    Log.d(TAG, "MIN: " + min +" MAX: " + max);
                    // TODO remove ---

                }
                return true;
            } catch (IOException e) {
                releaseResources();
                return false;
            }
        }
        return false;
    }

    // A chunk is a JSON format containing an array of measures
    private List<Measure> readChunk(JsonReader reader) throws IOException {
        List<Measure> measures = new ArrayList<>();

        reader.beginObject(); // consumes the first '{' of the json object
        String name = reader.nextName();
        if (name.equals("m")) {

            reader.beginArray(); // consumes the first '[' of the json array
            while (reader.hasNext()) {
                boolean sID = false, ts = false, dt = false;
                Measure m = new Measure();
                reader.beginObject();
                while (reader.hasNext()) {
                    name = reader.nextName();
                    if (name.equals("sensorID")) {
                        m.sensorId = reader.nextInt();
                        sID = true;
                    } if (name.equals("timestamp")) {
                        m.timestamp = reader.nextInt();
                        ts = true;
                    } if (name.equals("data")) {
                        m.data = reader.nextDouble();
                        dt = true;
                    }
                }
                reader.endObject();
                if (sID && ts && dt)
                    measures.add(m);
            }
            reader.endArray();
            reader.endObject();
        }

        return measures;
    }

    public boolean setDynamicModeOff() {
        if (rfcommChannel != null) {
            boolean started = LocationInfo.getCurrentLocation(context, this);
            if (started) {
                while (!locationReady.get()) {
                    SystemClock.sleep(LOC_SLEEP_MILLI_SEC);
                }
                String geohash = LocationInfo.encodeLocation(lastLocation);
                double altitude = lastLocation.getAltitude();
                JsonWriter writer = rfcommChannel.getJsonWriter();
                JsonReader jsonReader = rfcommChannel.getJsonReader();
                try {
                    writeLocation(writer, geohash, altitude);
                    return readAndCheckAck(jsonReader);
                } catch (IOException e) {
                    Log.e(TAG, "Connection end in wrong way!");
                    return false;
                } finally {
                    releaseResources();
                }
            } else {
                return false;
            }
        }
        return false;
    }

    private void writeLocation(JsonWriter writer, String geohash, double altitude) throws IOException {
        final String ACTION = "action";
        final String SET = "set";

        writer.beginObject();
        writer.name(ACTION).value(SET);
        writer.name("geohash").value(geohash);
        writer.name("altitude").value(altitude);
        writer.endObject();
        writer.flush();
    }

    @Override
    public void locationCompleted(Location location) {
        lastLocation = location;
        locationReady.set(true);
    }

    private boolean readAndCheckAck(JsonReader jsonReader) throws IOException {
        final String expectedAck = "ok";
        jsonReader.beginObject();
        jsonReader.nextName();
        String value = jsonReader.nextString();
        jsonReader.endObject();
        return value.equals(expectedAck);
    }

    private void releaseResources() {
        myDb.closeDb();
        rfcommChannel.close();
    }
}
