package com.matejdro.pebbledialer.callactions;

import android.content.Context;
import android.media.AudioManager;
import android.os.Vibrator;

import com.matejdro.pebbledialer.modules.CallModule;

public class ToggleRingerAction extends CallAction
{
    public static final int TOGGLE_RINGER_ACTION_ID = 2;

    private boolean isRingerMuted = false;
    private int prevRingerMode = AudioManager.RINGER_MODE_NORMAL;

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

        if (!isRingerMuted)
        {
            isRingerMuted = true;
            prevRingerMode = audioManager.getRingerMode();

            audioManager.setStreamSolo(AudioManager.STREAM_MUSIC, true);
            audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            getCallModule().setVibration(false);
        }
        else
        {
            isRingerMuted = false;
            audioManager.setStreamSolo(AudioManager.STREAM_MUSIC, false);
            audioManager.setRingerMode(prevRingerMode);
            getCallModule().setVibration(true);
        }

        getCallModule().updatePebble();
    }

    public void mute()
    {
        if (!isRingerMuted)
            executeAction();
    }



    @Override
    public void onCallEnd()
    {
        if (isRingerMuted)
        {
            AudioManager audioManager = (AudioManager) getCallModule().getService().getSystemService(Context.AUDIO_SERVICE);
            isRingerMuted = false;
            audioManager.setStreamSolo(AudioManager.STREAM_MUSIC, false);
            audioManager.setRingerMode(prevRingerMode);
            getCallModule().setVibration(true);
        }

    }

    @Override
    public int getIcon()
    {
        return isRingerMuted ? CallAction.ICON_BUTTON_SPEAKER_OFF : CallAction.ICON_BUTTON_SPEKAER_ON;
    }

    public static ToggleRingerAction get(CallModule callModule)
    {
        return (ToggleRingerAction) callModule.getCallAction(TOGGLE_RINGER_ACTION_ID);
    }
}
