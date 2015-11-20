package com.matejdro.pebbledialer.modules;

import android.app.PendingIntent;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;
import android.util.SparseArray;

import com.getpebble.android.kit.util.PebbleDictionary;
import com.matejdro.pebblecommons.pebble.CommModule;
import com.matejdro.pebblecommons.pebble.PebbleCommunication;
import com.matejdro.pebblecommons.pebble.PebbleImageToolkit;
import com.matejdro.pebblecommons.pebble.PebbleTalkerService;
import com.matejdro.pebblecommons.util.ContactUtils;
import com.matejdro.pebblecommons.util.TextUtil;
import com.matejdro.pebbledialer.callactions.AnswerCallAction;
import com.matejdro.pebbledialer.callactions.AnswerCallWithSpeakerAction;
import com.matejdro.pebbledialer.callactions.CallAction;
import com.matejdro.pebbledialer.callactions.DummyAction;
import com.matejdro.pebbledialer.callactions.EndCallAction;
import com.matejdro.pebbledialer.callactions.SMSReplyAction;
import com.matejdro.pebbledialer.callactions.ToggleMicrophoneAction;
import com.matejdro.pebbledialer.callactions.ToggleRingerAction;
import com.matejdro.pebbledialer.callactions.ToggleSpeakerAction;

import java.io.IOException;

import timber.log.Timber;

public class CallModule extends CommModule
{
    public static final String INTENT_CALL_STATUS = "CallStatus";
    public static final String INTENT_ACTION_FROM_NOTIFICATION = "ActionFromNotification";

    public static final int MAX_CALLER_IMAGE_WIDTH = 144 - 30;
    public static final int MAX_CALLER_IMAGE_HEIGHT = 168 - 16;
    public static final int CALLER_IMAGE_BYTES_PER_MESSAGE = 124 - 8;

    public static int MODULE_CALL = 1;

    private SparseArray<CallAction> actions = new SparseArray<CallAction>();

    private boolean updateRequired;
    private boolean callerNameUpdateRequired;
    private int callerImageNextByte = -1;

    private String number = "Outgoing Call";
    private String name = null;
    private String type = null;
    private Bitmap callerImage = null;
    private byte[] callerImageBytes;

    private CallState callState = CallState.NO_CALL;

    private boolean vibrating;
    private boolean closeAutomaticallyAfterThisCall = true;

    long callStartTime;

    public CallModule(PebbleTalkerService service)
    {
        super(service);

        service.registerIntent(INTENT_CALL_STATUS, this);
        service.registerIntent(INTENT_ACTION_FROM_NOTIFICATION, this);

        registerCallAction(new AnswerCallAction(this), AnswerCallAction.ANSWER_ACTION_ID);
        registerCallAction(new EndCallAction(this), EndCallAction.END_CALL_ACTION_ID);
        registerCallAction(new ToggleRingerAction(this), ToggleRingerAction.TOGGLE_RINGER_ACTION_ID);
        registerCallAction(new ToggleMicrophoneAction(this), ToggleMicrophoneAction.TOGGLE_MICROPHONE_ACTION_ID);
        registerCallAction(new SMSReplyAction(this), SMSReplyAction.SMS_REPLY_ACTION_ID);

        registerCallAction(new ToggleSpeakerAction(this), ToggleSpeakerAction.TOGGLE_SPEAKER_ACTION_ID);
        registerCallAction(new AnswerCallWithSpeakerAction(this), AnswerCallWithSpeakerAction.ANSWER_WITH_SPEAKER_ACTION_ID);

        registerCallAction(new DummyAction(this), DummyAction.DUMMY_ACTION_ID);

        updateRequired = false;
        callerNameUpdateRequired = false;
    }

    private void registerCallAction(CallAction action, int id)
    {
        actions.put(id, action);
    }

