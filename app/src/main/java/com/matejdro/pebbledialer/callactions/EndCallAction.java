package com.matejdro.pebbledialer.callactions;

import android.app.PendingIntent;
import android.content.Context;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.ITelephony;
import com.crashlytics.android.Crashlytics;
import com.matejdro.pebbledialer.modules.CallModule;

import java.io.IOException;
import java.lang.reflect.Method;

import timber.log.Timber;

public class EndCallAction extends CallAction
{
    public static final int END_CALL_ACTION_ID = 1;

    private PendingIntent notificationEndCallIntent;
    private static Method getITelephonyMethod;

    public EndCallAction(CallModule callModule)
    {
        super(callModule);

        try {
            getITelephonyMethod = TelephonyManager.class.getDeclaredMethod("getITelephony", (Class[]) null);
            getITelephonyMethod.setAccessible(true);
        } catch (Exception e) {
            Timber.e(e, "Error while acquiring iTelephony");
            Crashlytics.logException(e);
        }

    }

    public void registerNotificationEndCallIntent(PendingIntent notificationAnswerIntent)
    {
        this.notificationEndCallIntent = notificationAnswerIntent;
    }

    @Override
    public void executeAction()
    {
        getCallModule().setCloseAutomaticallyAfterThisCall(true);

        if (getCallModule().getService().getGlobalSettings().getBoolean("rootMode", false))
        {
            Timber.d("Ending call using root method...");
            try {
                Runtime.getRuntime().exec(new String[] {"su", "-c", "input keyevent 6"});
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (getCallModule().getCallState() == CallModule.CallState.RINGING && notificationEndCallIntent != null)
        {
            Timber.d("Ending call using notification method...");

            try {
                notificationEndCallIntent.send();
                return;
            } catch (PendingIntent.CanceledException e) {
            }
        }

        Timber.d("Ending call using generic iTelephony method...");
        try {
            ITelephony iTelephony = (ITelephony) getITelephonyMethod.invoke(getCallModule().getService().getSystemService(Context.TELEPHONY_SERVICE), (Object[]) null);
            iTelephony.endCall();
        } catch (Exception e) {
            Timber.e(e, "Error while invoking iTelephony.endCall()");
            Crashlytics.logException(e);
        }

    }

    @Override
    public void onCallEnd()
    {
        notificationEndCallIntent = null; //Reset intent (there will be new intent at next call)
    }

    @Override
    public int getIcon()
    {
        return CallAction.ICON_BUTTON_END_CALL;
    }

    public static EndCallAction get(CallModule callModule)
    {
        return (EndCallAction) callModule.getCallAction(END_CALL_ACTION_ID);
    }
}
