package com.matejdro.pebbledialer.callactions;

import android.content.Intent;

import com.getpebble.android.kit.util.PebbleDictionary;
import com.matejdro.pebbledialer.PebbleTalkerService;
import com.matejdro.pebbledialer.modules.CallModule;

public abstract class CallAction
{
    public static final int ICON_BUTTON_ANSWER = 0;
    public static final int ICON_BUTTON_END_CALL = 1;
    public static final int ICON_BUTTON_MIC_ON = 2;
    public static final int ICON_BUTTON_MIC_OFF = 3;
    public static final int ICON_BUTTON_SPEKAER_ON = 4;
    public static final int ICON_BUTTON_SPEAKER_OFF = 5;

    private CallModule callModule;

    public CallAction(CallModule callModule)
    {
        this.callModule = callModule;
    }

    public CallModule getCallModule()
    {
        return callModule;
    }

    public void onPhoneOffhook()
    {

    }

    public void onCallRinging()
    {

    }

    public void onCallEnd()
    {

    }

    public abstract void executeAction();
    public abstract int getIcon();
}
