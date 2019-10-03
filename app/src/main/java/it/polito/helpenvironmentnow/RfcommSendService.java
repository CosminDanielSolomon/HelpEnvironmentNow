package it.polito.helpenvironmentnow;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.SystemClock;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

public class RfcommSendService extends IntentService {

    private String TAG = "AppHelpNow";
    private static final int MAX_ATTEMPTS = 10; // the max number to retry establish bluetooth connection if fails
    private static final int BLUETOOTH_MSECONDS_SLEEP = 3000; // milliseconds to sleep after connection fails
    private static final int NUMBER_OF_MESSAGES_CHARS = 8; // the number of chars used to represent the total number of messages
    private static final int MESSAGE_SIZE_CHARS = 4; // the number of chars used to represent the size of a message
    private static final int SINGLE_READ_SIZE = 1024; // bytes to read with a single call to "read()" - the same size on server side(SINGLE_WRITE_SIZE)
    private int numberOfMessages, messageSize;
    private byte[] messagesRead;

    public RfcommSendService() {
        super("Rfcomm Send Service");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // A foreground service in order to work in Android has to show a notification, as quoted by
        // the official guide: "Foreground services must display a Notification."
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startMyOwnForeground(); // put the service in a foreground state
        else
            startForeground(1, new Notification()); // put the service in a foreground state
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "onHandleIntent(...) called");
        String remoteDeviceMacAddress = intent.getStringExtra("remoteMacAddress");
        Log.d(TAG,"Mac address from intent: " + remoteDeviceMacAddress);
        boolean result = connectAndReadFromRaspberry(remoteDeviceMacAddress);
        if(result)
            //connectAndSendToServer();
            Log.d(TAG, "onHandleIntent(...) data:" + Arrays.toString(messagesRead));
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startMyOwnForeground(){
        String NOTIFICATION_CHANNEL_ID = "it.polito.helpenvironmentnow";
        String channelName = "Background HelpEnvironmentNow Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("HelpEnvironmentNow is running in background")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(1, notification);
    }

    // This method reads the number of messages that follows and their size and sets the private
    // variables numberOfMessages and messageSize
    private void readMetaDataMessages(InputStream socketInputStream) throws IOException {
        byte[] buffer = new byte[NUMBER_OF_MESSAGES_CHARS + MESSAGE_SIZE_CHARS];
        int bytes_read = 0;

        do {
            bytes_read += socketInputStream.read(buffer, bytes_read, NUMBER_OF_MESSAGES_CHARS + MESSAGE_SIZE_CHARS - bytes_read);
        } while(bytes_read < NUMBER_OF_MESSAGES_CHARS + MESSAGE_SIZE_CHARS);
        String strMessages = new String(buffer);
        numberOfMessages = Integer.parseInt(strMessages.substring(0,NUMBER_OF_MESSAGES_CHARS));
        messageSize = Integer.parseInt(strMessages.substring(NUMBER_OF_MESSAGES_CHARS, NUMBER_OF_MESSAGES_CHARS + MESSAGE_SIZE_CHARS));
        Log.d(TAG,"in readMetaDataMessages() numberOfMessages:"+numberOfMessages);
        Log.d(TAG,"in readMetaDataMessages() messageSize:"+messageSize);
    }

    private void readMessages(InputStream socketInputStream) throws IOException {
        readMetaDataMessages(socketInputStream);
        int totalDataSize = numberOfMessages * messageSize;
        messagesRead = new byte[totalDataSize];

        Log.d(TAG, "in readMessages() totalDataSize:"+totalDataSize);
        int currentDataSize = 0, bytesRead;
        int singleReadSize = SINGLE_READ_SIZE;
        while(currentDataSize < totalDataSize) {
            if((totalDataSize - currentDataSize) < SINGLE_READ_SIZE)
                singleReadSize = totalDataSize - currentDataSize;
            bytesRead = socketInputStream.read(messagesRead, currentDataSize, singleReadSize);
            if(bytesRead != -1)
                currentDataSize += bytesRead;
            Log.d(TAG, "in readMessages():" + bytesRead + "currentDataSize:" + currentDataSize);
        }
    }

    // This method returns TRUE if all the data has ben received and it is ready to be sent to the server
    private boolean connectAndReadFromRaspberry(String remoteDeviceMacAddress) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null)
            Log.d(TAG, "bluetoothAdapter is NULL");
        else {
            BluetoothDevice remoteDevice = bluetoothAdapter.getRemoteDevice(remoteDeviceMacAddress);
            try {
                bluetoothAdapter.cancelDiscovery();
                int attempt = 1;
                boolean connected = false;
                while(attempt <= MAX_ATTEMPTS && !connected) {
                    BluetoothSocket socket = (BluetoothSocket) BluetoothDevice.class.getMethod(
                            "createInsecureRfcommSocket", int.class).invoke(remoteDevice, 1);
                    if(socket == null) Log.d(TAG, "socket is NULL");
                    try {
                        Log.d(TAG, "Socket connecting attempt:" + attempt);
                        socket.connect();
                        connected = true;
                        InputStream socketInputStream = socket.getInputStream();
                        readMessages(socketInputStream);
                        return true;
                    } catch (IOException e) {
                        if(attempt < MAX_ATTEMPTS)
                            SystemClock.sleep(BLUETOOTH_MSECONDS_SLEEP);
                        Log.d(TAG, "Error with the socket or input stream read!");
                        e.printStackTrace();
                        attempt++;
                    } finally {
                        try {
                            socket.close();
                            Log.d(TAG, "Socket closed.");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private void connectAndSendToServer() {}
}
