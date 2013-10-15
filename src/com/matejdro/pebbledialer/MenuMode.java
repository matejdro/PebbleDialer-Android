package com.matejdro.pebbledialer;

import java.util.List;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

public class MenuMode extends DialerMode {

	public List<String> contactGroups;

	public MenuMode(DialerService service) {
		super(service);
	}

	private void sendContactGroups(int offset)
	{
		PebbleDictionary data = new PebbleDictionary();
		data.addUint8(0, (byte) 1);
		data.addUint8(1, (byte) offset);

		for (int i = 0; i < 3; i++)
		{
			int listPos = offset + i;
			if (listPos >= contactGroups.size())
				break;
			
			data.addString(i + 2, PebbleUtil.prepareString(contactGroups.get(listPos)));
		}
				
		PebbleKit.sendDataToPebble(service, DataReceiver.dialerUUID, data);
	}
	
	@Override
	public void dataReceived(int packetId, PebbleDictionary data) {
		switch (packetId)
		{
		case 1:
			int offset = data.getUnsignedInteger(1).intValue();
			sendContactGroups(offset);
			break;
		}
	}

	@Override
	public void start() {
		contactGroups = ListSerialization.loadList(service.preferences, "displayedGroupsList");
		int skipGroupFiltering = service.preferences.getBoolean("skipGroupFiltering", false) ? 1 : 0;
			
		PebbleDictionary initPacket = new PebbleDictionary();
		initPacket.addUint8(0, (byte) 0);
		initPacket.addUint8(1, (byte) contactGroups.size());
		initPacket.addUint8(2, (byte) skipGroupFiltering);
		PebbleKit.sendDataToPebble(service, DataReceiver.dialerUUID, initPacket);
	}

}
