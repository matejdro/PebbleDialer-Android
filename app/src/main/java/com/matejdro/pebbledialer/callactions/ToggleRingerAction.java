package com.matejdro.pebbledialer.callactions;

import android.content.Context;
import android.media.AudioManager;

import com.matejdro.pebbledialer.modules.CallModule;

public class ToggleRingerAction extends CallAction
{
    public static final int TOGGLE_RINGER_ACTION_ID = 2;

    private int previousMuteMode = -1;

    public ToggleRingerAction(CallModule callModule)
    {
        super(callModule);
    }

    @Override
    public void executeAction()
    {
        if (getCallModule().getCallState() != CallModule.CallState.RINGING)
            return;

        AudioManager audioManager = (AudioManager) getCallModule().getService().getSystemService(Context.AUDIO_SERVICE);

        if (previousMuteMode == -1)
        {
            previousMuteMode = audioManager.getRingerMode();
            audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            getCallModule().setVibration(false);
        }
        else
        {
            audioManager.setRingerMode(previousMuteMode);
            previousMuteMode = -1;
            getCallModule().setVibration(true);
        }

        getCallModule().updatePebble();
    }

    @Override
    public void onCallEnd()
    {
        if (previousMuteMode != -1)
        {
            AudioManager audioManager = (AudioManager) getCallModule().getService().getSystemService(Context.AUDIO_SERVICE);
            audioManager.setRingerMode(previousMuteMode);

            previousMuteMode = -1;
            getCallModule().setVibration(true);
        }

    }

    @Override
    public int getIcon()
    {
        return previousMuteMode == -1 ? CallAction.ICON_BUTTON_SPEKAER_ON : CallAction.ICON_BUTTON_SPEAKER_OFF;
    }

    public static ToggleRingerAction get(CallModule callModule)
    {
        return (ToggleRingerAction) callModule.getCallAction(TOGGLE_RINGER_ACTION_ID);
    }
}
