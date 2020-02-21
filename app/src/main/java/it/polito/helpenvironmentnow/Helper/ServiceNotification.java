package it.polito.helpenvironmentnow.Helper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import it.polito.helpenvironmentnow.MainActivity;
import it.polito.helpenvironmentnow.R;

public class ServiceNotification {

    public static void notifyForeground(Service service, final int SERVICE_ID, String title, String content) {
        Notification notification;
        String id = "it.polito.helpenvironmentnow." + SERVICE_ID;
        String name = "Service" + SERVICE_ID;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) // check if Android version is 8 or higher
            notification = getNotificationWithChannel(service, id, name,
                    title, content); // foreground service notification for Android 8+
        else {
            notification = getNotification(service, id, title, content);
        }
        service.startForeground(SERVICE_ID, notification);
    }

    public static void showDisconnect(Service service) {
        String channelId = "it.polito.helpenvironmentnow.disconnected";
        String channelName = "disconnected";
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelName,
                    NotificationManager.IMPORTANCE_HIGH);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = service.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(service, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(service, 0, intent, 0);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(service, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("CONNECTION LOST!")
                .setContentText("Reconnect to the Pi, otherwise it will remain in an unstable state")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Reconnect to the Pi, otherwise it will remain in an unstable state"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(service);
        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(1, builder.build());
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static Notification getNotificationWithChannel(Context context, String id, String channelName,
                                                           String contentTitle, String contentText) {

        NotificationChannel chan = new NotificationChannel(id, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        return getNotification(context, id, contentTitle, contentText);
    }

    private static Notification getNotification(Context context, String channelId, String contentTitle,
                                                String contentText) {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, channelId);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();

        return notification;
    }
}
