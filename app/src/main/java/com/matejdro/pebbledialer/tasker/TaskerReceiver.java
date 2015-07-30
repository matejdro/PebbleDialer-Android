package com.matejdro.pebbledialer.tasker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.matejdro.pebblecommons.util.BundleSharedPreferences;

public class TaskerReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (intent == null)
            return;

        Bundle bundle = intent.getBundleExtra("com.twofortyfouram.locale.intent.extra.BUNDLE");
        if (bundle == null)
            return;

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();

        for (String key : bundle.keySet())
        {
            if (!key.startsWith("setting_"))
                continue;

            String actualSetting = key.substring(8);
            BundleSharedPreferences.writeIntoSharedPreferences(editor, actualSetting, bundle.get(key));
        }

        editor.apply();
    }
}
