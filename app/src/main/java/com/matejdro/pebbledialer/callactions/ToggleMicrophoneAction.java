package com.matejdro.pebbledialer.callactions;

import android.content.Context;
import android.media.AudioManager;
import android.os.Build;

import com.matejdro.pebbledialer.modules.CallModule;

import java.io.IOException;

public class ToggleMicrophoneAction extends CallAction
{
    public static final int TOGGLE_MICROPHONE_ACTION_ID = 3;

    private boolean microphoneMuted = false;

    public ToggleMicrophoneAction(CallModule callModule)
    {
        super(callModule);
    }

    @Override
    public void executeAction()
    {
        if (getCallModule().getCallState() != CallModule.CallState.ESTABLISHED)
            return;

        microphoneMuted = !microphoneMuted;

        if (getCallModule().getService().getGlobalSettings().getBoolean("rootMode", false))
        {
            String muteCommand;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                muteCommand = "input keyevent 79";
            else
                muteCommand = "input keyevent 91";
            try {
                Runtime.getRuntime().exec(new String[] {"su", "-c", muteCommand});
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else
        {
            AudioManager audioManager = (AudioManager) getCallModule().getService().getSystemService(Context.AUDIO_SERVICE);
            audioManager.setMicrophoneMute(microphoneMuted);
        }

        getCallModule().updatePebble();
    }

    @Override
    public int getIcon()
    {
        return microphoneMuted ? CallAction.ICON_BUTTON_MIC_OFF : CallAction.ICON_BUTTON_MIC_ON;
    }

    public static ToggleMicrophoneAction get(CallModule callModule)
    {
        return (ToggleMicrophoneAction) callModule.getCallAction(TOGGLE_MICROPHONE_ACTION_ID);
    }
}
