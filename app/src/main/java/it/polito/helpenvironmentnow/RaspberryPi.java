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

// The main purpose of this class is to connect to the Raspberry Pi device, read the measures in
// JSON and associate each measure with the location(matching). While receiving the data, the
// measures are inserted into a local database.
// After each chunk of data received the Raspberry Pi is acknowledged.
public class RaspberryPi {

    private String TAG = "RaspberryPi";
    private static final int MAX_CONNECTION_ATTEMPTS = 10; // the max number to retry bluetooth connection if it fails
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
                    Log.e(TAG, "Exception during readMatchSaveChunks");
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
                                     Location loc, MyDb myDb) {

        InputStreamReader isr = new InputStreamReader(socketInputStream, StandardCharsets.UTF_8);
        Matcher matcher = new Matcher();
        // this loop is interrupted by an IOException when the server has finished to send data and closes the socket or for any other Exception during data transfer
        boolean finished = false;
        while (!finished) {
            JsonReader jsonReader = new JsonReader(isr);
            try {
                List<Measure> measures = readChunk(jsonReader);
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

            } catch (IOException | IllegalStateException | NumberFormatException e) {
                closeReader(jsonReader);
                finished = true;
            }
        }

    }

    // A chunk is a JSON format containing an array of measures
    private List<Measure> readChunk(JsonReader reader) throws IOException, IllegalStateException,
            NumberFormatException {
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
                    switch (name) {
                        case "sensorID":
                            m.sensorId = reader.nextInt();
                            sID = true;
                            break;
                        case "timestamp":
                            m.timestamp = reader.nextInt();
                            ts = true;
                            break;
                        case "data":
                            m.data = reader.nextDouble();
                            dt = true;
                            break;
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

    private void closeReader(JsonReader jsonReader) {
        try {
            jsonReader.close();
        } catch (IOException e) {
            Log.e(TAG, "Close JsonReader failed!");
        }
    }
}
