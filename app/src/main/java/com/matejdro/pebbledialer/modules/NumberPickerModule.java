package com.matejdro.pebbledialer.modules;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.support.v4.content.ContextCompat;
import android.telephony.PhoneNumberUtils;

import com.getpebble.android.kit.util.PebbleDictionary;
import com.matejdro.pebblecommons.pebble.CommModule;
import com.matejdro.pebblecommons.pebble.PebbleTalkerService;
import com.matejdro.pebblecommons.pebble.PebbleCommunication;
import com.matejdro.pebblecommons.util.ContactUtils;
import com.matejdro.pebblecommons.util.TextUtil;
import com.matejdro.pebbledialer.callactions.SMSReplyAction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import timber.log.Timber;

public class NumberPickerModule extends CommModule
{
    public static int MODULE_NUMBER_PICKER = 4;

    private List<PebbleNumberEntry> phoneNumbers;

    private int nextToSend = -1;
    private boolean openWindow = false;

    public NumberPickerModule(PebbleTalkerService service) {
        super(service);

        phoneNumbers = new ArrayList<PebbleNumberEntry>();
    }

    public void showNumberPicker(int contactId) {
        phoneNumbers.clear();

        if (ContextCompat.checkSelfPermission(getService(), Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
        {
            ContentResolver resolver = getService().getContentResolver();
            Cursor cursor = resolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC LIMIT 2000");

            while (cursor.moveToNext())
            {
                int typeId = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                String label = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL));
                String number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

                String type = ContactUtils.convertNumberType(typeId, label);

                PebbleNumberEntry numberEntry = new PebbleNumberEntry(number, type);
                if (!phoneNumbers.contains(numberEntry))
                    phoneNumbers.add(numberEntry);
            }

            cursor.close();
        }
        else
        {
            phoneNumbers.add(new PebbleNumberEntry("No permission", "ERROR"));
        }


        int initialAmount = phoneNumbers.size();

        for (int i = 0; i < initialAmount; i++)
        {
            PebbleNumberEntry originalEntry = phoneNumbers.get(i);
            phoneNumbers.add(new PebbleNumberEntry(originalEntry.number, originalEntry.numberType, PebbleNumberEntry.NUMBER_ACTION_SMS));
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
        data.addUint16(3, (short) phoneNumbers.size());


        byte[] numberActions = new byte[2];
        for (int i = 0; i < 2; i++)
        {
            int listPos = offset + i;
            if (listPos >= phoneNumbers.size())
                break;

            PebbleNumberEntry numberEntry = phoneNumbers.get(listPos);

            data.addString(i + 4, TextUtil.prepareString(numberEntry.numberType));
            data.addString(i + 6, TextUtil.prepareString(numberEntry.number));
            numberActions[i] = (byte) numberEntry.numberAction;
        }

        data.addBytes(8, numberActions);

        if (openWindow)
            data.addUint8(999, (byte) 1);

        Timber.d("sendNumbers %d", offset);
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

                PebbleNumberEntry numberEntry = phoneNumbers.get(offset);
                if (numberEntry.numberAction == PebbleNumberEntry.NUMBER_ACTION_CALL)
                    ContactUtils.call(numberEntry.number, getService());
                else
                    SMSReplyModule.get(getService()).startSMSProcess(numberEntry.number);
                break;
        }
    }

    public static NumberPickerModule get(PebbleTalkerService service)
    {
        return (NumberPickerModule) service.getModule(MODULE_NUMBER_PICKER);
    }

    private static class PebbleNumberEntry
    {
        public static final int NUMBER_ACTION_CALL = 0;
        public static final int NUMBER_ACTION_SMS = 1;

        public String number;
        public String numberType;
        public int numberAction;

        public PebbleNumberEntry(String number, String numberType, int numberAction)
        {
            this.number = number;
            this.numberType = numberType;
            this.numberAction = numberAction;
        }

        public PebbleNumberEntry(String number, String numberType)
        {
            this.number = number;
            this.numberType = numberType;

            this.numberAction = NUMBER_ACTION_CALL;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PebbleNumberEntry that = (PebbleNumberEntry) o;

            if (numberAction != that.numberAction) return false;
            return PhoneNumberUtils.compare(number, that.number);
        }

        @Override
        public int hashCode()
        {
            // Number is not used in hashcode, because it involves complicated comparing method that
            // cannot be boiled down to hash.

            return numberAction;
        }
    }
}
