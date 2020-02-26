package it.polito.helpenvironmentnow.Helper;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;

public class BtConnection {

    private String TAG = "BtConnection";
    private static final int MAX_CONNECTION_ATTEMPTS = 5; // the max number to retry establish bluetooth connection if fails
    private static final int BLUETOOTH_MSECONDS_SLEEP = 2000; // milliseconds to sleep if open connection fails
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket socket = null;

    public BtConnection() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public RfcommChannel establishConnection(String remoteDeviceMacAddress, int channel) {
        RfcommChannel rfcommChannel = null;

        if(bluetoothAdapter != null) {
            socket = getBluetoothSocketByReflection(remoteDeviceMacAddress, channel);
            if (socket != null) {
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
                        rfcommChannel = new RfcommChannel(socket.getInputStream(), socket.getOutputStream());
                    } catch (IOException e) {
                        closeConnection();
                    }
                }
            }
        }

        return rfcommChannel;
    }

    public void closeConnection() {
        if (socket != null) {
            try {
                Log.d(TAG, "I close connected socket.");
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "Close bluetooth socket failed!");
            }
        }
    }

    private BluetoothSocket getBluetoothSocketByReflection(String remoteDeviceMacAddress, int channel) {
        BluetoothDevice remoteDevice;
        BluetoothSocket socket = null;
        if(BluetoothAdapter.checkBluetoothAddress(remoteDeviceMacAddress)) {
            remoteDevice = bluetoothAdapter.getRemoteDevice(remoteDeviceMacAddress);
            try {
                socket = (BluetoothSocket) BluetoothDevice.class.getMethod(
                        "createInsecureRfcommSocket", int.class).invoke(remoteDevice, channel);
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
}
