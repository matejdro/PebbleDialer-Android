package com.matejdro.pebbledialer.modules;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;

import com.android.internal.telephony.ITelephony;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.matejdro.pebbledialer.PebbleTalkerService;
import com.matejdro.pebbledialer.pebble.PebbleCommunication;
import com.matejdro.pebbledialer.util.ContactUtils;
import com.matejdro.pebbledialer.util.TextUtil;

import java.io.IOException;
import java.lang.reflect.Method;

import timber.log.Timber;

public class CallModule extends CommModule
{
    public static final String INTENT_CALL_STATUS = "CallStatus";
    public static final String INTENT_ACTION_FROM_NOTIFICATION = "ActionFromNotification";

    public static int MODULE_CALL = 1;

    private boolean updateRequired;

    private String number = "Outgoing Call";
    private String name = null;
    private String type = null;

    private CallState callState = CallState.NO_CALL;

    private int previousMuteMode = -1;
    private boolean micMuted = false;
    private boolean speakerphoneEnabled = false;

    long callStart;

    private PendingIntent answerIntent;
    private PendingIntent declineIntent;

    public CallModule(PebbleTalkerService service)
    {
        super(service);

        service.registerIntent(INTENT_CALL_STATUS, this);
        service.registerIntent(INTENT_ACTION_FROM_NOTIFICATION, this);
    }

    @Override
    public void gotIntent(Intent intent) {
        if (intent.getAction().equals(INTENT_CALL_STATUS))
        {
            Intent callIntent = intent.getParcelableExtra("callIntent");
            intentDelivered(callIntent);
        }
        else if (intent.getAction().equals(INTENT_ACTION_FROM_NOTIFICATION))
        {
            int actionType = intent.getIntExtra("actionType", 0);
            PendingIntent actionIntent = intent.getParcelableExtra("action");

            Timber.d("Got action from notification " + actionType + " " + actionIntent);

            if (actionType == 0)
                answerIntent = actionIntent;
            else
                declineIntent = actionIntent;
        }
    }

