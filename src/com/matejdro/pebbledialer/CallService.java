package com.matejdro.pebbledialer;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.UUID;

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
import android.util.Log;
import android.view.KeyEvent;

import com.android.internal.telephony.ITelephony;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

public class CallService extends Service {
	public static CallService instance;

	private SharedPreferences settings;
	
	private String number = "Outgoing Call";
	private String name = null;
	private String type = null;

	private boolean inCall = false;

	private int previousMuteMode = -1;
	private boolean micMuted = false;
	private boolean speakerphoneEnabled = false;

	long callStart;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onDestroy() {
		instance = null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		instance = this;
		settings = PreferenceManager.getDefaultSharedPreferences(this);

		if (intent != null)
		{
			Intent startingIntent = intent.getParcelableExtra("callIntent");
			if (startingIntent != null)
				intentDelivered(startingIntent);
		}
		

		return super.onStartCommand(intent, flags, startId);
	}

	private void intentDelivered(Intent intent)
	{
		if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL))
		{
			Log.d("PebbleCallService", "Outgoing intent");
			number =  intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
			updateNumberData();

			return;
		}
		
		Log.d("PebbleCallService", "phone state intent " + intent.getStringExtra(TelephonyManager.EXTRA_STATE));

		if (previousMuteMode != -1)
		{
			AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
			audioManager.setRingerMode(previousMuteMode);

			previousMuteMode = -1;
		}

		String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);

		System.out.println("state: " + state);
		
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
		//Launch glance
		if (settings.getBoolean("launchGlance", false))
			PebbleKit.startAppOnPebble(this, UUID.fromString("4B760064-1488-4044-967A-1B1D3AB30574"));
		else
			PebbleKit.closeAppOnPebble(this, DataReceiver.dialerUUID);

		stopSelf();
	}

	private void inCall()
	{
		callStart = System.currentTimeMillis();
		inCall = true;
		
		updatePebble();
		PebbleKit.startAppOnPebble(this, DataReceiver.dialerUUID);

		//toggleSpeakerphone();
	}

	private void ringing(Intent intent)
	{
		inCall = false;
		number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
		Log.d("PebbleCallService", "Ringing " + number);

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
			data.addString(1, PebbleUtil.prepareString(name));
			data.addString(2, PebbleUtil.prepareString(type));
		}
		
		data.addString(3, PebbleUtil.prepareString(number));

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
		Log.d("PebbleCallService", "UpdatePebble");
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
				endCall();
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
		}
		else
		{
			Intent buttonUp = new Intent(Intent.ACTION_MEDIA_BUTTON);              
			buttonUp.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HEADSETHOOK));
			this.sendOrderedBroadcast(buttonUp, "android.permission.CALL_PRIVILEGED");		

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
				Log.e(this.getClass().getName(), "endCallError", e);
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

			Log.d("PebbleCallService", "Name " + name + " " + type);
		}
	}

	public static void onCall(final Context context, final Intent intent)
	{
		Log.d("PebbleCallService", "onCall");
		CallService service = CallService.instance;

		if (service == null)
		{
			Intent startIntent = new Intent(context, CallService.class);
			startIntent.putExtra("callIntent", intent);

			context.startService(startIntent);
		}
		else
		{
			service.intentDelivered(intent);
		}
	}

}
