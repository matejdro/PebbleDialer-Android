package com.matejdro.pebbledialer.callactions;

import android.content.Context;
import android.media.AudioManager;

import com.matejdro.pebbledialer.modules.CallModule;

public class VolumeUpAction extends CallAction
{
    public static final int VOLUME_UP_ACTION_ID = 8;

    public VolumeUpAction(CallModule callModule)
    {
        super(callModule);
    }

    @Override
    public void executeAction()
    {
        if (getCallModule().getCallState() != CallModule.CallState.ESTABLISHED)
            return;

        AudioManager audioManager = (AudioManager) getCallModule().getService().getSystemService(Context.AUDIO_SERVICE);
        audioManager.adjustStreamVolume(AudioManager.STREAM_VOICE_CALL, AudioManager.ADJUST_RAISE, 0);
    }



    @Override
    public int getIcon()
    {
        return CallAction.ICON_BUTTON_VOLUME_UP;
    }
}
