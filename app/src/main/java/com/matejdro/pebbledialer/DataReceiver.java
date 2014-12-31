package com.matejdro.pebbledialer;

import static com.getpebble.android.kit.Constants.APP_UUID;
import static com.getpebble.android.kit.Constants.MSG_DATA;
import static com.getpebble.android.kit.Constants.TRANSACTION_ID;

import java.util.UUID;

import org.json.JSONException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;


public class DataReceiver extends BroadcastReceiver {

	public final static UUID dialerUUID = UUID.fromString("158A074D-85CE-43D2-AB7D-14416DDC1058");
	
	public void initDialer(Context context)
	{
		CallService call = CallService.instance;
		if (call != null)
		{
			call.updatePebble();
			return;
		}
		
		DialerService service = DialerService.instance;
		if (service == null)
		{
			context.startService(new Intent(context, DialerService.class));
		}
		else
		{
			service.initialize();
		}
	}
		
	public void inCallAction(Context context, PebbleDictionary input)
	{
		int buttonId = input.getUnsignedInteger(1).intValue() & 0xFF;		
		
		CallService service = CallService.instance;
		if (service != null)
		{
			service.pebbleAction(buttonId);
		}
	}
	
	public void dialerAction(int packet, Context context, PebbleDictionary input)
	{
		DialerService service = DialerService.instance;
		if (service == null)
		{
			//Something went horribly wrong here
			Log.wtf("PebbleDialer", "Got packet without running service! " + input.toJsonString());
		}
		else
		{
			service.gotPacket(packet, input);
		}
	}
	
	public void receiveData(final Context context, final int transactionId, final PebbleDictionary data)
	{
		PebbleKit.sendAckToPebble(context, transactionId);

		int id = data.getUnsignedInteger(0).intValue() & 0xFF;

		Log.d("PebbleDialer", "Got packet " + id);

		switch (id)
		{
		case 0:
			initDialer(context);
			break;
		case 7:
			inCallAction(context, data);
			break;
		default:
			dialerAction(id, context, data);
			break;
		}
	}
	
	

	public void onReceive(final Context context, final Intent intent) {
		final UUID receivedUuid = (UUID) intent.getSerializableExtra(APP_UUID);

		// Pebble-enabled apps are expected to be good citizens and only inspect broadcasts containing their UUID
		if (!receivedUuid.equals(dialerUUID)) {
			return;
		}

		final int transactionId = intent.getIntExtra(TRANSACTION_ID, -1);
		final String jsonData = intent.getStringExtra(MSG_DATA);
		if (jsonData == null || jsonData.isEmpty()) {
			return;
		}

		try {
			final PebbleDictionary data = PebbleDictionary.fromJson(jsonData);
			receiveData(context, transactionId, data);
		} catch (JSONException e) {
			e.printStackTrace();
			return;
		}
	}

}
