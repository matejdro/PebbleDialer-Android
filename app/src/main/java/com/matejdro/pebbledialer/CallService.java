package com.matejdro.pebbledialer;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import com.android.internal.telephony.ITelephony;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.matejdro.pebbledialer.util.ContactUtils;
import com.matejdro.pebbledialer.util.PebbleDeveloperConnection;
import com.matejdro.pebbledialer.util.TextUtil;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.UUID;

import timber.log.Timber;

public class CallService extends Service {
    public static final String INTENT_CALL_STATUS = "CallStatus";
    public static final String INTENT_ACTION_FROM_NOTIFICATION = "ActionFromNotification";

    private static final UUID SYSTEM_UUID = UUID.fromString("0a7575eb-e5b9-456b-8701-3eacb62d74f1");

    public static CallService instance;

	private SharedPreferences settings;
	private PebbleDeveloperConnection developerConnection;
    private UUID previousApp;

	private String number = "Outgoing Call";
	private String name = null;
	private String type = null;

	private boolean inCall = false;

	private int previousMuteMode = -1;
	private boolean micMuted = false;
	private boolean speakerphoneEnabled = false;

	long callStart;

    private PendingIntent answerIntent;
    private PendingIntent declineIntent;


	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

    @Override
    public void onCreate()
    {
        instance = this;

        super.onCreate();

        settings = PreferenceManager.getDefaultSharedPreferences(this);

        try
        {
            developerConnection = new PebbleDeveloperConnection();
            developerConnection.connectBlocking();
        } catch (URISyntaxException e)
        {
            e.printStackTrace();
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        instance = null;
        developerConnection.close();
    }


    @Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null && intent.getAction() != null)
		{
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
		

		return super.onStartCommand(intent, flags, startId);
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
			AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
			audioManager.setRingerMode(previousMuteMode);

			previousMuteMode = -1;
		}

		String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);

		if (state.equals(TelephonyManager.EXTRA_STATE_RINGING))
		{
			ringing(intent);
		}
		else if (state.equals(TelephonyManager.EXTRA_STATE_IDLE))
		{
			callEnded();
		}
		else if (state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK))
		{
			inCall();
		}
	}

	private void callEnded()
	{
		closePebbleApp();

		stopSelf();
	}

    private void openPebbleApp()
    {
        Timber.d("openApp");

        if (settings.getBoolean("closeToLastApp", false))
        {
            UUID currentApp = developerConnection.getCurrentRunningApp();

            if (currentApp != null && !(currentApp.getLeastSignificantBits() == 0 && currentApp.getMostSignificantBits() == 0) && !currentApp.equals(DataReceiver.dialerUUID) && !currentApp.equals(SYSTEM_UUID))
            {
                previousApp = currentApp;
            }

            System.out.println(currentApp);
        }

        PebbleKit.startAppOnPebble(this, DataReceiver.dialerUUID);
    }

    private void closePebbleApp()
    {
        Timber.d("closeApp");

        if (previousApp != null)
        {
            PebbleKit.startAppOnPebble(this, previousApp);
            previousApp = null;
        }
        else
        {
            PebbleKit.closeAppOnPebble(this, DataReceiver.dialerUUID);
        }
    }

	private void inCall()
	{
		callStart = System.currentTimeMillis();
		inCall = true;
		
		updatePebble();
        openPebbleApp();

		//toggleSpeakerphone();
	}

	private void ringing(Intent intent)
	{
		inCall = false;
		number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
		Timber.d("Ringing " + number);

		updateNumberData();
		updatePebble();
		
		PebbleKit.startAppOnPebble(this, DataReceiver.dialerUUID);
	}

	public void updatePebble()
	{
		// 0   = packet ID (5)
		// 1   = Contact name
		// 2   = Contact Type
		// 3   = Calling Number
		// 4.0 = In call / Incoming call
		// 4.1 = Is speaker muted
		// 4.2 = Is mic muted
		// 4.3 = Is contact known
		// 5 = Elapsed seconds
		
		PebbleDictionary data = new PebbleDictionary();

		data.addUint8(0, (byte) 5);

		if (name != null)
		{
			data.addString(1, TextUtil.prepareString(name));
			data.addString(2, TextUtil.prepareString(type));
		}
		
		data.addString(3, TextUtil.prepareString(number));

		byte[] parameters = new byte[4];
		parameters[0] = (byte) (inCall ? 1 : 0);
		if (inCall)
			parameters[1] = (byte) (speakerphoneEnabled ? 1 : 0);
		else
			parameters[1] = (byte) (previousMuteMode > 0 ? 0 : 1);

		parameters[2] = (byte) (micMuted ? 0 : 1);
		parameters[3] = (byte) (name != null ? 1 : 0);

		data.addBytes(4, parameters);

		if (inCall)
			data.addUint16(5, (short) Math.min(65000, (System.currentTimeMillis() - callStart) / 1000));
		
		PebbleKit.sendDataToPebble(this, DataReceiver.dialerUUID, data);
		Timber.d("UpdatePebble");
	}

	public void pebbleAction(int button)
	{
		if (inCall)
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

	public void answerCall()
	{
		if (inCall)
			return;
				
		if (settings.getBoolean("rootMode", false))
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
        this.sendOrderedBroadcast(buttonUp, "android.permission.CALL_PRIVILEGED");
	}

    public void declineCall()
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

	public void endCall()	
	{
		if (settings.getBoolean("rootMode", false))
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
				ITelephony iTelephony = (ITelephony) getITelephonyMethod.invoke(this.getSystemService(TELEPHONY_SERVICE), (Object[]) null);
				iTelephony.endCall();
			} catch (Exception e) {
				Timber.e(e, "endCallError");
			} 
		}
	}

	public void toggleRingerMute()
	{
		if (inCall)
			return;

		AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);

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

	public void toggleSpeakerphone() {
		if (!inCall)
			return;

		AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);

		speakerphoneEnabled = !speakerphoneEnabled;
		audioManager.setSpeakerphoneOn(speakerphoneEnabled);
	}

	public void toggleMicrophoneMute()
	{
		if (!inCall)
			return;
		micMuted = !micMuted;

		if (settings.getBoolean("rootMode", false))
		{
			try {
				Runtime.getRuntime().exec(new String[] {"su", "-c", "input keyevent 91"});
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else
		{
			AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
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
		
		Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
		Cursor cursor = this.getContentResolver().query(uri, new String[]{PhoneLookup.DISPLAY_NAME, PhoneLookup.TYPE, PhoneLookup.LABEL},null,  null, PhoneLookup.DISPLAY_NAME + " LIMIT 1");

		name = null;
		if (cursor.moveToNext())
		{
			name = cursor.getString(cursor.getColumnIndex(PhoneLookup.DISPLAY_NAME));
			String label = cursor.getString(cursor.getColumnIndex(PhoneLookup.LABEL));
			int typeId = cursor.getInt(cursor.getColumnIndex(PhoneLookup.TYPE));

			type = ContactUtils.convertNumberType(typeId, label);
			if (type == null)
				type = "Other";
		}
	}

}
