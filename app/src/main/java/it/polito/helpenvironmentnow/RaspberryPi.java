package it.polito.helpenvironmentnow;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

public class RaspberryPi {

    private String TAG = "AppHelpNow";
    private static final int MAX_CONNNECTION_ATTEMPTS = 10; // the max number to retry establish bluetooth connection if fails
    private static final int BLUETOOTH_MSECONDS_SLEEP = 3000; // milliseconds to sleep after connection fails
    private static final int NUMBER_OF_MESSAGES_CHARS = 8; // the number of chars used to represent the total number of messages
    private static final int MESSAGE_SIZE_CHARS = 4; // the number of chars used to represent the size of a message
    private static final int SINGLE_READ_SIZE = 1024; // bytes to read with a single call to "read()" - the same size on server side(SINGLE_WRITE_SIZE)
    private BluetoothAdapter bluetoothAdapter;
    private int numberOfMessages, messageSize; // these fields will be set inside "readMetaDataMessages" after receiving them from raspberry
    private byte[] messagesRead; // contains the sensor data(with timestamps) received from raspberry

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

    public byte[] getMessagesRead() {
        return messagesRead;
    }

    public int getNumberOfMessages() {
        return numberOfMessages;
    }

    public int getMessageSize() {
        return messageSize;
    }

    // This method reads the number of messages that follows and their size and sets the private
    // variables numberOfMessages and messageSize
    private void readMetaDataMessages(InputStream socketInputStream) throws IOException {
        byte[] buffer = new byte[NUMBER_OF_MESSAGES_CHARS + MESSAGE_SIZE_CHARS];
        int resultRead, bytesRead = 0;

        do {
            resultRead = socketInputStream.read(buffer, bytesRead, NUMBER_OF_MESSAGES_CHARS + MESSAGE_SIZE_CHARS - bytesRead);
            if(resultRead != -1)
                bytesRead += resultRead;
        } while(bytesRead < NUMBER_OF_MESSAGES_CHARS + MESSAGE_SIZE_CHARS && bytesRead != -1);
        String strMessages = new String(buffer);
        numberOfMessages = Integer.parseInt(strMessages.substring(0, NUMBER_OF_MESSAGES_CHARS));
        messageSize = Integer.parseInt(strMessages.substring(NUMBER_OF_MESSAGES_CHARS, NUMBER_OF_MESSAGES_CHARS + MESSAGE_SIZE_CHARS));
        Log.d(TAG,"readMetaDataMessages() numberOfMessages:" + numberOfMessages + " messageSize:" + messageSize);
    }

    // This method receives all the messages from raspberry and save them into "messagesRead" array
    private void readMessages(InputStream socketInputStream) throws IOException {
        readMetaDataMessages(socketInputStream);
        int totalDataSize = numberOfMessages * messageSize;
        messagesRead = new byte[totalDataSize];

        Log.d(TAG, "readMessages() totalDataSize:"+totalDataSize);
        int currentDataSize = 0, bytesRead = 0;
        int singleReadSize = SINGLE_READ_SIZE;
        while(currentDataSize < totalDataSize && bytesRead != -1) {
            if((totalDataSize - currentDataSize) < SINGLE_READ_SIZE)
                singleReadSize = totalDataSize - currentDataSize;
            bytesRead = socketInputStream.read(messagesRead, currentDataSize, singleReadSize);
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
