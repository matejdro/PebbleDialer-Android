package com.matejdro.pebbledialer;

import com.getpebble.android.kit.util.PebbleDictionary;

public abstract class DialerMode {
	protected DialerService service;
	
	public DialerMode(DialerService service)
	{
		this.service = service;
	}
	
	public abstract void start();
	public abstract void dataReceived(int packetId, PebbleDictionary data);
}
