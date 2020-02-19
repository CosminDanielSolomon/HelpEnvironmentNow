package it.polito.helpenvironmentnow;

import android.bluetooth.BluetoothSocket;
import android.util.JsonReader;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
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
    private final byte[] ACK_CHUNK = "ok".getBytes();
    private long totalInsertions = 0;

    // This method calls "connectAndReadFromRaspberry" and returns the number of measures received
    // and inserted into local database
    public long connectAndRead(String remoteDeviceMacAddress, MyDb myDb) {

        BtConnection btConn = new BtConnection();
        RfcommChannel rfcommChannel = btConn.establishConnection(remoteDeviceMacAddress, STATIC_CHANNEL);
        if(rfcommChannel != null) {
            try {
                readSaveChunks(rfcommChannel.getChannelInputStream(), rfcommChannel.getChannelOutputStream(), myDb);
            } catch (IOException e) {
                Log.e(TAG, "socket IOException during readSaveChunks");
                e.printStackTrace();
            } finally {
                btConn.closeConnection();
            }
        }

        return totalInsertions;
    }

    private void readSaveChunks(InputStream socketInputStream, OutputStream socketOutputStream,
                                MyDb myDb) throws IOException {
        JsonReader reader = new JsonReader(new InputStreamReader(socketInputStream, StandardCharsets.UTF_8));
        reader.setLenient(true); // it is needed in order to avoid MalformedJsonException before reading the second chunk and the others that follow
        // this loop is interrupted by an IOException when the server has finished to send data and closes the socket or for any other IOException during data transfer
        while (true) {
            try {
                List<Measure> measures = readChunk(reader);
                // save measures into local database
                myDb.insertMeasures(measures);
                totalInsertions += measures.size();

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
                // send ACK for the received chunk to the server
                socketOutputStream.write(ACK_CHUNK);
                socketOutputStream.flush();

            } catch (IOException e) {
                reader.close();
                throw e;
            }
        }

    }

    // A chunk is a JSON format containing an array of measures
    private List<Measure> readChunk(JsonReader reader) throws IOException {
        List<Measure> measures = new ArrayList<>();

        reader.beginObject(); // consumes the first '{' of the json object
        String name = reader.nextName();
        if (name.equals("m")) {
            int sensorId = 0;
            int timestamp = 0;
            double data = 0;
            String geohash = "";
            double altitude = 0;

            reader.beginArray(); // consumes the first '[' of the json array
            while (reader.hasNext()) {

                reader.beginObject();
                    name = reader.nextName();
                    if (name.equals("sensorID")) {
                        sensorId = reader.nextInt();
                    }
                    name = reader.nextName();
                    if (name.equals("timestamp")) {
                        timestamp = reader.nextInt();
                    }
                    name = reader.nextName();
                    if (name.equals("data")) {
                        data = reader.nextDouble();
                    }
                    name = reader.nextName();
                    if (name.equals("geohash")) {
                        geohash = reader.nextString();
                    }
                    name = reader.nextName();
                    if (name.equals("altitude")) {
                        altitude = reader.nextDouble();
                    }
                reader.endObject();

                Measure m = new Measure();
                m.sensorId = sensorId;
                m.timestamp = timestamp;
                m.data = data;
                m.geoHash = geohash;
                m.altitude = altitude;

                measures.add(m);
            }
            reader.endArray();
            reader.endObject();

        }

        return measures;
    }
}