    private void intentDelivered(Intent intent)
    {
        Timber.d("Intent " + intent.getAction());

        if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL))
        {
            Timber.d("Outgoing intent");
            number =  intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
            updateNumberData();

            return;
        }

        Timber.d("phone state intent " + intent.getStringExtra(TelephonyManager.EXTRA_STATE));

        if (previousMuteMode != -1)
        {
            AudioManager audioManager = (AudioManager) getService().getSystemService(Context.AUDIO_SERVICE);
            audioManager.setRingerMode(previousMuteMode);

            previousMuteMode = -1;
        }

        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);

        if (state.equals(TelephonyManager.EXTRA_STATE_RINGING))
        {
            ringingStarted(intent);
        }
        else if (state.equals(TelephonyManager.EXTRA_STATE_IDLE))
        {
            callEnded();
        }
        else if (state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK))
        {
            callEstablished();
        }
    }

    private void callEnded()
    {
        SystemModule.get(getService()).startClosing();
        callState = CallState.NO_CALL;
    }

    private void callEstablished()
    {
        callStart = System.currentTimeMillis();
        callState = CallState.ESTABLISHED;

        updatePebble();
        SystemModule.get(getService()).openApp();
    }

    private void ringingStarted(Intent intent)
    {
        callState = CallState.RINGING;
        number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
        Timber.d("Ringing " + number);

        updateNumberData();
        updatePebble();

        SystemModule.get(getService()).openApp();
    }

    private void updatePebble()
    {
        updateRequired = true;

        PebbleCommunication communication = getService().getPebbleCommunication();
        communication.queueModulePriority(this);
        communication.sendNext();
    }

    private void answerCall()
    {
        if (callState != CallState.RINGING)
            return;

        if (getService().getGlobalSettings().getBoolean("rootMode", false))
        {
            try {
                Runtime.getRuntime().exec(new String[] {"su", "-c", "input keyevent 5"});
            } catch (IOException e) {
                e.printStackTrace();
            }

            return;
        }

        if (answerIntent != null)
        {
            try {
                answerIntent.send();
            } catch (PendingIntent.CanceledException e) {
            }
            return;
        }

        Intent buttonUp = new Intent(Intent.ACTION_MEDIA_BUTTON);
        buttonUp.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HEADSETHOOK));
        getService().sendOrderedBroadcast(buttonUp, "android.permission.CALL_PRIVILEGED");
    }

    private void declineCall()
    {
        if (declineIntent != null)
        {
            try {
                declineIntent.send();
            } catch (PendingIntent.CanceledException e) {
            }
        }
        else
        {
            endCall();
        }

    }

    private void endCall()
    {
        if (getService().getGlobalSettings().getBoolean("rootMode", false))
        {
            try {
                Runtime.getRuntime().exec(new String[] {"su", "-c", "input keyevent 6"});
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else
        {
            //iTelephony
            Class<TelephonyManager> c = TelephonyManager.class;
            Method getITelephonyMethod = null;
            try {
                getITelephonyMethod = c.getDeclaredMethod("getITelephony",(Class[]) null);
                getITelephonyMethod.setAccessible(true);
                ITelephony iTelephony = (ITelephony) getITelephonyMethod.invoke(getService().getSystemService(Context.TELEPHONY_SERVICE), (Object[]) null);
                iTelephony.endCall();
            } catch (Exception e) {
                Timber.e(e, "endCallError");
            }
        }
    }

    private void toggleRingerMute()
    {
        if (callState != CallState.RINGING)
            return;

        AudioManager audioManager = (AudioManager) getService().getSystemService(Context.AUDIO_SERVICE);

        if (previousMuteMode == -1)
        {
            previousMuteMode = audioManager.getRingerMode();
            audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
        }
        else
        {
            audioManager.setRingerMode(previousMuteMode);
            previousMuteMode = -1;
        }
    }

    private void toggleSpeakerphone() {
        if (callState != CallState.ESTABLISHED)
            return;

        AudioManager audioManager = (AudioManager) getService().getSystemService(Context.AUDIO_SERVICE);

        speakerphoneEnabled = !speakerphoneEnabled;
        audioManager.setSpeakerphoneOn(speakerphoneEnabled);
    }

    private void toggleMicrophoneMute()
    {
        if (callState != CallState.ESTABLISHED)
            return;
        micMuted = !micMuted;

        if (getService().getGlobalSettings().getBoolean("rootMode", false))
        {
            try {
                Runtime.getRuntime().exec(new String[] {"su", "-c", "input keyevent 91"});
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else
        {
            AudioManager audioManager = (AudioManager) getService().getSystemService(Context.AUDIO_SERVICE);
            audioManager.setMicrophoneMute(micMuted);
        }
    }

    private void updateNumberData()
    {
        Timber.d("Updating number...");

        if (number == null)
        {
            name = null;
            number = "Unknown Number";
            return;
        }

        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        Cursor cursor = getService().getContentResolver().query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup.TYPE, ContactsContract.PhoneLookup.LABEL},null,  null, ContactsContract.PhoneLookup.DISPLAY_NAME + " LIMIT 1");

        name = null;
        if (cursor.moveToNext())
        {
            name = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
            String label = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.LABEL));
            int typeId = cursor.getInt(cursor.getColumnIndex(ContactsContract.PhoneLookup.TYPE));

            type = ContactUtils.convertNumberType(typeId, label);
            if (type == null)
                type = "Other";
        }
    }

    @Override
    public boolean sendNextMessage()
    {
        if (updateRequired)
        {
            sendUpdatePacket();
            return true;
        }

        return false;
    }

    private void sendUpdatePacket()
    {
        PebbleDictionary data = new PebbleDictionary();

        data.addUint8(0, (byte) 1);
        data.addUint8(1, (byte) 0);

        if (name != null)
        {
            data.addString(2, TextUtil.prepareString(name));
            data.addString(3, TextUtil.prepareString(type));
        }

        data.addString(4, TextUtil.prepareString(number));

        byte[] parameters = new byte[4];
        parameters[0] = (byte) (callState == CallState.ESTABLISHED ? 1 : 0);
        if (callState == CallState.ESTABLISHED)
            parameters[1] = (byte) (speakerphoneEnabled ? 1 : 0);
        else
            parameters[1] = (byte) (previousMuteMode > 0 ? 0 : 1);

        parameters[2] = (byte) (micMuted ? 0 : 1);
        parameters[3] = (byte) (name != null ? 1 : 0);

        data.addBytes(5, parameters);

        if (callState == CallState.ESTABLISHED)
            data.addUint16(6, (short) Math.min(65000, (System.currentTimeMillis() - callStart) / 1000));

        data.addUint8(999, (byte) 1);

        getService().getPebbleCommunication().sendToPebble(data);
        Timber.d("Sent Call update packet...");

        updateRequired = false;
    }

    public void gotMessagePebbleAction(PebbleDictionary data)
    {
        int button = data.getUnsignedIntegerAsLong(2).intValue();

        if (callState == CallState.ESTABLISHED)
        {
            switch (button)
            {
                case 0:
                    toggleMicrophoneMute();
                    break;
                case 1:
                    toggleSpeakerphone();
                    break;
                case 2:
                    endCall();
                    break;
            }
        }
        else
        {
            switch (button)
            {
                case 0:
                    toggleRingerMute();
                    break;
                case 1:
                    answerCall();
                    break;
                case 2:
                    declineCall();
                    break;
            }
        }
    }


    @Override
    public void gotMessageFromPebble(PebbleDictionary message)
    {
        int id = message.getUnsignedIntegerAsLong(1).intValue();

        Timber.d("Incoming call packet " + id);

        switch (id)
        {
            case 0: //Call action
                gotMessagePebbleAction(message);
                break;
        }
    }

    @Override
    public void pebbleAppOpened() {
        if (callState != CallState.NO_CALL)
        {
            updateRequired = true;

            PebbleCommunication communication = getService().getPebbleCommunication();
            communication.queueModulePriority(this);
        }
    }

    public CallState getCallState()
    {
        return callState;
    }

    public static CallModule get(PebbleTalkerService service)
    {
        return (CallModule) service.getModule(MODULE_CALL);
    }

    public static enum CallState
    {
        NO_CALL,
        RINGING,
        ESTABLISHED
    }
}
