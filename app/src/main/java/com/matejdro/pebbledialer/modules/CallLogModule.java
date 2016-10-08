package com.matejdro.pebbledialer.modules;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.support.v4.content.ContextCompat;
import android.telecom.Call;
import android.telephony.PhoneNumberUtils;

import com.getpebble.android.kit.util.PebbleDictionary;
import com.matejdro.pebblecommons.pebble.CommModule;
import com.matejdro.pebblecommons.pebble.PebbleCommunication;
import com.matejdro.pebblecommons.pebble.PebbleTalkerService;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;
import com.matejdro.pebblecommons.util.ContactUtils;
import com.matejdro.pebblecommons.util.TextUtil;

public class CallLogModule extends CommModule
{
    public static int MODULE_CALL_LOG = 2;

    private List<CallLogEntry> entries;

    private int nextToSend = -1;
    private boolean openWindow = false;

    public CallLogModule(PebbleTalkerService service) {
        super(service);

        entries = new ArrayList<CallLogEntry>();
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
        if (entries.size() <= offset)
            return;

        CallLogEntry callLogEntry = entries.get(offset);

		PebbleDictionary data = new PebbleDictionary();

		data.addUint8(0, (byte) 2);
        data.addUint8(1, (byte) 0);
        data.addUint16(2, (short) offset);
		data.addUint16(3, (short) entries.size());

		data.addUint8(4, (byte) callLogEntry.eventType);
		data.addString(6, callLogEntry.date);

		if (callLogEntry.name == null)
            callLogEntry.name = TextUtil.prepareString(callLogEntry.number);

		data.addString(5, callLogEntry.name);

		if (callLogEntry.numberType == null)
            callLogEntry.numberType = "";
		data.addString(7, callLogEntry.numberType);

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
        Timber.d("Picked %d %d", index, mode);

        if (entries.size() <= index)
        {
            Timber.d("Number out of bounds!");
            return;
        }

        CallLogEntry pickedEntry = entries.get(index);

        if (mode == 0)
        {
            ContactUtils.call(pickedEntry.number, getService());
        }
        else
        {
            Integer contactId = pickedEntry.contactId;
            if (contactId == null)
                contactId = getContactId(pickedEntry.number);

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
                Timber.d("off %d", offset);
                queueSendEntries(offset);
                break;
            case 1:
                gotMessageEntryPicked(message);
                break;
        }
    }

    private void refreshCallLog()
    {
        entries.clear();

        if (ContextCompat.checkSelfPermission(getService(), Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED)
            return;

        ContentResolver resolver = getService().getContentResolver();
        String sortOrder = CallLog.Calls.DEFAULT_SORT_ORDER + " LIMIT 100";
        Cursor cursor = resolver.query(CallLog.Calls.CONTENT_URI, null, null, null, sortOrder);

        if (cursor != null)
        {
            while (cursor.moveToNext())
            {
                String number = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));
                String name = TextUtil.prepareString(cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)), 16);
                int type = cursor.getInt(cursor.getColumnIndex(CallLog.Calls.TYPE));
                long date = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE));
                int numberType = cursor.getInt(cursor.getColumnIndex(CallLog.Calls.CACHED_NUMBER_TYPE));
                String customLabel = cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NUMBER_LABEL));

                if (number == null)
                    continue;

                String numberTypeText = TextUtil.prepareString(ContactUtils.convertNumberType(numberType, customLabel));

                CallLogEntry callLogEntry = new CallLogEntry(name, number, numberTypeText, getFormattedDate(date), type);
                if (!entries.contains(callLogEntry))
                {
                    if (name == null)
                        lookupContactInfo(callLogEntry);

                    entries.add(callLogEntry);
                }
            }

            cursor.close();
        }

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

        if (cursor != null)
        {
            if (cursor.moveToNext())
            {
                id = cursor.getInt(cursor.getColumnIndex(ContactsContract.PhoneLookup._ID));
            }

            cursor.close();
        }

        return id;
    }

    private void lookupContactInfo(CallLogEntry callLogEntry)
    {
        Cursor cursor = null;
        try
        {
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(callLogEntry.number));
            cursor = getService().getContentResolver().query(uri, new String[]{ContactsContract.PhoneLookup._ID, ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup.TYPE, ContactsContract.PhoneLookup.LABEL}, null, null, "contacts_view.last_time_contacted DESC");
        } catch (IllegalArgumentException e)
        {
            //This is sometimes thrown when number is in invalid format, so phone cannot recognize it.
        }
        catch (SecurityException e)
        {
            return;
        }

        if (cursor != null)
        {
            if (cursor.moveToNext())
            {
                callLogEntry.contactId = cursor.getInt(cursor.getColumnIndex(ContactsContract.PhoneLookup._ID));
                callLogEntry.name = TextUtil.prepareString(cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)));
                String label = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.LABEL));
                int typeId = cursor.getInt(cursor.getColumnIndex(ContactsContract.PhoneLookup.TYPE));

                callLogEntry.numberType = TextUtil.prepareString(ContactUtils.convertNumberType(typeId, label));
                if (callLogEntry.numberType == null)
                    callLogEntry.numberType = "Other";
            }

            cursor.close();
        }
    }


    private static class CallLogEntry
    {
        public Integer contactId;
        public String name;
        public String number;
        public String numberType;
        public String date;
        public int eventType;

        public CallLogEntry(String name, String number, String numberType, String date, int eventType)
        {
            this.name = name;
            this.number = PhoneNumberUtils.formatNumber(number);
            this.numberType = numberType;
            this.date = date;
            this.eventType = eventType;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CallLogEntry that = (CallLogEntry) o;

            return PhoneNumberUtils.compare(number, that.number);

        }

        @Override
        public int hashCode()
        {
            // Number is not used in hashcode, because it involves complicated comparing method that
            // cannot be boiled down to hash.
            // Because of this, we can't use hashcode for comparison.

            return 0;
        }
    }

    public static CallLogModule get(PebbleTalkerService service)
    {
        return (CallLogModule) service.getModule(MODULE_CALL_LOG);
    }
}
