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

public class RaspberryPi {

    private String TAG = "RaspberryPi";
    private static final int MAX_CONNNECTION_ATTEMPTS = 15; // the max number to retry establish bluetooth connection if fails
    private static final int BLUETOOTH_MSECONDS_SLEEP = 3000; // milliseconds to sleep after connection fails

    private static final int DHT_NUMBER_OF_READS_CHARS = 8; // the number of chars used to represent the length(in bytes) of number of dht reads
    private static final int DHT_READ_LENGTH_CHARS = 4; // the number of chars used to represent the length(in bytes) of a message
    private static final int SENSOR_ID_LENGTH_CHARS = 2; // the number of chars used to represent the length(in bytes) of sensor id (Sensor_SN)
    private static final int TIMESTAMP_LENGTH_CHARS = 2; // the number of chars used to represent the length(in bytes) of timestamp
    private static final int TEMPERATURE_LENGTH_CHARS = 2; // the number of chars used to represent the length(in bytes) of temperature
    private static final int HUMIDITY_LENGTH_CHARS = 2; // the number of chars used to represent the length(in bytes) of humidity
    private static final int DHT_META_DATA_CHARS = DHT_NUMBER_OF_READS_CHARS + DHT_READ_LENGTH_CHARS + SENSOR_ID_LENGTH_CHARS +
            TIMESTAMP_LENGTH_CHARS + TEMPERATURE_LENGTH_CHARS + HUMIDITY_LENGTH_CHARS;

    private static final int PM_NUMBER_OF_READS_CHARS = 8; // the number of chars used to represent the length(in bytes) of number of pm reads
    private static final int PM_READ_LENGTH_CHARS = 4; // the number of chars used to represent the length(in bytes) of a pm read
    private static final int PM_VALUE_LENGTH_CHARS = 1; // the number of chars used to represent the length(in bytes) of pm measure(both pm2.5 and pm10)
    private static final int PM_META_DATA_CHARS = PM_NUMBER_OF_READS_CHARS + PM_READ_LENGTH_CHARS +
            TIMESTAMP_LENGTH_CHARS + PM_VALUE_LENGTH_CHARS + SENSOR_ID_LENGTH_CHARS;

    private BluetoothAdapter bluetoothAdapter;

    private long totalInsertions = 0;

    // This method returns the number of measures inserted into local database
    public long connectAndRead(String remoteDeviceMacAddress, Location location, MyDb myDb) {

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter != null)
            connectAndReadFromRaspberry(remoteDeviceMacAddress, location, myDb);

