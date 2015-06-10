package com.matejdro.pebbledialer.callactions;

import android.app.PendingIntent;
import android.content.Intent;
import android.view.KeyEvent;

import com.matejdro.pebbledialer.modules.CallModule;

import java.io.IOException;

import com.matejdro.pebblecommons.log.Timber;

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

        Timber.d("Answering using generic headset hook method...");
        Intent buttonUp = new Intent(Intent.ACTION_MEDIA_BUTTON);
        buttonUp.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HEADSETHOOK));
        getCallModule().getService().sendOrderedBroadcast(buttonUp, "android.permission.CALL_PRIVILEGED");

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
