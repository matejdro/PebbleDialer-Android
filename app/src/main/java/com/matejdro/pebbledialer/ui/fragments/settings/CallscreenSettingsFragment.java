package com.matejdro.pebbledialer.ui.fragments.settings;

import android.os.Bundle;
import android.support.v7.preference.PreferenceScreen;

import com.matejdro.pebbledialer.R;
import com.matejdro.pebbledialer.ui.MainActivity;

public class CallscreenSettingsFragment extends CustomStoragePreferenceFragment
{
    @Override
    public void onCreatePreferencesEx(Bundle bundle, String s)
    {
        setPreferencesFromResource(R.xml.settings_callscreen, s);
    }
}
