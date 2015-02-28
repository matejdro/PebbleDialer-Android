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

public class DummyAction extends CallAction
{
    public static final int DUMMY_ACTION_ID = 999;


    public DummyAction(CallModule callModule)
    {
        super(callModule);

    }

    @Override
    public void executeAction()
    {
    }

    @Override
    public int getIcon()
    {
        return CallAction.ICON_BUTTON_ANSWER;
    }

    public static DummyAction get(CallModule callModule)
    {
        return (DummyAction) callModule.getCallAction(DUMMY_ACTION_ID);
    }
}
