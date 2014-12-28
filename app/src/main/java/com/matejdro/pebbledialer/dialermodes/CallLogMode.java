package com.matejdro.pebbledialer.dialermodes;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.provider.ContactsContract.PhoneLookup;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.matejdro.pebbledialer.util.ContactUtils;
import com.matejdro.pebbledialer.DataReceiver;
import com.matejdro.pebbledialer.DialerService;
import com.matejdro.pebbledialer.util.TextUtil;

public class CallLogMode extends DialerMode {	
	private List<String> names;
	private List<String> dates;
	private List<Integer> logTypes;
	private List<String> numberTypes;
	private List<String> numbers;

	private HashSet<String> numberSet;

	public CallLogMode(DialerService service) {
		super(service);		

		names = new ArrayList<String>();
		dates = new ArrayList<String>();
		logTypes = new ArrayList<Integer>();
		numberTypes = new ArrayList<String>();
		numbers = new ArrayList<String>();
		numberSet = new HashSet<String>();

	}

	@Override
	public void dataReceived(int packetId, PebbleDictionary data) {
		switch (packetId)
		{
		case 10:
			int offset = data.getUnsignedInteger(1).intValue();
			sendEntries(offset);
			break;
		case 11:
			offset = data.getInteger(1).intValue();
			int mode = data.getUnsignedInteger(2).intValue();

			entryPicked(offset, mode);
			break;
		}

	}

	@Override
	public void start() {
		refreshCallLog();
		sendEntries(0);
	}

	private void entryPicked(int index, int mode)
	{
		if (mode == 0)
			ContactUtils.call(numbers.get(index), service);
		else
		{
			int id = getContactId(numbers.get(index));
			if (id > 0)
			{
				service.mode = new NumberPickerMode(this, id);
				service.mode.start();
			}
		}
	}

	public void sendEntries(int offset)
	{
		PebbleDictionary data = new PebbleDictionary();

		data.addUint8(0, (byte) 4);
		data.addUint16(1, offset);
		data.addUint16(2, names.size());

		data.addUint8(3, logTypes.get(offset).byteValue());
		data.addString(5, dates.get(offset));

		String name = names.get(offset);
		if (name == null)
			name = TextUtil.prepareString(numbers.get(offset));
		
		data.addString(4, name);

		String numType = numberTypes.get(offset);
		if (numType == null) 
			numType = "";
		data.addString(6, numType);

		PebbleKit.sendDataToPebble(service, DataReceiver.dialerUUID, data);
	}

	private void refreshCallLog()
	{
		names.clear();
		dates.clear();
		logTypes.clear();
		numberTypes.clear();
		numbers.clear();
		numberSet.clear();

		ContentResolver resolver = service.getContentResolver();
		String sortOrder = CallLog.Calls.DEFAULT_SORT_ORDER + " LIMIT 1000";
		Cursor cursor = resolver.query(CallLog.Calls.CONTENT_URI, null, null, null, sortOrder);

		while (cursor.moveToNext())
		{
			String number = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));
			if (numberSet.contains(number))
				continue;

			String name = TextUtil.prepareString(cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)), 16);
			int type = cursor.getInt(cursor.getColumnIndex(CallLog.Calls.TYPE));
			long date = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE));
			int numberType = cursor.getInt(cursor.getColumnIndex(CallLog.Calls.CACHED_NUMBER_TYPE));
			String customLabel = cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NUMBER_LABEL));

			names.add(name);
			dates.add(getFormattedDate(date));
			logTypes.add(type);
			numbers.add(number);
			numberTypes.add(TextUtil.prepareString(ContactUtils.convertNumberType(numberType, customLabel)));
			numberSet.add(number);			
		}		

		cursor.close();
	}

	public String getFormattedDate(long date)
	{
		DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(service);
		DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(service);
		Date dateO = new Date(date);

		String dateS = dateFormat.format(dateO) + " " + timeFormat.format(dateO);

		return TextUtil.prepareString(dateS);
	}
	
	private int getContactId(String number)
	{
		if (number == null)
			return -1;
		
		Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
		Cursor cursor = service.getContentResolver().query(uri, new String[]{PhoneLookup._ID},null,  null, PhoneLookup._ID + " LIMIT 1");
		int id = -1;

		if (cursor.moveToNext())
		{
			id = cursor.getInt(cursor.getColumnIndex(PhoneLookup._ID));
		}
		
		return id;
	}
}
