package com.matejdro.pebbledialer;

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.preference.PreferenceManager;

import com.crashlytics.android.Crashlytics;
import com.matejdro.pebblecommons.PebbleCompanionApplication;
import com.matejdro.pebblecommons.pebble.PebbleTalkerService;
import com.matejdro.pebblecommons.util.LogWriter;

import java.util.UUID;

import timber.log.Timber;

/**
 * Created by Matej on 28.12.2014.
 */
public class PebbleDialerApplication extends PebbleCompanionApplication
{
    public static final UUID WATCHAPP_UUID = UUID.fromString("158A074D-85CE-43D2-AB7D-14416DDC1058");

    @Override
    public void onCreate() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        Timber.setAppTag("PebbleDialer");
        Timber.plant(new Timber.AppTaggedDebugTree());
        LogWriter.init(preferences, "PebbleDialer");

        boolean isDebuggable =  ( 0 != ( getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE ) );
        if (!isDebuggable)
            Crashlytics.start(this);

        super.onCreate();
    }

    @Override
    public UUID getPebbleAppUUID()
    {
        return WATCHAPP_UUID;
    }

    @Override
    public Class<? extends PebbleTalkerService> getTalkerServiceClass()
    {
        return DialerTalkerService.class;
    }
}
