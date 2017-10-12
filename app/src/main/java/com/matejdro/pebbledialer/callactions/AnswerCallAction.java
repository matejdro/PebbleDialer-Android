package com.matejdro.pebbledialer.callactions;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.os.Build;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;

import com.matejdro.pebbledialer.modules.CallModule;
import com.matejdro.pebbledialer.notifications.JellybeanNotificationListener;

import java.io.IOException;
import java.util.List;

import timber.log.Timber;

public class AnswerCallAction extends CallAction
{
    public static final int ANSWER_ACTION_ID = 0;

    private PendingIntent notificationAnswerIntent;

    public AnswerCallAction(CallModule callModule)
    {
        super(callModule);
    }

    public void registerNotificationAnswerIntent(PendingIntent notificationAnswerIntent)
    {
        this.notificationAnswerIntent = notificationAnswerIntent;
    }

    @Override
    public void executeAction()
    {
        if (getCallModule().getCallState() != CallModule.CallState.RINGING)
            return;

        if (getCallModule().getService().getGlobalSettings().getBoolean("rootMode", false))
        {
            Timber.d("Answering using root method...");
            try {
                Runtime.getRuntime().exec(new String[] {"su", "-c", "input keyevent 5"});
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        if (notificationAnswerIntent != null)
        {
            Timber.d("Answering using notification method...");

            try {
                notificationAnswerIntent.send();
                return;
            } catch (PendingIntent.CanceledException e) {
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            answerNativelyOreo();
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            answerUsingMediaServer();
        }
        else
        {
            Timber.d("Answering using generic headset hook method...");
            Intent buttonUp = new Intent(Intent.ACTION_MEDIA_BUTTON);
            buttonUp.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HEADSETHOOK));
            getCallModule().getService().sendOrderedBroadcast(buttonUp, "android.permission.CALL_PRIVILEGED");
        }

    }

    @TargetApi(Build.VERSION_CODES.O)
    private void answerNativelyOreo() {
        TelecomManager telecomManager
                = (TelecomManager) getCallModule().getService().getSystemService(Context.TELECOM_SERVICE);

        Timber.d("Answering natively with Oreo.");

        try {
            telecomManager.acceptRingingCall();
        } catch (SecurityException e) {
            Timber.e("No accept call permission!");
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void answerUsingMediaServer()
    {
        Timber.d("Answering using media server method...");

        MediaSessionManager mediaSessionManager =  (MediaSessionManager) getCallModule().getService().getSystemService(Context.MEDIA_SESSION_SERVICE);

        try {
            List<MediaController> mediaControllerList = mediaSessionManager.getActiveSessions
                    (new ComponentName(getCallModule().getService(), JellybeanNotificationListener.class));

            for (MediaController m : mediaControllerList) {
                if ("com.android.server.telecom".equals(m.getPackageName())) {
                    Timber.d("Found telephony media controller!");
                    m.dispatchMediaButtonEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HEADSETHOOK));
                    break;
                }
            }
        } catch (SecurityException e) {
            Timber.e("Notification service not running!");
        }
    }

    @Override
    public void onCallEnd()
    {
        notificationAnswerIntent = null; //Reset intent (there will be new intent at next call)
    }

    @Override
    public int getIcon()
    {
        return CallAction.ICON_BUTTON_ANSWER;
    }

    public static AnswerCallAction get(CallModule callModule)
    {
        return (AnswerCallAction) callModule.getCallAction(ANSWER_ACTION_ID);
    }
}
