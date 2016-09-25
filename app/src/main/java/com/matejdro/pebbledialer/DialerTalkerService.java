package com.matejdro.pebbledialer;

import com.matejdro.pebblecommons.pebble.PebbleTalkerService;
import com.matejdro.pebbledialer.modules.CallLogModule;
import com.matejdro.pebbledialer.modules.CallModule;
import com.matejdro.pebbledialer.modules.ContactsModule;
import com.matejdro.pebbledialer.modules.NumberPickerModule;
import com.matejdro.pebbledialer.modules.SMSReplyModule;
import com.matejdro.pebbledialer.modules.SystemModule;

public class DialerTalkerService extends PebbleTalkerService
{
    @Override
    public void onCreate() {
        super.onCreate();

        // Disable until properly tested
        setEnableDeveloperConnectionRefreshing(false);
    }

    @Override
    public void registerModules()
    {
        addModule(new SystemModule(this), SystemModule.MODULE_SYSTEM);
        addModule(new CallModule(this), CallModule.MODULE_CALL);
        addModule(new CallLogModule(this), CallLogModule.MODULE_CALL_LOG);
        addModule(new NumberPickerModule(this), NumberPickerModule.MODULE_NUMBER_PICKER);
        addModule(new ContactsModule(this), ContactsModule.MODULE_CONTACTS);
        addModule(new SMSReplyModule(this), SMSReplyModule.MODULE_SMS_REPLIES);
    }
}
