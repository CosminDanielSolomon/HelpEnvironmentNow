package it.polito.helpenvironmentnow;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;

import it.polito.helpenvironmentnow.Helper.TempHumMetaData;

public class RaspberryPi {

    private String TAG = "AppHelpNow";
    private static final int MAX_CONNNECTION_ATTEMPTS = 10; // the max number to retry establish bluetooth connection if fails
    private static final int BLUETOOTH_MSECONDS_SLEEP = 3000; // milliseconds to sleep after connection fails

    private static final int NUMBER_OF_MESSAGES_CHARS = 8; // the number of chars used to represent the length(in bytes) of number of messages
    private static final int MESSAGE_LENGTH_CHARS = 4; // the number of chars used to represent the length(in bytes) of a message
    private static final int SENSOR_ID_LENGTH_CHARS = 2; // the number of chars used to represent the length(in bytes) of sensor id (Sensor_SN)
    private static final int TIMESTAMP_LENGTH_CHARS = 2; // the number of chars used to represent the length(in bytes) of timestamp
    private static final int TEMPERATURE_LENGTH_CHARS = 2; // the number of chars used to represent the length(in bytes) of temperature
    private static final int HUMIDITY_LENGTH_CHARS = 2; // the number of chars used to represent the length(in bytes) of humidity
    private static final int META_DATA_CHARS = NUMBER_OF_MESSAGES_CHARS + MESSAGE_LENGTH_CHARS + SENSOR_ID_LENGTH_CHARS +
            TIMESTAMP_LENGTH_CHARS + TEMPERATURE_LENGTH_CHARS + HUMIDITY_LENGTH_CHARS;

    private BluetoothAdapter bluetoothAdapter;
    private TempHumMetaData tempHumMetaData; // object fields will be set inside "readTempHumMetaData" after receiving them from raspberry
    private byte[] fixedSensorsData; // contains the fixed sensor data -> temperature sensor id and humidity sensor id
    private byte[] variableSensorsData; // contains the variable sensor data(with timestamps) received from raspberry

    public RaspberryPi() {

    }

    public boolean connectAndRead(String remoteDeviceMacAddress) {
        boolean result = false;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter != null) {
            Log.d(TAG, "Mac address from service: " + remoteDeviceMacAddress);
            result = connectAndReadFromRaspberry(remoteDeviceMacAddress);
        }

        return result;
    }

    public TempHumMetaData getTempHumMetaData() {
        return tempHumMetaData;
    }

    public byte[] getFixedSensorsData() {
        return fixedSensorsData;
    }
    public byte[] getVariableSensorsData() {
        return variableSensorsData;
    }

    private void readSocketData(InputStream socketInputStream, byte[] buffer, int size) throws IOException {
        int resultRead, bytesRead = 0;

        do {
            resultRead = socketInputStream.read(buffer, bytesRead, size - bytesRead);
            if(resultRead != -1)
                bytesRead += resultRead;
        } while(bytesRead < size && resultRead != -1);
    }

    // This method reads the number of messages that follows and their size and sets the private
    // fields of the object TempHumMetaData
    private void readTempHumMetaData(InputStream socketInputStream) throws IOException {
        byte[] buffer = new byte[META_DATA_CHARS];

        readSocketData(socketInputStream, buffer, META_DATA_CHARS);
        String strMetaData = new String(buffer, StandardCharsets.UTF_8);
        parseMetaData(strMetaData);
    }

    private void parseMetaData(String strMetaData) {
        int bIndex = 0, eIndex = NUMBER_OF_MESSAGES_CHARS;
        tempHumMetaData = new TempHumMetaData();
        tempHumMetaData.setNumberOfMessages(Integer.parseInt(strMetaData.substring(bIndex, eIndex)));
        bIndex = eIndex;
        eIndex += MESSAGE_LENGTH_CHARS;
        tempHumMetaData.setMessageLength(Integer.parseInt(strMetaData.substring(bIndex, eIndex)));
        bIndex = eIndex;
        eIndex += SENSOR_ID_LENGTH_CHARS;
        tempHumMetaData.setSensorIdLength(Integer.parseInt(strMetaData.substring(bIndex, eIndex)));
        bIndex = eIndex;
        eIndex += TIMESTAMP_LENGTH_CHARS;
        tempHumMetaData.setTimestampLength(Integer.parseInt(strMetaData.substring(bIndex, eIndex)));
        bIndex = eIndex;
        eIndex += TEMPERATURE_LENGTH_CHARS;
        tempHumMetaData.setTemperatureLength(Integer.parseInt(strMetaData.substring(bIndex, eIndex)));
        bIndex = eIndex;
        eIndex += HUMIDITY_LENGTH_CHARS;
        tempHumMetaData.setHumidityLength(Integer.parseInt(strMetaData.substring(bIndex, eIndex)));
    }

    private void readFixedSensorsData(InputStream socketInputStream) throws IOException {
        final int sensorIds = 2; // one sensorId for temperature and one for humidity
        int totalDataSize = tempHumMetaData.getSensorIdLength() * sensorIds;
        fixedSensorsData = new byte[totalDataSize];

        readSocketData(socketInputStream, fixedSensorsData, totalDataSize);
    }

    // This method receives all the messages from raspberry and save them into "variableSensorsData" array
    private void readMessages(InputStream socketInputStream) throws IOException {
        final int SINGLE_READ_SIZE = 1024; // bytes to read with a single call to "read()" - the same size on raspberry side(SINGLE_WRITE_SIZE)
        int totalDataSize = tempHumMetaData.getNumberOfMessages() * tempHumMetaData.getMessageLength();
        variableSensorsData = new byte[totalDataSize];

        Log.d(TAG, "readMessages() totalDataSize:"+totalDataSize);
        int currentDataSize = 0, bytesRead = 0;
        int singleReadSize = SINGLE_READ_SIZE;
        while(currentDataSize < totalDataSize && bytesRead != -1) {
            if((totalDataSize - currentDataSize) < SINGLE_READ_SIZE)
                singleReadSize = totalDataSize - currentDataSize;
            bytesRead = socketInputStream.read(variableSensorsData, currentDataSize, singleReadSize);
            if(bytesRead != -1)
                currentDataSize += bytesRead;
            Log.d(TAG, "readMessages():" + bytesRead + " currentDataSize:" + currentDataSize);
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

    // This method returns TRUE if all the data has ben received and is ready to be sent to the server
    private boolean connectAndReadFromRaspberry(String remoteDeviceMacAddress) {
        boolean read = false;

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
                Log.d(TAG, "Socket connected!");
                try {
                    InputStream socketInputStream = socket.getInputStream();
                    readTempHumMetaData(socketInputStream);
                    readFixedSensorsData(socketInputStream);
                    readMessages(socketInputStream);
                    read = true;
                } catch (IOException e) {
                    Log.e(TAG, "Read from socket failed!");
                    e.printStackTrace();
                } finally {
                    try {
                        Log.d(TAG, "Socket connected. I close it.");
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return read;
    }
}