        return totalInsertions;
    }

    // This method receives all the measure from Raspberry Pi and acknowledges it if no exceptions occur
    private void connectAndReadFromRaspberry(String remoteDeviceMacAddress, Location location, MyDb myDb) {

        BluetoothSocket socket = getBluetoothSocketByReflection(remoteDeviceMacAddress);
        if(socket != null) {
            int attempt = 1;
            if (bluetoothAdapter.isDiscovering())
                bluetoothAdapter.cancelDiscovery();
            while (attempt <= MAX_CONNNECTION_ATTEMPTS && !socket.isConnected()) {
                try {
                    Log.d(TAG, "Socket connect() attempt:" + attempt);
                    socket.connect();
                } catch (IOException e) {
                    Log.d(TAG, "Socket connect() failed!");
                    e.printStackTrace();
                    if (attempt < MAX_CONNNECTION_ATTEMPTS)
                        SystemClock.sleep(BLUETOOTH_MSECONDS_SLEEP); // sleep before retry to connect
                }
                attempt++;
            }
            if(socket.isConnected()) {
                try {
                    InputStream socketInputStream = socket.getInputStream();

                    DhtMetaData dhtMetaData = readTempHumMetaData(socketInputStream);
                    byte[] dhtFixedData = readFixedSensorsData(socketInputStream, dhtMetaData);
                    readAndSaveDhtData(socketInputStream, dhtMetaData, dhtFixedData, location, myDb);
                    Log.d(TAG,"dht variable SUCCESS");

                    PmMetaData pmMetaData = readPmMetaData(socketInputStream);
                    readAndSavePmData(socketInputStream, pmMetaData, location, myDb);
                    Log.d(TAG,"pm variable SUCCESS");

                    OutputStream socketOutputStream = socket.getOutputStream();
                    byte[] ack = "ok".getBytes();
                    socketOutputStream.write(ack);
                    socketOutputStream.flush();
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

    // This method reads the number of messages that follows and their size and sets the private
    // fields of the object DhtMetaData
    private DhtMetaData readTempHumMetaData(InputStream socketInputStream) throws IOException {
        byte[] buffer = new byte[DHT_META_DATA_CHARS];

        readSocketData(socketInputStream, buffer, DHT_META_DATA_CHARS);
        String strMetaData = new String(buffer, StandardCharsets.UTF_8);

        return parseDhtMetaData(strMetaData);
    }

    private DhtMetaData parseDhtMetaData(String strMetaData) {
        int bIndex = 0, eIndex = DHT_NUMBER_OF_READS_CHARS;
        DhtMetaData dhtMetaData = new DhtMetaData();
        dhtMetaData.setNumberOfReads(Integer.parseInt(strMetaData.substring(bIndex, eIndex)));
        bIndex = eIndex;
        eIndex += DHT_READ_LENGTH_CHARS;
        dhtMetaData.setReadLength(Integer.parseInt(strMetaData.substring(bIndex, eIndex)));
        bIndex = eIndex;
        eIndex += SENSOR_ID_LENGTH_CHARS;
        dhtMetaData.setSensorIdLength(Integer.parseInt(strMetaData.substring(bIndex, eIndex)));
        bIndex = eIndex;
        eIndex += TIMESTAMP_LENGTH_CHARS;
        dhtMetaData.setTimestampLength(Integer.parseInt(strMetaData.substring(bIndex, eIndex)));
        bIndex = eIndex;
        eIndex += TEMPERATURE_LENGTH_CHARS;
        dhtMetaData.setTemperatureLength(Integer.parseInt(strMetaData.substring(bIndex, eIndex)));
        bIndex = eIndex;
        eIndex += HUMIDITY_LENGTH_CHARS;
        dhtMetaData.setHumidityLength(Integer.parseInt(strMetaData.substring(bIndex, eIndex)));

        return dhtMetaData;
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
                                    byte[] fixedDhtData, Location loc, MyDb myDb) throws IOException {

        Parser parser = new Parser();
        Matcher matcher = new Matcher();
        int sensorIdTemperature = parser.parseSensorIdTemperature(fixedDhtData, dhtMetaData.getSensorIdLength());
        int sensorIdHumidity = parser.parseSensorIdHumidity(fixedDhtData, dhtMetaData.getSensorIdLength());

        final int N_MEASURES = 1000; // max measures to receive in one cycle, before parse them
        int measureLength = dhtMetaData.getReadLength(); // is the measure length in bytes
        final int BUFFER_SIZE = N_MEASURES * measureLength; // size of the local buffer
        byte[] data = new byte[BUFFER_SIZE];

        int totalMeasures = dhtMetaData.getNumberOfReads();
        Log.d(TAG, "DHT:"+ totalMeasures +"measures");
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
                totalInsertions += n*2;
            } else {
                result = -1;
            }

        }
    }

    // This method reads the number of messages that follows and their size and sets the private
    // fields of the object PmMetaData
    private PmMetaData readPmMetaData(InputStream socketInputStream) throws IOException {
        byte[] buffer = new byte[PM_META_DATA_CHARS];

        readSocketData(socketInputStream, buffer, PM_META_DATA_CHARS);
        String strMetaData = new String(buffer, StandardCharsets.UTF_8);

        return parsePmMetaData(strMetaData);
    }

    private PmMetaData parsePmMetaData(String strMetaData) {
        int bIndex = 0, eIndex = PM_NUMBER_OF_READS_CHARS;
        PmMetaData pmMetaData = new PmMetaData();
        pmMetaData.setNumberOfReads(Integer.parseInt(strMetaData.substring(bIndex, eIndex)));
        bIndex = eIndex;
        eIndex += PM_READ_LENGTH_CHARS;
        pmMetaData.setReadLength(Integer.parseInt(strMetaData.substring(bIndex, eIndex)));
        bIndex = eIndex;
        eIndex += TIMESTAMP_LENGTH_CHARS;
        pmMetaData.setTimestampLength(Integer.parseInt(strMetaData.substring(bIndex, eIndex)));
        bIndex = eIndex;
        eIndex += PM_VALUE_LENGTH_CHARS;
        pmMetaData.setPmValueLength(Integer.parseInt(strMetaData.substring(bIndex, eIndex)));
        bIndex = eIndex;
        eIndex += SENSOR_ID_LENGTH_CHARS;
        pmMetaData.setSensorIdLength(Integer.parseInt(strMetaData.substring(bIndex, eIndex)));

        return pmMetaData;
    }

    // This method receives all PM data from Raspberry Pi, parse them and save in local db
    // I save in db, and not in local buffer for scalable reasons(i.e. tens of MB of data received from Raspberry Pi)
    private void readAndSavePmData(InputStream socketInputStream, PmMetaData pmMetaData,
                                   Location loc, MyDb myDb) throws IOException {

        Parser parser = new Parser();

        final int N_MEASURES = 8000; // max measures to receive in one cycle, before parse them
        int measureLength = pmMetaData.getReadLength(); // is the measure length
        final int BUFFER_SIZE = N_MEASURES * measureLength; // size of the local buffer
        byte[] data = new byte[BUFFER_SIZE];

        int totalMeasures = pmMetaData.getNumberOfReads();
        Log.d(TAG, "PM:"+ totalMeasures +"measures");
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
               // TODO match
                myDb.insertMeasures(parsedMeasures);
                totalInsertions += n*2;
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
