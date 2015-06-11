package com.matejdro.pebbledialer.callactions;

import android.app.PendingIntent;
import android.content.Context;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.ITelephony;
import com.crashlytics.android.Crashlytics;
import com.matejdro.pebbledialer.modules.CallModule;
import com.matejdro.pebbledialer.modules.SMSReplyModule;

import java.io.IOException;
import java.lang.reflect.Method;

import timber.log.Timber;

public class SMSReplyAction extends CallAction
{
    public static final int SMS_REPLY_ACTION_ID = 6;

    public SMSReplyAction(CallModule callModule)
    {
        super(callModule);
    }

    @Override
    public void executeAction()
    {
        ToggleRingerAction toggleRingerAction = ToggleRingerAction.get(getCallModule());
        toggleRingerAction.mute();

        SMSReplyModule smsReplyModule = SMSReplyModule.get(getCallModule().getService());
        smsReplyModule.startSMSProcess(getCallModule().getNumber());

        getCallModule().setCloseAutomaticallyAfterThisCall(false);
    }

    @Override
    public void onCallEnd()
    {
    }

    @Override
    public int getIcon()
    {
        return CallAction.ICON_BUTTON_END_CALL;
    }

    public static SMSReplyAction get(CallModule callModule)
    {
        return (SMSReplyAction) callModule.getCallAction(SMS_REPLY_ACTION_ID);
    }
}
