package com.matejdro.pebbledialer.callactions;

import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.view.KeyEvent;

import com.matejdro.pebbledialer.modules.CallModule;

import java.io.IOException;

import timber.log.Timber;

public class ToggleRingerAction extends CallAction
{
    public static final int TOGGLE_RINGER_ACTION_ID = 2;

    private boolean isMutedViaAudioManager = false;
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

        getCallModule().setVibration(false);

        if (!isMutedViaAudioManager)
        {
            if (getCallModule().getService().getGlobalSettings().getBoolean("rootMode", false))
            {
                Timber.d("Muting using root method...");
                try {
                    Runtime.getRuntime().exec(new String[] {"su", "-c", "input keyevent " + KeyEvent.KEYCODE_VOLUME_DOWN});
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            else if (canMuteRinger(getCallModule().getService()))
            {
                isMutedViaAudioManager = true;
                prevRingerMode = audioManager.getRingerMode();

                audioManager.setStreamSolo(AudioManager.STREAM_MUSIC, true);
                audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            }
        }
        else if (canMuteRinger(getCallModule().getService()))
        {
            isMutedViaAudioManager = false;
            audioManager.setStreamSolo(AudioManager.STREAM_MUSIC, false);
            audioManager.setRingerMode(prevRingerMode);
        }

        getCallModule().updatePebble();
    }

    public void mute()
    {
        if (!isMutedViaAudioManager)
            executeAction();
    }

    public static boolean canMuteRinger(Context context)
    {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return true;

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        return notificationManager.isNotificationPolicyAccessGranted();
    }

    @Override
    public void onCallEnd()
    {        if (isMutedViaAudioManager && canMuteRinger(getCallModule().getService()))
        {
            AudioManager audioManager = (AudioManager) getCallModule().getService().getSystemService(Context.AUDIO_SERVICE);
            isMutedViaAudioManager = false;
            audioManager.setStreamSolo(AudioManager.STREAM_MUSIC, false);
            audioManager.setRingerMode(prevRingerMode);
        }

        getCallModule().setVibration(true);
    }

    @Override
    public int getIcon()
    {
        return isMutedViaAudioManager ? CallAction.ICON_BUTTON_SPEAKER_OFF : CallAction.ICON_BUTTON_SPEKAER_ON;
    }

    public static ToggleRingerAction get(CallModule callModule)
    {
        return (ToggleRingerAction) callModule.getCallAction(TOGGLE_RINGER_ACTION_ID);
    }
}
