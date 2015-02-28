package com.matejdro.pebbledialer.pebble;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;

import com.matejdro.pebbledialer.R;

public class WatchappHandler extends BroadcastReceiver {
    public static final int SUPPORTED_PROTOCOL = 5;
    public static final String INTENT_UPDATE_WATCHAPP = "com.matejdro.pebbledialer.UPDATE_WATCHAPP";

    public static final String WATCHAPP_URL = "https://dl.dropboxusercontent.com/u/6999250/dialer/beta/PebbleDialer.pbw";

    public static boolean isFirstRun(SharedPreferences settings)
	{
        boolean firstRun = settings.getBoolean("FirstRun", true);

        if (firstRun)
        {
            Editor editor = settings.edit();
            editor.putBoolean("FirstRun", false);
            editor.apply();
        }
        return firstRun;
	}

	public static void install(Context context, Editor editor)
	{
        Intent intent = new Intent(Intent.ACTION_VIEW);

        intent.setData(Uri.parse("pebble://appstore/532323bf60c773c1420000a8"));
        try
        {
            context.startActivity(intent);
        }
        catch (ActivityNotFoundException e)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(R.string.openingPebbleAppFailed).setNegativeButton("OK", null).show();
        }
	}

    public static void showUpdateNotification(Context context)
    {
        Notification.Builder mBuilder =
                new Notification.Builder(context).setSmallIcon(R.drawable.icon)
                        .setContentTitle("Pebble Dialer watchapp update").setContentText("Click on this notiifcation to update Pebble Dialer watchapp on Pebble")
                        .setContentIntent(PendingIntent.getBroadcast(context, 1, new Intent(INTENT_UPDATE_WATCHAPP), PendingIntent.FLAG_CANCEL_CURRENT));

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, mBuilder.getNotification());
    }

    public static void openUpdateWebpage(Context context)
    {
        Intent intent = new Intent(Intent.ACTION_VIEW);

        intent.setData(Uri.parse(WATCHAPP_URL));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try
        {
            context.startActivity(intent);
        }
        catch (ActivityNotFoundException e)
        {
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (INTENT_UPDATE_WATCHAPP.equals(intent.getAction()))
        {
            openUpdateWebpage(context);
        }
    }
}
