package com.matejdro.pebbledialer;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;

import com.getpebble.android.kit.util.PebbleDictionary;

public class DialerService extends Service {

	public static DialerService instance = null;	

	public DialerMode mode;
	public SharedPreferences preferences;

	@Override
	public void onDestroy() {
		instance = null;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		instance = this;

		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		
		initialize();

		return super.onStartCommand(intent, flags, startId);
	}

	public void initialize()
	{
		mode = new MenuMode(this);
		mode.start();
	}

	private void mainMenuSelected(int selection)
	{
		switch (selection)
		{
		case 0:
			mode = new CallLogMode(this);
			mode.start();
			break;
		case 1:
			mode = new ContactsMode(this, null);
			mode.start();
			break;
		default:
			String pickedGroup = ((MenuMode) mode).contactGroups.get(selection - 2);
			mode = new ContactsMode(this, pickedGroup);
			mode.start();
			break;
		}
	}

	public void gotPacket(int packetId, PebbleDictionary dictionary)
	{
		switch (packetId)
		{
		case 2:
			int selection = dictionary.getUnsignedInteger(1).intValue();
			mainMenuSelected(selection);
			break;
		default:
			mode.dataReceived(packetId, dictionary);
			break;
		}
	}
}
