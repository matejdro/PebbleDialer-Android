package com.matejdro.pebbledialer.modules;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.ContactsContract;

import com.getpebble.android.kit.util.PebbleDictionary;
import com.matejdro.pebblecommons.pebble.CommModule;
import com.matejdro.pebblecommons.pebble.PebbleTalkerService;
import com.matejdro.pebblecommons.pebble.PebbleCommunication;
import com.matejdro.pebblecommons.util.ContactUtils;
import com.matejdro.pebblecommons.util.TextUtil;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class NumberPickerModule extends CommModule
{
    public static int MODULE_NUMBER_PICKER = 4;

    private List<String> phoneNumbers;
    private List<String> phoneTitles;

    private int nextToSend = -1;
    private boolean openWindow = false;

    public NumberPickerModule(PebbleTalkerService service) {
        super(service);

        phoneNumbers = new ArrayList<String>(15);
        phoneTitles = new ArrayList<String>(15);


    }

    public void showNumberPicker(int contactId) {
        phoneNumbers.clear();
        phoneTitles.clear();

        ContentResolver resolver = getService().getContentResolver();
        Cursor cursor = resolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC LIMIT 2000");

        while (cursor.moveToNext())
        {
            int typeId = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
            String label = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL));
            String number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

            String type = ContactUtils.convertNumberType(typeId, label);

            phoneNumbers.add(number);
            phoneTitles.add(TextUtil.prepareString(type));
        }

        openWindow = true;
        sendNumbers(0);
    }


    public void sendNumbers(int offset)
    {
        PebbleDictionary data = new PebbleDictionary();

        data.addUint8(0, (byte) 4);
        data.addUint8(1, (byte) 0);

        data.addUint16(2, (short) offset);
        data.addUint16(3, (short) phoneTitles.size());

        for (int i = 0; i < 2; i++)
        {
            int listPos = offset + i;
            if (listPos >= phoneTitles.size())
                break;

            data.addString(i + 4, phoneTitles.get(i + offset));
            data.addString(i + 6, TextUtil.prepareString(phoneNumbers.get(i + offset)));
        }

        if (openWindow)
            data.addUint8(999, (byte) 1);

        Timber.d("sendNumbers " + offset);

        getService().getPebbleCommunication().sendToPebble(data);
    }


    public void queueSendEntries(int offset)
    {
        nextToSend = offset;
        PebbleCommunication communication = getService().getPebbleCommunication();
        communication.queueModulePriority(this);
        communication.sendNext();
    }

    @Override
    public boolean sendNextMessage() {
        if (nextToSend != -1)
        {
            sendNumbers(nextToSend);

            nextToSend = -1;
            openWindow = false;

            return true;
        }

        return false;
    }


    @Override
    public void gotMessageFromPebble(PebbleDictionary message) {
        int id = message.getUnsignedIntegerAsLong(1).intValue();
        switch (id)
        {
            case 0:
                int offset = message.getUnsignedIntegerAsLong(2).intValue();
                queueSendEntries(offset);
                break;
            case 1:
                offset = message.getInteger(2).intValue();
                if (offset >= phoneNumbers.size())
                    break;
                ContactUtils.call(phoneNumbers.get(offset), getService());
                break;
        }
    }

    public static NumberPickerModule get(PebbleTalkerService service)
    {
        return (NumberPickerModule) service.getModule(MODULE_NUMBER_PICKER);
    }
}
