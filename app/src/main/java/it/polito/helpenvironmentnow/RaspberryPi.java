package it.polito.helpenvironmentnow;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.location.Location;
import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import it.polito.helpenvironmentnow.Helper.DhtMetaData;
import it.polito.helpenvironmentnow.Helper.Matcher;
import it.polito.helpenvironmentnow.Helper.Parser;
import it.polito.helpenvironmentnow.Helper.PmMetaData;
import it.polito.helpenvironmentnow.Storage.Measure;
import it.polito.helpenvironmentnow.Storage.MyDb;

import static it.polito.helpenvironmentnow.Helper.Parser.DHT_META_DATA_CHARS;
import static it.polito.helpenvironmentnow.Helper.Parser.PM_META_DATA_CHARS;

// The main purpose of this class is to connect to the Raspberry Pi device, read the stream of bytes,
// parse it to obtain each measure and associate it(matching) with a location. While receiving the
// data, the measures are inserted into a local database.
// At the end the Raspberry Pi is acknowledged and the socket connection is close.
public class RaspberryPi {

    private String TAG = "RaspberryPi";
    private static final int MAX_CONNECTION_ATTEMPTS = 15; // the max number to retry establish bluetooth connection if fails
    private static final int BLUETOOTH_MSECONDS_SLEEP = 3000; // milliseconds to sleep if open connection fails

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

                    Matcher matcher = new Matcher();
                    Parser parser = new Parser();

                    String dhtMetaData = readTempHumMetaData(socketInputStream);
                    DhtMetaData parsedDhtMetaData = parser.parseDhtMetaData(dhtMetaData);
                    byte[] dhtFixedData = readFixedSensorsData(socketInputStream, parsedDhtMetaData);
                    readAndSaveDhtData(socketInputStream, parsedDhtMetaData, dhtFixedData, matcher, location, myDb);

                    String pmMetaData = readPmMetaData(socketInputStream);
                    PmMetaData parsedPmMetaData = parser.parsePmMetaData(pmMetaData);
                    readAndSavePmData(socketInputStream, parsedPmMetaData, matcher, location, myDb);

                    OutputStream socketOutputStream = socket.getOutputStream();
                    byte[] ack = "ok".getBytes();
                    socketOutputStream.write(ack);
                    socketOutputStream.flush();

                    // location is null only if MOVEMENT mode is on
                    if(location == null)
                        myDb.deletePositions(matcher.getMaxOverallTimestamp());
                } catch (IOException e) {
                    Log.e(TAG, "Read or write to socket failed!");
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

    // This method reads the number of messages that follows and their size
    private String readTempHumMetaData(InputStream socketInputStream) throws IOException {
        byte[] buffer = new byte[DHT_META_DATA_CHARS];

        readSocketData(socketInputStream, buffer, DHT_META_DATA_CHARS);

        return new String(buffer, StandardCharsets.UTF_8);
    }

    private byte[] readFixedSensorsData(InputStream socketInputStream, DhtMetaData dhtMetaData) throws IOException {
        final int sensorIds = 2; // one sensorId for temperature and one for humidity
        int totalDataSize = dhtMetaData.getSensorIdLength() * sensorIds;
        byte[] dhtFixedData = new byte[totalDataSize];

        readSocketData(socketInputStream, dhtFixedData, totalDataSize);

        return dhtFixedData;
    }

    // This method receives all DHT data from Raspberry Pi, parse them and save in local db
    // I save in db, and not in local buffer for scalable reasons(i.e. tens of MB of data received from Raspberry Pi)
    private void readAndSaveDhtData(InputStream socketInputStream, DhtMetaData dhtMetaData,
                                    byte[] fixedDhtData, Matcher matcher, Location loc, MyDb myDb) throws IOException {

        Parser parser = new Parser();
        int sensorIdTemperature = parser.parseSensorIdTemperature(fixedDhtData, dhtMetaData.getSensorIdLength());
        int sensorIdHumidity = parser.parseSensorIdHumidity(fixedDhtData, dhtMetaData.getSensorIdLength());

        final int N_MEASURES = 1000; // max measures to receive in one cycle, before parse them
        int measureLength = dhtMetaData.getReadLength(); // is the measure length in bytes
        final int BUFFER_SIZE = N_MEASURES * measureLength; // size of the local buffer
        byte[] data = new byte[BUFFER_SIZE];

        int totalMeasures = dhtMetaData.getNumberOfReads();
        int currentMeasures = 0, size, result = 0;
        int n = N_MEASURES;
        while(currentMeasures < totalMeasures && result != -1) {

            if((totalMeasures - currentMeasures) < N_MEASURES)
                n = totalMeasures - currentMeasures;

            size = n * measureLength;
            result = readSocketData(socketInputStream, data, size);
            if(result == size) {
                currentMeasures += n;

                // I parse the stream of bytes and I build Measure objects
                List<Measure> parsedMeasures = parser.parseDhtData(sensorIdTemperature,
                        sensorIdHumidity, data, n, dhtMetaData);
                // I assign position to each measure
                matcher.matchMeasuresAndPositions(loc, parsedMeasures, myDb);
                // I save measures into local database
                myDb.insertMeasures(parsedMeasures);
                totalInsertions += n * 2;
            } else {
                result = -1;
            }

        }
    }

    // This method reads the number of messages that follows and their size
    private String readPmMetaData(InputStream socketInputStream) throws IOException {
        byte[] buffer = new byte[PM_META_DATA_CHARS];

        readSocketData(socketInputStream, buffer, PM_META_DATA_CHARS);

        return new String(buffer, StandardCharsets.UTF_8);
    }

    // This method receives all PM data from Raspberry Pi, parse them and save in local db
    // I save in db, and not in local buffer for scalable reasons(i.e. tens of MB of data received from Raspberry Pi)
    private void readAndSavePmData(InputStream socketInputStream, PmMetaData pmMetaData, Matcher matcher,
                                   Location loc, MyDb myDb) throws IOException {

        Parser parser = new Parser();

        final int N_MEASURES = 8000; // max measures to receive in one cycle, before parse them
        int measureLength = pmMetaData.getReadLength(); // is the measure length
        final int BUFFER_SIZE = N_MEASURES * measureLength; // size of the local buffer
        byte[] data = new byte[BUFFER_SIZE];

        int totalMeasures = pmMetaData.getNumberOfReads();
        int currentMeasures = 0, size, result = 0;
        int n = N_MEASURES;
        while(currentMeasures < totalMeasures && result != -1) {

            if((totalMeasures - currentMeasures) < N_MEASURES)
                n = totalMeasures - currentMeasures;

            size = n * measureLength;
            result = readSocketData(socketInputStream, data, size);
            if(result == size) {
                currentMeasures += n;

                List<Measure> parsedMeasures = parser.parsePmData(data, n, pmMetaData);
                // I assign position to each measure
                matcher.matchMeasuresAndPositions(loc, parsedMeasures, myDb);
                // I save measures into local database
                myDb.insertMeasures(parsedMeasures);
                totalInsertions += n * 2;
            } else {
                result = -1;
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

    // Low level method that reads "size" bytes or fails and returns -1
    private int readSocketData(InputStream socketInputStream, byte[] buffer, int size) throws IOException {
        int resultRead, bytesRead = 0;

        do {
            resultRead = socketInputStream.read(buffer, bytesRead, size - bytesRead);
            if(resultRead != -1)
                bytesRead += resultRead;
        } while(bytesRead < size && resultRead != -1);

        return bytesRead;
    }
}
