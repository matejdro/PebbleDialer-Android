package com.matejdro.pebbledialer.notifications;

import android.annotation.TargetApi;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import timber.log.Timber;

@TargetApi(value = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class JellybeanNotificationListener extends NotificationListenerService {
	private static boolean active = false;

	@Override
	public void onDestroy() {
        Timber.d("Notification Listener stopped...");
		super.onDestroy();

		active = false;
	}

	@Override
	public void onCreate() {
        Timber.d("Creating Notification Listener...");
        super.onCreate();

		active = true;
	}

	public static boolean isActive()
	{
		return active;
	}

	@Override
	public void onNotificationPosted(final StatusBarNotification sbn) {
        Timber.d("Got new jellybean notification");
        NotificationHandler.newNotification(JellybeanNotificationListener.this, sbn.getPackageName(), sbn.getNotification());
	}

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
    }
}