    public CallAction getCallAction(int id)
    {
        return actions.get(id);
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

            Timber.d("Got action from notification %d %s", actionType, actionIntent);

            if (actionType == 0)
                AnswerCallAction.get(this).registerNotificationAnswerIntent(actionIntent);
            else
                EndCallAction.get(this).registerNotificationEndCallIntent(actionIntent);
        }
    }

    private void intentDelivered(Intent intent)
    {
        if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL))
        {
            Timber.d("Outgoing intent");
            number =  intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
            updateNumberData();

            if (getService().getGlobalSettings().getBoolean("popupOnOutgoing", true))
                SystemModule.get(getService()).openApp();

            return;
        }

        Timber.d("phone state intent " + intent.getStringExtra(TelephonyManager.EXTRA_STATE));

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
        if (closeAutomaticallyAfterThisCall)
            SystemModule.get(getService()).startClosing();

        callState = CallState.NO_CALL;

        for (int i = 0; i < actions.size(); i++)
            actions.valueAt(i).onCallEnd();

        updateRequired = false;
        callerNameUpdateRequired = false;
        callerImageNextByte = -1;
    }

    private void callEstablished()
    {
        callStartTime = System.currentTimeMillis();
        callState = CallState.ESTABLISHED;

        updatePebble();

        for (int i = 0; i < actions.size(); i++)
            actions.valueAt(i).onPhoneOffhook();

        closeAutomaticallyAfterThisCall = true;
    }

    private void ringingStarted(Intent intent)
    {
        Timber.d("Ringing");

        callState = CallState.RINGING;
        number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
        vibrating = true;

        updateRequired = true;
        callerNameUpdateRequired = true;

        updateNumberData();
        updatePebble();

        if (getService().getGlobalSettings().getBoolean("popupOnIncoming", true))
            SystemModule.get(getService()).openApp();

        for (int i = 0; i < actions.size(); i++)
            actions.valueAt(i).onCallRinging();

        closeAutomaticallyAfterThisCall = true;
    }

    public void updatePebble()
    {
        updateRequired = true;

        PebbleCommunication communication = getService().getPebbleCommunication();
        communication.queueModulePriority(this);
        communication.sendNext();
    }

    public void setVibration(boolean vibrating)
    {
        this.vibrating = vibrating;
    }

    public void setCloseAutomaticallyAfterThisCall(boolean closeAutomaticallyAfterThisCall)
    {
        this.closeAutomaticallyAfterThisCall = closeAutomaticallyAfterThisCall;
    }

    public String getNumber()
    {
        return number;
    }

    private void updateNumberData()
    {
        Timber.d("Updating number...");

        callerImage = null;

        if (number == null)
        {
            name = null;
            number = "Unknown Number";
            return;
        }

        Cursor cursor = null;
        try
        {
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
            cursor = getService().getContentResolver().query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup.TYPE, ContactsContract.PhoneLookup.LABEL, ContactsContract.PhoneLookup.PHOTO_URI}, null, null, "contacts_view.last_time_contacted DESC");
        } catch (IllegalArgumentException e)
        {
            //This is sometimes thrown when number is in invalid format, so phone cannot recognize it.
        }

        name = null;
        if (cursor != null)
        {
            if (cursor.moveToNext())
            {
                name = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
                String label = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.LABEL));
                int typeId = cursor.getInt(cursor.getColumnIndex(ContactsContract.PhoneLookup.TYPE));

                type = ContactUtils.convertNumberType(typeId, label);
                if (type == null)
                    type = "Other";

                String photoUri = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_URI));
                if (photoUri != null && getService().getGlobalSettings().getBoolean("displayCallerImage", true))
                {
                    try

                    {
                        callerImage = MediaStore.Images.Media.getBitmap(getService().getContentResolver(), Uri.parse(photoUri));
                        callerImage = PebbleImageToolkit.resizePreservingRatio(callerImage, MAX_CALLER_IMAGE_WIDTH, MAX_CALLER_IMAGE_HEIGHT);
                    }
                    catch (IOException e)
                    {
                        Timber.w("Unable to load contact image!", e);
                    }
                }
            }

            cursor.close();
        }
    }

    private void processContactImage()
    {
        callerImage = PebbleImageToolkit.ditherToPebbleTimeColors(callerImage);
        callerImageBytes = PebbleImageToolkit.getIndexedPebbleImageBytes(callerImage);
    }

    @Override
    public boolean sendNextMessage()
    {
        if (updateRequired)
        {
            sendUpdatePacket();
            return true;
        }

        if (callerNameUpdateRequired)
        {
            sendCallerName();
            return true;
        }

        if (callerImageNextByte >= 0)
        {
            sendImagePacket();
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
            data.addString(2, TextUtil.prepareString(type, 30));
            callerNameUpdateRequired = true;
        }

        data.addString(3, TextUtil.prepareString(number, 30));

        byte[] parameters = new byte[6];
        parameters[0] = (byte) (callState == CallState.ESTABLISHED ? 1 : 0);
        parameters[1] = (byte) (name != null ? 1 : 0);
        parameters[5] = (byte) (vibrating ? 1 : 0);

        parameters[2] = (byte) getCallAction(getUserSelectedAction(getExtendedButtonId("Up"))).getIcon();
        parameters[3] = (byte) getCallAction(getUserSelectedAction(getExtendedButtonId("Select"))).getIcon();
        parameters[4] = (byte) getCallAction(getUserSelectedAction(getExtendedButtonId("Down"))).getIcon();

        data.addBytes(4, parameters);

        if (callState == CallState.ESTABLISHED)
            data.addUint16(5, (short) Math.min(65000, (System.currentTimeMillis() - callStartTime) / 1000));

        data.addUint8(999, (byte) 1);

        callerImageNextByte = -1;
        if (getService().getPebbleCommunication().getConnectedPebblePlatform().hasColors())
        {
            int imageSize = 0;

            if (callerImage != null)
            {
                processContactImage();
                imageSize = callerImageBytes.length;
            }

            data.addUint16(7, (short) imageSize);

            if (imageSize != 0)
                callerImageNextByte = 0;

            Timber.d("Image size: %d", imageSize);
        }

        getService().getPebbleCommunication().sendToPebble(data);
        Timber.d("Sent Call update packet...");

        updateRequired = false;
    }

    private void sendCallerName()
    {
        PebbleDictionary data = new PebbleDictionary();

        data.addUint8(0, (byte) 1);
        data.addUint8(1, (byte) 1);

        String name = TextUtil.prepareString(this.name, 100);
        data.addString(2, name == null ? "" : name);

        getService().getPebbleCommunication().sendToPebble(data);
        Timber.d("Sent Caller name packet...");

        callerNameUpdateRequired = false;
    }

    private void sendImagePacket()
    {
        int bytesToSend = Math.min(callerImageBytes.length - callerImageNextByte, CALLER_IMAGE_BYTES_PER_MESSAGE);
        byte[] bytes = new byte[bytesToSend];
        System.arraycopy(callerImageBytes, callerImageNextByte, bytes, 0, bytesToSend);

        PebbleDictionary data = new PebbleDictionary();

        data.addUint8(0, (byte) 1);
        data.addUint8(1, (byte) 2);

        data.addBytes(2, bytes);

        getService().getPebbleCommunication().sendToPebble(data);
        Timber.d("Sent image packet %d / %d", callerImageNextByte, callerImageBytes.length);

        callerNameUpdateRequired = false;

        callerImageNextByte += bytesToSend;
        if (callerImageNextByte >= callerImageBytes.length)
            callerImageNextByte = -1;
    }

    public void gotMessagePebbleAction(PebbleDictionary data)
    {
        int button = data.getUnsignedIntegerAsLong(2).intValue();
        String extendedButton = getExtendedButtonFromPebbleButton(button);
        int action = getUserSelectedAction(extendedButton);

        Timber.d("PebbleAction %d %s %d", button, extendedButton, action);

        getCallAction(action).executeAction();

    }


    @Override
    public void gotMessageFromPebble(PebbleDictionary message)
    {
        int id = message.getUnsignedIntegerAsLong(1).intValue();

        Timber.d("Incoming call packet %d", id);

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

    private int getUserSelectedAction(String button)
    {
        return Integer.parseInt(getService().getGlobalSettings().getString("callButton" + button, Integer.toString(getDefaultAction(button))));
    }

    private String getExtendedButtonId(String button)
    {
        switch (callState)
        {
            case RINGING:
                return "Ring" + button;
            case ESTABLISHED:
                return "Established" + button;
            default:
                return "Invalid";
        }
    }

    private static int getDefaultAction(String button)
    {
        switch (button)
        {
            case "RingUp":
                return  ToggleRingerAction.TOGGLE_RINGER_ACTION_ID;
            case "RingSelect":
                return  AnswerCallAction.ANSWER_ACTION_ID;
            case "RingDown":
                return  EndCallAction.END_CALL_ACTION_ID;
            case "RingUpHold":
                return  999;
            case "RingSelectHold":
                return  AnswerCallWithSpeakerAction.ANSWER_WITH_SPEAKER_ACTION_ID;
            case "RingDownHold":
                return  SMSReplyAction.SMS_REPLY_ACTION_ID;
            case "RingShake":
                return  999;
            case "EstablishedUp":
                return ToggleMicrophoneAction.TOGGLE_MICROPHONE_ACTION_ID;
            case "EstablishedSelect":
                return  ToggleSpeakerAction.TOGGLE_SPEAKER_ACTION_ID;
            case "EstablishedDown":
                return  EndCallAction.END_CALL_ACTION_ID;
            case "EstablishedUpHold":
                return 999;
            case "EstablishedSelectHold":
                return  999;
            case "EstablishedDownHold":
                return  999;
            case "EstablishedShake":
                return  999;
            default:
                return 999;
        }
    }

    private String getExtendedButtonFromPebbleButton(int pebbleButtonId)
    {
        String button;

        switch (pebbleButtonId)
        {
            case 0:
                button = "Up";
                break;
            case 1:
                button = "Select";
                break;
            case 2:
                button = "Down";
                break;
            case 3:
                button = "UpHold";
                break;
            case 4:
                button = "SelectHold";
                break;
            case 5:
                button = "DownHold";
                break;
            case 6:
                button = "Shake";
                break;
            default:
                button = "Invalid";
        }

        return getExtendedButtonId(button);
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
