package com.matejdro.pebbledialer.notifications;

import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import timber.log.Timber;

@TargetApi(value = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class JellybeanNotificationListener extends NotificationListenerService {
	private static JellybeanNotificationListener instance = null;

	@Override
	public void onDestroy() {
        Timber.d("Notification Listener stopped...");
		super.onDestroy();

		instance = null;
	}

	@Override
	public void onCreate() {
        Timber.d("Creating Notification Listener...");
        super.onCreate();

		instance = this;
	}

	public static boolean isActive()
	{
		return instance != null;
	}

	@TargetApi(value = Build.VERSION_CODES.LOLLIPOP)
	public static boolean isPhoneInDoNotInterrupt()
	{
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP || instance == null)
			return false;

		int interruptionFilter = instance.getCurrentInterruptionFilter();
		Timber.d("Interrupt filter: %d", interruptionFilter);
		return interruptionFilter != NotificationListenerService.INTERRUPTION_FILTER_ALL && interruptionFilter != 0;
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
