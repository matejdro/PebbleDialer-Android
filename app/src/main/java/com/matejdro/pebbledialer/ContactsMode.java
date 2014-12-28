package com.matejdro.pebbledialer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.util.Log;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

public class ContactsMode extends DialerMode {

	private List<Integer> filters;
	private List<String> names;
	private List<Integer> ids;
	private HashSet<Integer> idSet;

	private int group = -1;;

	public static final String queries[] = {"[^GHIghi4JKLjkl6MNOmno6PQRSpqrs7TUVtuv8wWXYZxyz9]", "[GHIghi4JKLjkl6MNOmno6 ]", "[PQRSŠpqrsš7TUVtuv8wWXYZŽxyzž9 ]"};

	private boolean filterMode;

	public ContactsMode(DialerService service, String contactGroup) {
		super(service);
		if (contactGroup != null)
		{
			this.group = getGroupId(contactGroup);
			Log.d("PebbleDialer", "Group " + group);

		}


		names = new ArrayList<String>();
		idSet = new HashSet<Integer>();
		ids = new ArrayList<Integer>();
		filters = new ArrayList<Integer>();

		filterMode = contactGroup == null || !service.preferences.getBoolean("skipGroupFiltering", false);

	}

	@Override
	public void dataReceived(int packetId, PebbleDictionary data) {
		switch (packetId)
		{
		case 3:
			filterMode = true;
			int offset = data.getUnsignedInteger(1).intValue();
			sendContacts(offset);
			break;
		case 4:
			filterMode = true;
			int button = data.getUnsignedInteger(1).intValue();
			filterContacts(button);
			break;
		case 5:
			if (filterMode)
			{
				filterMode = false;
				refreshContacts();
			}
			offset = data.getUnsignedInteger(1).intValue();
			sendContacts(offset);
			break;
		case 6:
			offset = data.getInteger(1).intValue();
			contactPicked(offset);
			break;
		}
	}

	@Override
	public void start() {
		refreshContacts();
		sendContacts(0);
	}

	private void contactPicked(int index)
	{
		service.mode = new NumberPickerMode(this, ids.get(index));
		service.mode.start();
		//Log.d("PebbleDialer", "Picked contact " + index + " (" + names.get(index) + " - " + ids.get(index) + ")");
	}

	private String buildSelection()
	{
		if (filters.size() == 0)
			return "1";
		String selectionHalf = "";
		for (int i = 0; i < filters.size(); i++)
			selectionHalf = selectionHalf.concat(queries[filters.get(i)]);

		String selection = ContactsContract.Contacts.DISPLAY_NAME + " GLOB \"" + selectionHalf + "*\" OR " + ContactsContract.Contacts.DISPLAY_NAME + " GLOB \"* " + selectionHalf + "*\"";
		return selection;
	}

	public List<String> getContacts()
	{
		return names;
	}

	public void sendContacts(int offset)
	{
		PebbleDictionary data = new PebbleDictionary();

		data.addUint8(0, (byte) 2);

		data.addUint16(1, offset);
		data.addUint16(2, names.size());

		for (int i = 0; i < 3; i++)
		{
			int listPos = offset + i;
			if (listPos >= names.size())
				break;

			data.addString(i + 3, names.get(i + offset));

		}

		Log.d("PebbleDialer", "send " + offset);

		PebbleKit.sendDataToPebble(service, DataReceiver.dialerUUID, data);
	}

	public void refreshContacts()
	{
		ContentResolver resolver = service.getContentResolver();

		String selection = "( " + buildSelection() + " )";
		String sortOrder = ContactsContract.Contacts.DISPLAY_NAME + " ASC";

		Uri uri;
		String idIndex;
		if (group >= 0)
		{
			uri = Data.CONTENT_URI;
			selection += " AND " + ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID + " = " + group; 
			idIndex = "contact_id";
		}
		else
		{
			selection += " AND " + ContactsContract.Contacts.HAS_PHONE_NUMBER + " = 1";
			uri = ContactsContract.Contacts.CONTENT_URI;
			sortOrder += " LIMIT " + (filterMode ? "6" : "2000");
			idIndex = ContactsContract.Contacts._ID;
		}
		

		Cursor cursor = resolver.query(uri, null, selection, null, sortOrder);

		names.clear();
		ids.clear();
		idSet.clear();

		while (cursor.moveToNext())
		{
			String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
			int id = cursor.getInt(cursor.getColumnIndex(idIndex));

			if (idSet.contains(id))
				continue;
			
			names.add(PebbleUtil.prepareString(name));

			ids.add(id);
			idSet.add(id);
			
			if (ids.size() > (filterMode ? 6 : 2000))
				break;
		}		

		cursor.close();
	}

	public void filterContacts(int buttonId)
	{		
		filters.add(buttonId);

		refreshContacts();
		sendContacts(0);
	}
	
	private int getGroupId(String group)
	{		
		ContentResolver resolver = service.getContentResolver();
		
		Cursor cursor = resolver.query(ContactsContract.Groups.CONTENT_SUMMARY_URI, new String[] { ContactsContract.Groups._ID, ContactsContract.Groups.SUMMARY_COUNT}, ContactsContract.Groups.TITLE + " = ? AND " + ContactsContract.Groups.SUMMARY_COUNT + " > 0", new String[] { group }, null);
		
		if (!cursor.moveToNext())
			return -1;
		
		return cursor.getInt(0);
	}
}
