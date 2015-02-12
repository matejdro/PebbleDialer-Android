package com.matejdro.pebbledialer;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.preference.PreferenceManager;

import com.crashlytics.android.Crashlytics;
import com.matejdro.pebbledialer.util.LogWriter;

/**
 * Created by Matej on 28.12.2014.
 */
public class PebbleDialerApplication extends Application {

    @Override
    public void onCreate() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        LogWriter.init(preferences);

        boolean isDebuggable =  ( 0 != ( getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE ) );
        if (!isDebuggable)
            Crashlytics.start(this);

        super.onCreate();
    }

}
