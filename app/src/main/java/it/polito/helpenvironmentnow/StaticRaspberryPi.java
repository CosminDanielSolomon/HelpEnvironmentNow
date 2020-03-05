package it.polito.helpenvironmentnow;

import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import it.polito.helpenvironmentnow.Helper.BtConnection;
import it.polito.helpenvironmentnow.Helper.RfcommChannel;
import it.polito.helpenvironmentnow.Storage.Measure;
import it.polito.helpenvironmentnow.Storage.MyDb;

// The main purpose of this class is to connect to the Raspberry Pi device and manage the connection.
// The measures are inserted into a local database.
public class StaticRaspberryPi {

    private String TAG = "StaticRaspberryPi";
    private final int STATIC_CHANNEL = 1;
    private long totalInsertions = 0;
    private RfcommChannel rfcommChannel;

    // This method calls "connectAndReadFromRaspberry" and returns the number of measures received
    // and inserted into local database
    public long connectAndRead(String remoteDeviceMacAddress, MyDb myDb) {
        BtConnection btConn = new BtConnection();
        rfcommChannel = btConn.establishConnection(remoteDeviceMacAddress, STATIC_CHANNEL);
        if(rfcommChannel != null) {
            try {
                readChunks(myDb);
            } catch (IOException | IllegalStateException | NumberFormatException e) {
                Log.e(TAG, "Exception during readChunks");
                e.printStackTrace();
            } finally {
                rfcommChannel.close();
            }
        }

        return totalInsertions;
    }

    private void readChunks(MyDb myDb) throws IOException, IllegalStateException,
            NumberFormatException {
        boolean finished = false;
        // this loop is interrupted by an IOException OR when the server has finished to send data
        while (!finished) {
            JsonReader jsonReader = rfcommChannel.getJsonReader();
            finished = readCompleted(jsonReader);
            if (!finished) {
                List<Measure> measures = readChunk(jsonReader);
                // save measures into local database
                if (measures.size() > 0)
                    myDb.insertMeasures(measures);
                totalInsertions += measures.size();
                // send ACK for the received chunk to the server
                JsonWriter jsonWriter = rfcommChannel.getJsonWriter();
                sendAck(jsonWriter);
            }
        }
    }

    private boolean readCompleted(JsonReader jsonReader) throws IOException, IllegalStateException,
            NumberFormatException {
        jsonReader.beginObject(); // consumes the first '{' of the json object
        String name = jsonReader.nextName();
        if (name.equals("m")) {
            return false;
        } else if(name.equals("action")) {
            jsonReader.nextString();
            jsonReader.endObject();
            return true;
        }
        return true;
    }

    // A chunk is a JSON format containing an array of measures
    private List<Measure> readChunk(JsonReader jsonReader) throws IOException, IllegalStateException,
            NumberFormatException {
        List<Measure> measures = new ArrayList<>();

        jsonReader.beginArray(); // consumes the first '[' of the json array
        while (jsonReader.hasNext()) {
            boolean sID = false, ts = false, dt = false, gh = false, al = false;
            Measure m = new Measure();
            jsonReader.beginObject();
            while (jsonReader.hasNext()) {
                String name = jsonReader.nextName();
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
                    case "geohash":
                        m.geoHash = jsonReader.nextString();
                        gh = true;
                        break;
                    case "altitude":
                        m.altitude = jsonReader.nextDouble();
                        al = true;
                        break;
                }
            }
            jsonReader.endObject();
            if (sID && ts && dt && gh && al)
                measures.add(m);
        }
        jsonReader.endArray();
        jsonReader.endObject();

        return measures;
    }

    private void sendAck(JsonWriter jsonWriter) throws IOException {
        final String ACTION = "action";
        final String OK = "ok";

        jsonWriter.beginObject();
        jsonWriter.name(ACTION).value(OK);
        jsonWriter.endObject();
        jsonWriter.flush();
    }
}
