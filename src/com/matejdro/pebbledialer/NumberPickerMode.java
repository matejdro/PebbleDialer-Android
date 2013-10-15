package com.matejdro.pebbledialer;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

public class NumberPickerMode extends DialerMode {
	private DialerMode parent;

	private List<String> phoneNumbers;
	private List<String> phoneTitles;

	public NumberPickerMode(DialerMode parent, int contact) {
		super(parent.service);

		this.parent = parent;

		phoneNumbers = new ArrayList<String>(15);
		phoneTitles = new ArrayList<String>(15);

		ContentResolver resolver = service.getContentResolver();
		Cursor cursor = resolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contact, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC LIMIT 2000");

		while (cursor.moveToNext())
		{
			int typeId = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
			String label = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL));
			String number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

			String type = ContactUtils.convertNumberType(typeId, label);

			phoneNumbers.add(number);
			phoneTitles.add(PebbleUtil.prepareString(type));
		}

	}

	@Override
	public void start() {
		sendNumbers(0);
	}

	@Override
	public void dataReceived(int packetId, PebbleDictionary data) {
		switch (packetId)
		{
		case 8:
			int offset = data.getUnsignedInteger(1).intValue();
			sendNumbers(offset);
			break;
		case 9:
			offset = data.getInteger(1).intValue();
			ContactUtils.call(phoneNumbers.get(offset), parent.service);
			break;
		default:
			parent.service.mode = parent;
			parent.dataReceived(packetId, data);
		}
	}

	public void sendNumbers(int offset)
	{
		PebbleDictionary data = new PebbleDictionary();

		data.addUint8(0, (byte) 3);

		data.addUint16(1, offset);
		data.addUint16(2, phoneTitles.size());

		for (int i = 0; i < 2; i++)
		{
			int listPos = offset + i;
			if (listPos >= phoneTitles.size())
				break;

			data.addString(i + 3, phoneTitles.get(i + offset));
			data.addString(i + 5, PebbleUtil.prepareString(phoneNumbers.get(i + offset)));

		}

		Log.d("PebbleDialer", "sendNumbers " + offset);

		PebbleKit.sendDataToPebble(service, DataReceiver.dialerUUID, data);
	}

//	public void call(int entry)
//	{
//		String number = PhoneNumberUtils.stripSeparators(phoneNumbers.get(entry));
//		Log.d("PebbleDialer", "Calling " + number);
//
//		Intent startIntent = new Intent(service, CallService.class);
//		startIntent.putExtra("name", contactName);
//		startIntent.putExtra("number", phoneNumbers.get(entry));
//		startIntent.putExtra("type", phoneTitles.get(entry));
//
//		service.startService(startIntent);
//
//		Intent intent = new Intent(Intent.ACTION_CALL);
//		intent.setData(Uri.parse("tel:" + number));
//		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//		service.startActivity(intent);
//
//	}
}
