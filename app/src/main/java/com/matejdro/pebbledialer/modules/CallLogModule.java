package com.matejdro.pebbledialer.modules;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;

import com.android.internal.telephony.ITelephony;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.matejdro.pebbledialer.PebbleTalkerService;
import com.matejdro.pebbledialer.pebble.PebbleCommunication;
import com.matejdro.pebbledialer.util.ContactUtils;
import com.matejdro.pebbledialer.util.TextUtil;

import java.io.IOException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import timber.log.Timber;

public class CallLogModule extends CommModule
{
    public static int MODULE_CALL_LOG = 2;

    private List<String> names;
    private List<String> dates;
    private List<Integer> logTypes;
    private List<String> numberTypes;
    private List<String> numbers;
    private HashSet<String> numberSet;

    private int nextToSend = -1;
    private boolean openWindow = false;

    public CallLogModule(PebbleTalkerService service) {
        super(service);

        names = new ArrayList<String>();
        dates = new ArrayList<String>();
        logTypes = new ArrayList<Integer>();
        numberTypes = new ArrayList<String>();
        numbers = new ArrayList<String>();
        numberSet = new HashSet<String>();
    }

    public void beginSending()
    {
        refreshCallLog();

        openWindow = true;
        queueSendEntries(0);
    }

    public void queueSendEntries(int offset)
    {
        nextToSend = offset;
        PebbleCommunication communication = getService().getPebbleCommunication();
        communication.queueModulePriority(this);
        communication.sendNext();
    }

	public void sendEntriesPacket(int offset)
	{
        if (logTypes.size() <= offset)
            return;

		PebbleDictionary data = new PebbleDictionary();

		data.addUint8(0, (byte) 2);
        data.addUint8(1, (byte) 0);
        data.addUint16(2, (short) offset);
		data.addUint16(3, (short) names.size());

		data.addUint8(4, logTypes.get(offset).byteValue());
		data.addString(6, dates.get(offset));

		String name = names.get(offset);
		if (name == null)
			name = TextUtil.prepareString(numbers.get(offset));

		data.addString(5, name);

		String numType = numberTypes.get(offset);
		if (numType == null)
			numType = "";
		data.addString(7, numType);

        if (openWindow)
            data.addUint8(999, (byte) 1);

        getService().getPebbleCommunication().sendToPebble(data);
	}

    @Override
    public boolean sendNextMessage() {
        if (nextToSend != -1)
        {
            sendEntriesPacket(nextToSend);

            nextToSend = -1;
            openWindow = false;

            return true;
        }

        return false;
    }

    private void gotMessageEntryPicked(PebbleDictionary message)
    {
        int index = message.getInteger(2).intValue();
        int mode = message.getUnsignedIntegerAsLong(3).intValue();
        Timber.d("Picked " + index + " " + mode);
		if (mode == 0)
        {
            if (numbers.size() > index)
                ContactUtils.call(numbers.get(index), getService());
        }
        else
        {
            int contactId = getContactId(numbers.get(index));
            NumberPickerModule.get(getService()).showNumberPicker(contactId);
        }
    }


    @Override
    public void gotMessageFromPebble(PebbleDictionary message) {
        int id = message.getUnsignedIntegerAsLong(1).intValue();
        switch (id)
        {
            case 0: //Request call log entry
                int offset = message.getUnsignedIntegerAsLong(2).intValue();
                Timber.d("off " + offset);
                queueSendEntries(offset);
                break;
            case 1:
                gotMessageEntryPicked(message);
                break;
        }
    }

    private void refreshCallLog()
    {
        names.clear();
        dates.clear();
        logTypes.clear();
        numberTypes.clear();
        numbers.clear();
        numberSet.clear();

        ContentResolver resolver = getService().getContentResolver();
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
        DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(getService());
        DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(getService());
        Date dateO = new Date(date);

        String dateS = dateFormat.format(dateO) + " " + timeFormat.format(dateO);

        return TextUtil.prepareString(dateS);
    }

    private int getContactId(String number)
    {
        if (number == null)
            return -1;

        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        Cursor cursor = getService().getContentResolver().query(uri, new String[]{ContactsContract.PhoneLookup._ID},null,  null, ContactsContract.PhoneLookup._ID + " LIMIT 1");
        int id = -1;

        if (cursor.moveToNext())
        {
            id = cursor.getInt(cursor.getColumnIndex(ContactsContract.PhoneLookup._ID));
        }

        return id;
    }


    public static CallLogModule get(PebbleTalkerService service)
    {
        return (CallLogModule) service.getModule(MODULE_CALL_LOG);
    }
}
