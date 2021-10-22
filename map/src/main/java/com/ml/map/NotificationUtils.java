package com.ml.map;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.os.Build;

import androidx.annotation.RequiresApi;

public class NotificationUtils extends ContextWrapper {

    private NotificationManager mManager;
    public static final String ANDROID_CHANNEL_ID = "com.goblob.alerts.ANDROID";
    public static final String ANDROID_CHANNEL_NAME = "ANDROID CHANNEL";

    public static final String ANDROID_CHANNEL_LOCATION_ID = "com.goblob.locations.ANDROID";
    public static final String ANDROID_CHANNEL_LOCATION_NAME = "ANDROID LOCATION CHANNEL";

    public NotificationUtils(Context base) {
        super(base);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannels();
        }
    }

    public NotificationUtils(Context base, String type) {
        super(base);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannelsLocation();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void createChannels() {
        // create android channel
        NotificationChannel androidChannel = new NotificationChannel(ANDROID_CHANNEL_ID,
                ANDROID_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);

        // Sets whether notifications posted to this channel should display notification lights
        androidChannel.enableLights(true);
        // Sets whether notification posted to this channel should vibrate.
        androidChannel.enableVibration(true);
        // Sets the notification light color for notifications posted to this channel
        androidChannel.setLightColor(Color.GREEN);
        // Sets whether notifications posted to this channel appear on the lockscreen or not
        androidChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

        getManager().createNotificationChannel(androidChannel);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void createChannelsLocation() {
        // create android channel
        NotificationChannel androidChannel = new NotificationChannel(ANDROID_CHANNEL_LOCATION_ID,
                ANDROID_CHANNEL_LOCATION_NAME, NotificationManager.IMPORTANCE_LOW);

        // Sets whether notifications posted to this channel should display notification lights
        androidChannel.enableLights(true);
        // Sets whether notification posted to this channel should vibrate.
        androidChannel.enableVibration(true);
        // Sets the notification light color for notifications posted to this channel
        androidChannel.setLightColor(Color.GREEN);
        // Sets whether notifications posted to this channel appear on the lockscreen or not
        androidChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

        getManager().createNotificationChannel(androidChannel);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private Notification.Builder getAndroidChannelNotification() {
        return new Notification.Builder(getApplicationContext(), ANDROID_CHANNEL_ID)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setVisibility(Notification.VISIBILITY_PUBLIC) //This hides the notification from lock screen
                .setOngoing(true);
    }

    private Notification.Builder getAndroidNotification1() {
        return new Notification.Builder(getApplicationContext())
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setVisibility(Notification.VISIBILITY_PUBLIC) //This hides the notification from lock screen
                .setOngoing(true);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private Notification.Builder getAndroidChannelNotificationLocation() {
        return new Notification.Builder(getApplicationContext(), ANDROID_CHANNEL_LOCATION_ID)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setVisibility(Notification.VISIBILITY_SECRET) //This hides the notification from lock screen
                .setOngoing(true);
    }

    private Notification.Builder getAndroidNotification1Location() {
        return new Notification.Builder(getApplicationContext())
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setVisibility(Notification.VISIBILITY_SECRET) //This hides the notification from lock screen
                .setOngoing(true);
    }

    public Notification.Builder getAndroidNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return getAndroidChannelNotification();
        } else {
            return getAndroidNotification1();
        }
    }

    public Notification.Builder getAndroidNotificationLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return getAndroidChannelNotificationLocation();
        } else {
            return getAndroidNotification1Location();
        }
    }

    private NotificationManager getManager() {
        if (mManager == null) {
            mManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return mManager;
    }

    public void cancelAll() {
        getManager().cancelAll();
    }

    public void cancel(int notId) {
        getManager().cancel(notId);
    }

    public void notify(int notificationId, Notification build) {
        getManager().notify(notificationId, build);
    }
}