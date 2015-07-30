package com.matejdro.pebbledialer.tasker;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;

import com.matejdro.pebblecommons.util.BundleSharedPreferences;
import com.matejdro.pebbledialer.R;
import com.matejdro.pebbledialer.ui.SettingsActivity;

import java.lang.reflect.Field;

public class TaskerSettingsActivity extends SettingsActivity
{
    private Bundle storage;

    @Override
    protected void initSuper()
    {
        storage = new Bundle();

        loadIntent();

        replaceSharedPreferences();
    }

    protected void loadIntent() {
        Intent intent = getIntent();

        if (intent == null)
            return;

        Bundle bundle = intent.getBundleExtra("com.twofortyfouram.locale.intent.extra.BUNDLE");
        if (bundle == null)
            return;

        storage.putAll(bundle);
        for (String key : bundle.keySet())
        {
            if (!key.startsWith("setting_"))
            {
                storage.remove(key);
            }
        }
    }

    public void onBackPressed()
    {
        Intent intent = new Intent();

        String description = getString(R.string.tasker_change_settings);

        Bundle taskerBundle = new Bundle();
        taskerBundle.putAll(storage);

        intent.putExtra("com.twofortyfouram.locale.intent.extra.BLURB", description);
        intent.putExtra("com.twofortyfouram.locale.intent.extra.BUNDLE", taskerBundle);

        setResult(RESULT_OK, intent);

        super.onBackPressed();
    }

    private void replaceSharedPreferences()
    {
        PreferenceManager manager = getPreferenceManager();
        BundleSharedPreferences bundleSharedPreferences = new BundleSharedPreferences(manager.getSharedPreferences(), storage);

        try
        {
            Field field = PreferenceManager.class.getDeclaredField("mSharedPreferences");
            field.setAccessible(true);
            field.set(manager, bundleSharedPreferences);

            field = PreferenceManager.class.getDeclaredField("mEditor");
            field.setAccessible(true);
            field.set(manager, bundleSharedPreferences.edit());

        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

}
