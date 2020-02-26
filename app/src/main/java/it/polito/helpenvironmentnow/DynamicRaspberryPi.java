package it.polito.helpenvironmentnow;

import android.content.Context;
import android.location.Location;
import android.os.SystemClock;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
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
    private final int READ_SLEEP_MILLI_SEC = 1000;
    private final int LOC_SLEEP_MILLI_SEC = 100;
    private final int MAX_WAIT_MILLI_SEC = 5000;
    private final int SINGLE_WAIT_MILLI_SEC = 500;

    private Context context;
    private BtConnection btConn = null;
    private RfcommChannel rfcommChannel = null;
    private AtomicBoolean locationReady = new AtomicBoolean(false);
    private Location lastLocation;

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

    public void requestData() throws IOException {
        OutputStreamWriter osw = new OutputStreamWriter(rfcommChannel.getChannelOutputStream(),
                StandardCharsets.UTF_8);
        JsonWriter jsonWriter = new JsonWriter(osw);
        try {
            writeActionGET(jsonWriter);
            jsonWriter.flush();
            //closeWriter(jsonWriter);
        } catch (IOException e) {
            closeWriter(jsonWriter);
            btConn.closeConnection();
            throw e;
        }
    }

    private void writeActionGET(JsonWriter jsonWriter) throws IOException {
        jsonWriter.beginObject();
        jsonWriter.name("action").value("get");
        jsonWriter.endObject();
    }

    public void read() throws IOException {
        if(rfcommChannel != null) {
            Matcher matcher = new Matcher();
            MyDb myDb = new MyDb(context);
            InputStreamReader isr = new InputStreamReader(rfcommChannel.getChannelInputStream(),
                    StandardCharsets.UTF_8);
            JsonReader jsonReader = new JsonReader(isr);
            jsonReader.setLenient(true); // it is needed in order to avoid MalformedJsonException before reading the second chunk and the others that follow
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
                    }
                    Log.d(TAG, "min: " + min +" max: " + max);
                    // TODO remove ---

                }
            } catch (IOException e) {
                e.printStackTrace();
                myDb.closeDb();
                closeReader(jsonReader);
                btConn.closeConnection();
                throw e;
            }
            myDb.closeDb();
            //closeReader(jsonReader);
        }
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

    public void sendLocation() {
        boolean started = LocationInfo.getCurrentLocation(context, this);
        if(started) {
            while (!locationReady.get()) {
                SystemClock.sleep(LOC_SLEEP_MILLI_SEC);
            }
            String geohash = LocationInfo.encodeLocation(lastLocation);
            double altitude = lastLocation.getAltitude();
            OutputStreamWriter osw = new OutputStreamWriter(rfcommChannel.getChannelOutputStream(),
                    StandardCharsets.UTF_8);
            JsonWriter writer = new JsonWriter(osw);
            try {
                writeLocation(writer, geohash, altitude);
            } catch (IOException e) {
                Log.e(TAG, "Connection end in wrong way!");
            } finally {
                closeWriter(writer);
                btConn.closeConnection();
            }
        } else {
            // TODO inform user that permissions are needed
        }
    }

    private void writeLocation(JsonWriter writer, String geohash, double altitude) throws IOException {
        writer.beginObject();
        writer.name("action").value("set");
        writer.name("geohash").value(geohash);
        writer.name("altitude").value(altitude);
        writer.endObject();
    }

    @Override
    public void locationCompleted(Location location) {
        lastLocation = location;
        locationReady.set(true);
    }

    public boolean waitLocationAck() throws IOException {
        InputStreamReader isr = new InputStreamReader(rfcommChannel.getChannelInputStream(),
                StandardCharsets.UTF_8);
        JsonReader jsonReader = new JsonReader(isr);
        int current_wait = 0;
        boolean ack = false;
        while (current_wait < MAX_WAIT_MILLI_SEC && !ack) {
            try {
                if (isr.ready()) {
                    if (readAndCheckAck(jsonReader))
                        ack = true;
                    else
                        current_wait = MAX_WAIT_MILLI_SEC;
                    closeReader(jsonReader);
                } else {
                    SystemClock.sleep(SINGLE_WAIT_MILLI_SEC);
                    current_wait += SINGLE_WAIT_MILLI_SEC;
                }
            } catch (IOException e) {
                closeReader(jsonReader);
                btConn.closeConnection();
                throw e;
            }
        }
        return ack;
    }

    private boolean readAndCheckAck(JsonReader jsonReader) throws IOException {
        String expectedAck = "ok";
        jsonReader.beginObject();
        jsonReader.nextName();
        String value = jsonReader.nextString();
        jsonReader.endObject();
        return value.equals(expectedAck);
    }

    private void closeReader(JsonReader jsonReader) {
        try {
            jsonReader.close();
        } catch (IOException e) {
            Log.e(TAG, "Close JsonReader failed!");
        }
    }

    private void closeWriter(JsonWriter jsonWriter) {
        try {
            jsonWriter.close();
        } catch (IOException e) {
            Log.e(TAG, "Close JsonWriter failed!");
        }
    }
}
