package com.matejdro.pebbledialer.ui.fragments.settings;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceScreen;

import com.matejdro.pebblecommons.util.BundleSharedPreferences;
import com.matejdro.pebbledialer.R;
import com.matejdro.pebbledialer.ui.MainActivity;
import com.matejdro.pebbledialer.ui.PreferenceSource;

import java.lang.reflect.Field;

public abstract class CustomStoragePreferenceFragment extends PreferenceFragmentCompat
{

    @Override
    public void onCreatePreferences(Bundle bundle, String s)
    {
        if (getActivity() instanceof PreferenceSource)
        {
            SharedPreferences sharedPreferences = ((PreferenceSource) getActivity()).getCustomPreferences();
            injectSharedPreferences(sharedPreferences);
        }

        onCreatePreferencesEx(bundle, s);
    }

    protected abstract void onCreatePreferencesEx(Bundle bundle, String s);

    @Override
    public void onNavigateToScreen(PreferenceScreen preferenceScreen)
    {
        ((PreferenceActivity) getActivity()).switchToGenericPreferenceScreen(R.xml.settings_callscreen, preferenceScreen.getKey());
    }


    @SuppressLint("CommitPrefEdits")
    private void injectSharedPreferences(SharedPreferences preferences)
    {
        PreferenceManager manager = getPreferenceManager();

        try
        {
            Field field = PreferenceManager.class.getDeclaredField("mSharedPreferences");
            field.setAccessible(true);
            field.set(manager, preferences);

            field = PreferenceManager.class.getDeclaredField("mEditor");
            field.setAccessible(true);
            field.set(manager, preferences.edit());

        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

}
