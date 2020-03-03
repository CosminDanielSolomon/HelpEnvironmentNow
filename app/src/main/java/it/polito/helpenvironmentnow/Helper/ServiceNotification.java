package it.polito.helpenvironmentnow.Helper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import it.polito.helpenvironmentnow.R;

public class ServiceNotification {

    public static void notifyForeground(Service service, final int SERVICE_ID, String title, String content) {
        Notification notification;
        String id = "it.polito.helpenvironmentnow.participative." + SERVICE_ID;
        String name = "Service" + SERVICE_ID;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) // check if Android version is 8 or higher
            notification = getNotificationWithChannel(service, id, name,
                    title, content); // foreground service notification for Android 8+
        else {
            notification = getNotification(service, id, title, content);
        }
        service.startForeground(SERVICE_ID, notification);
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
