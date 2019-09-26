package it.polito.helpenvironmentnow;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
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
import java.lang.reflect.InvocationTargetException;

public class RfcommSendService extends IntentService {

    private String TAG = "RfcommSendService";
    private static final String CHANNEL_ID = "ForegroundServiceChannel";

    public RfcommSendService() {
        super("Rfcomm Send Service");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startMyOwnForeground();
        else
            startForeground(1, new Notification());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "onHandleIntent(...) called");
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null)
            Log.d(TAG, "bluetoothAdapter is NULL");
        else {
            String remoteDeviceMacAddress = intent.getStringExtra("remoteMacAddress");
            Log.d(TAG,"Mac address from intent: " + remoteDeviceMacAddress);
            BluetoothDevice remoteDevice = bluetoothAdapter.getRemoteDevice(remoteDeviceMacAddress);
            try {
                BluetoothSocket socket = (BluetoothSocket) BluetoothDevice.class.getMethod("createInsecureRfcommSocket", int.class).
                        invoke(remoteDevice, 1);
                if(socket == null)
                    Log.d(TAG, "socket is NULL");
                else {
                    bluetoothAdapter.cancelDiscovery();
                    int retry = 3, attempt = 1;
                    boolean connected = false;
                    while(attempt <= retry && !connected) {
                        try {
                            Log.d(TAG, "Socket connecting attempt:" + attempt);
                            socket.connect();
                            connected = true;
                            byte[] buffer = new byte[19];
                            int bytes; // = socket.getInputStream().read(buffer); // bytes returned from read
                            //int itemCount = Integer.parseInt(new String(buffer, 0, bytes));
                            //int currentCount = 0;
                            while ((bytes = socket.getInputStream().read(buffer)) != -1) {
                                String item = new String(buffer, 0, bytes);
                                Log.d(TAG, "Read from server: " + bytes + " bytes->" + item);
                                long time = Long.parseLong(item.substring(0, 10));
                                double temp = Double.parseDouble(item.substring(10, 15));
                                double hum = Double.parseDouble(item.substring(15, 19));
                                Log.d(TAG, "Time:" + time + " Temperature:" + temp + " Humidity:" + hum);
                                //currentCount++;
                            }
                        } catch (IOException e) {
                            SystemClock.sleep(500);
                            Log.d(TAG, "Error in creating socket!");
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
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startMyOwnForeground(){
        String NOTIFICATION_CHANNEL_ID = "com.example.simpleapp";
        String channelName = "My Background Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("App is running in background")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(1, notification);
    }
}
