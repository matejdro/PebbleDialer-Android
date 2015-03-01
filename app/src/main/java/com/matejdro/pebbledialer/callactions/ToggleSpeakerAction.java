package com.matejdro.pebbledialer.callactions;

import android.content.Context;
import android.media.AudioManager;

import com.matejdro.pebbledialer.modules.CallModule;

public class ToggleSpeakerAction extends CallAction
{
    public static final int TOGGLE_SPEAKER_ACTION_ID = 4;

    private boolean speakerphoneEnabled = false;

    public ToggleSpeakerAction(CallModule callModule)
    {
        super(callModule);
    }

    @Override
    public void executeAction()
    {
        if (getCallModule().getCallState() != CallModule.CallState.ESTABLISHED)
            return;

        AudioManager audioManager = (AudioManager) getCallModule().getService().getSystemService(Context.AUDIO_SERVICE);

        speakerphoneEnabled = !speakerphoneEnabled;
        audioManager.setSpeakerphoneOn(speakerphoneEnabled);

        getCallModule().updatePebble();
    }

    public boolean isSpeakerphoneEnabled()
    {
        return speakerphoneEnabled;
    }

    private void updateSpeakerphoneEnabled()
    {
        AudioManager audioManager = (AudioManager) getCallModule().getService().getSystemService(Context.AUDIO_SERVICE);
        speakerphoneEnabled = audioManager.isSpeakerphoneOn();
    }

    @Override
    public void onPhoneOffhook()
    {
        updateSpeakerphoneEnabled();
    }

    @Override
    public int getIcon()
    {
        return speakerphoneEnabled ? ICON_BUTTON_SPEKAER_ON : ICON_BUTTON_SPEAKER_OFF;
    }

    public static ToggleSpeakerAction get(CallModule callModule)
    {
        return (ToggleSpeakerAction) callModule.getCallAction(TOGGLE_SPEAKER_ACTION_ID);
    }
}
