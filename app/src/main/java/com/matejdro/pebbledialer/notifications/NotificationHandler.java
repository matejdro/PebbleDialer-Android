package com.matejdro.pebbledialer.notifications;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import com.matejdro.pebblecommons.pebble.PebbleTalkerService;
import com.matejdro.pebbledialer.modules.CallModule;

import java.lang.reflect.Field;

import com.matejdro.pebblecommons.log.Timber;

public class NotificationHandler {
	@TargetApi(Build.VERSION_CODES.KITKAT)
    public static void newNotification(Context context, String pkg, Notification notification)
	{
        if (pkg.contains("dialer") || pkg.contains("phone") || pkg.contains("call"))
        {
            Timber.d("Found potentially useful notification from " + pkg);

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            String answerText = preferences.getString("callNotificationAnswerButton", "Answer");
            String declineText = preferences.getString("callNotificationDeclineButton", "Decline");

            Notification.Action[] actions = getActionsField(notification);
            if (actions == null)
                return;

            PendingIntent answerIntent = null;
            PendingIntent declineIntent = null;

            for (Notification.Action action : actions)
            {
                Timber.d("Found action " + action.title);

                if (action.title.equals(answerText))
                    answerIntent = action.actionIntent;
                else if (action.title.equals(declineText))
                    declineIntent = action.actionIntent;
            }

            if (answerIntent != null)
            {
                Intent intent = new Intent(context, PebbleTalkerService.class);
                intent.setAction(CallModule.INTENT_ACTION_FROM_NOTIFICATION);
                intent.putExtra("actionType", 0);
                intent.putExtra("action", answerIntent);
                context.startService(intent);
            }

            if (declineIntent != null)
            {
                Intent intent = new Intent(context, PebbleTalkerService.class);
                intent.setAction(CallModule.INTENT_ACTION_FROM_NOTIFICATION);
                intent.putExtra("actionType", 1);
                intent.putExtra("action", declineIntent);
                context.startService(intent);
            }
        }
	}

    /**
     * Get the actions array from a notification using reflection. Actions were present in
     * Jellybean notifications, but the field was private until KitKat.
     */
    public static Notification.Action[] getActionsField(Notification notif) {

        try {
            Field actionsField = Notification.class.getDeclaredField("actions");
            actionsField.setAccessible(true);

            Notification.Action[] actions = (Notification.Action[]) actionsField.get(notif);
            return actions;
        } catch (IllegalAccessException e) {
        } catch (NoSuchFieldException e) {
        } catch (IllegalAccessError e)
        {
            //Weird error that appears on some devices (Only Xiaomi reported so far) and apparently means that Notification.Action on these devices is different than usual Android.
            //Unsupported for now.
        }


        return null;
    }
}
