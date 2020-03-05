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
                }
                return true;
            } catch (IOException | IllegalStateException | NumberFormatException e) {
                releaseResources();
                return false;
            }
        }
        return false;
    }

    // A chunk is a JSON format containing an array of measures
    private List<Measure> readChunk(JsonReader jsonReader) throws IOException, IllegalStateException,
            NumberFormatException {
        List<Measure> measures = new ArrayList<>();

        jsonReader.beginObject(); // consumes the first '{' of the json object
        String name = jsonReader.nextName();
        if (name.equals("m")) {
            jsonReader.beginArray(); // consumes the first '[' of the json array
            while (jsonReader.hasNext()) { // loop for the objects inside the array
                boolean sID = false, ts = false, dt = false;
                Measure m = new Measure();
                jsonReader.beginObject();
                while (jsonReader.hasNext()) { // loop for the elements inside single object
                    name = jsonReader.nextName();
                    switch (name) {
                        case "sensorID":
                            m.sensorId = jsonReader.nextInt();
                            sID = true;
                            break;
                        case "timestamp":
                            m.timestamp = jsonReader.nextInt();
                            ts = true;
                            break;
                        case "data":
                            m.data = jsonReader.nextDouble();
                            dt = true;
                            break;
                    }
                }
                jsonReader.endObject();
                if (sID && ts && dt)
                    measures.add(m);
            }
            jsonReader.endArray();
            jsonReader.endObject();
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
                } catch (IOException | IllegalStateException | NumberFormatException e) {
                    Log.e(TAG, "Connection ended in wrong way!");
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

    private boolean readAndCheckAck(JsonReader jsonReader) throws IOException, IllegalStateException,
            NumberFormatException {
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
