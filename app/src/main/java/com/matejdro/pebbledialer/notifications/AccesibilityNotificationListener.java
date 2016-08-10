package com.matejdro.pebbledialer.notifications;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.os.Parcelable;
import android.view.accessibility.AccessibilityEvent;

import timber.log.Timber;

public class AccesibilityNotificationListener extends AccessibilityService {

	private static boolean active = false;

	@Override
	public void onCreate() {
		super.onCreate();
		active = true;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		active = false;
	}

	public static boolean isActive()
	{
		return active;
	}

	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) {
		Parcelable parcelable = event.getParcelableData();
		if (!(parcelable instanceof Notification))
			return;
		
		Notification notification = (Notification) parcelable;

        Timber.d("Got new accessibility notification");
        NotificationHandler.newNotification(this, event.getPackageName().toString(), notification);
	}

	@Override
	public void onInterrupt() {
	}
}
