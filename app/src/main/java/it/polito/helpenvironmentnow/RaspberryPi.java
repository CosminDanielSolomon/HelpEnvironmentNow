package it.polito.helpenvironmentnow;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.location.Location;
import android.os.SystemClock;
import android.util.JsonReader;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import it.polito.helpenvironmentnow.Helper.Matcher;
import it.polito.helpenvironmentnow.Storage.Measure;
import it.polito.helpenvironmentnow.Storage.MyDb;

// The main purpose of this class is to connect to the Raspberry Pi device, read the stream of bytes,
// parse it to obtain each measure and associate it(matching) with a location. While receiving the
// data, the measures are inserted into a local database.
// At the end the Raspberry Pi is acknowledged and the socket connection is close.
public class RaspberryPi {

    private String TAG = "RaspberryPi";
    private static final int MAX_CONNECTION_ATTEMPTS = 15; // the max number to retry establish bluetooth connection if fails
    private static final int BLUETOOTH_MSECONDS_SLEEP = 3000; // milliseconds to sleep if open connection fails
    private final byte[] ACK_CHUNK = "ok".getBytes();

    private BluetoothAdapter bluetoothAdapter;

    private long totalInsertions = 0;

    // This method calls "connectAndReadFromRaspberry" and returns the number of measures received
    // and inserted into local database
    public long connectAndRead(String remoteDeviceMacAddress, Location location, MyDb myDb) {

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter != null)
            connectAndReadFromRaspberry(remoteDeviceMacAddress, location, myDb);

        return totalInsertions;
    }

    // This method receives the stream of bytes from the Raspberry Pi device, parses and saves the
    // measures into local database, after obtaining(matching) the location for the measures.
    // At the end it acknowledges the Raspberry Pi if no exceptions occur.
    private void connectAndReadFromRaspberry(String remoteDeviceMacAddress, Location location, MyDb myDb) {

        BluetoothSocket socket = getBluetoothSocketByReflection(remoteDeviceMacAddress);
        if(socket != null) {
            int attempt = 1;
            if (bluetoothAdapter.isDiscovering())
                bluetoothAdapter.cancelDiscovery();
            while (attempt <= MAX_CONNECTION_ATTEMPTS && !socket.isConnected()) {
                try {
                    Log.d(TAG, "Socket connect() attempt:" + attempt);
                    socket.connect();
                } catch (IOException e) {
                    Log.d(TAG, "Socket connect() failed!");
                    e.printStackTrace();
                    if (attempt < MAX_CONNECTION_ATTEMPTS)
                        SystemClock.sleep(BLUETOOTH_MSECONDS_SLEEP); // sleep before retry to connect
                }
                attempt++;
            }
            if(socket.isConnected()) {
                try {
                    InputStream socketInputStream = socket.getInputStream();
                    OutputStream socketOutputStream = socket.getOutputStream();
                    readMatchSaveChunks(socketInputStream, socketOutputStream, location, myDb);
                } catch (IOException e) {
                    Log.e(TAG, "socket IOException during readMatchSaveChunks");
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
    }

    private BluetoothSocket getBluetoothSocketByReflection(String remoteDeviceMacAddress) {
        BluetoothDevice remoteDevice;
        BluetoothSocket socket = null;
        if(BluetoothAdapter.checkBluetoothAddress(remoteDeviceMacAddress)) {
            remoteDevice = bluetoothAdapter.getRemoteDevice(remoteDeviceMacAddress);
            try {
                socket = (BluetoothSocket) BluetoothDevice.class.getMethod(
                        "createInsecureRfcommSocket", int.class).invoke(remoteDevice, 1);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }

        return socket;
    }

    private void readMatchSaveChunks(InputStream socketInputStream, OutputStream socketOutputStream,
                                     Location loc, MyDb myDb) throws IOException {
        JsonReader reader = new JsonReader(new InputStreamReader(socketInputStream, StandardCharsets.UTF_8));
        reader.setLenient(true); // it is needed in order to avoid MalformedJsonException before reading the second chunk and the others that follow
        Matcher matcher = new Matcher();
        // this loop is interrupted by an IOException when the server has finished to send data and closes the socket or for any other IOException during data transfer
        while (true) {
            try {
                List<Measure> measures = readChunk(reader);
                // associate position to each measure
                matcher.matchMeasuresAndPositions(loc, measures);
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
