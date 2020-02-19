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

public class DynamicRaspberryPi {

    private String TAG = "DynamicRaspberryPi";
    private static final int DYNAMIC_CHANNEL = 2;
    private final byte[] ACK_CHUNK = "ok".getBytes();
    private long totalInsertions = 0;

    public boolean connect(String remoteDeviceMacAddress) {
        boolean result = false;
        BtConnection btConn = new BtConnection();
        RfcommChannel rfcommChannel = btConn.establishConnection(remoteDeviceMacAddress, DYNAMIC_CHANNEL);
        if(rfcommChannel != null) {
            result = true;
        }

        return result;
    }

    public void read() {

    }

    // This method calls "connectAndReadFromRaspberry" and returns the number of measures received
    // and inserted into local database
    /*public long connectAndRead(String remoteDeviceMacAddress, MyDb myDb) {

        BtConnection btConn = new BtConnection();
        BluetoothSocket socket = btConn.establishConnection(remoteDeviceMacAddress, DYNAMIC_CHANNEL);
        if(socket != null) {
            if(socket.isConnected()) {
                try {
                    InputStream socketInputStream = socket.getInputStream();
                    OutputStream socketOutputStream = socket.getOutputStream();
                    readSaveChunks(socketInputStream, socketOutputStream, myDb);
                } catch (IOException e) {
                    Log.e(TAG, "socket IOException during readSaveChunks");
                    e.printStackTrace();
                } finally {
                    try {
                        Log.d(TAG, "I close connected socket.");
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return totalInsertions;
    }*/

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
                reader.endObject();

                Measure m = new Measure();
                m.sensorId = sensorId;
                m.timestamp = timestamp;
                m.data = data;

                measures.add(m);
            }
            reader.endArray();
            reader.endObject();

        }

        return measures;
    }
}
