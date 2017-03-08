package com.matejdro.pebbledialer.tasker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.annotation.XmlRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import com.matejdro.pebblecommons.util.BundleSharedPreferences;
import com.matejdro.pebblecommons.util.LogWriter;
import com.matejdro.pebbledialer.R;
import com.matejdro.pebbledialer.ui.PreferenceSource;
import com.matejdro.pebbledialer.ui.fragments.settings.GenericPreferenceScreen;
import com.matejdro.pebbledialer.ui.fragments.settings.PreferenceActivity;

public class TaskerSettingsActivity extends AppCompatActivity implements PreferenceActivity, PreferenceSource
{
    private Bundle settingStorageBundle;
    private static String KEY_STORAGE_BUNDLE = "StorageBundle";

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        if (savedInstanceState == null)
        {
            settingStorageBundle = new Bundle();
            loadTaskerIntent();
        }
        else
        {
            settingStorageBundle = savedInstanceState.getBundle(KEY_STORAGE_BUNDLE);
        }

        sharedPreferences = new BundleSharedPreferences(PreferenceManager.getDefaultSharedPreferences(this), settingStorageBundle);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasker);

        if (savedInstanceState == null)
        {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_frame, new TaskerMenuFragment())
                    .commit();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        outState.putBundle(KEY_STORAGE_BUNDLE, settingStorageBundle);
        super.onSaveInstanceState(outState);
    }

    public void swapFragment(Fragment fragment)
    {
        getSupportFragmentManager()
                .beginTransaction()
                .addToBackStack(null)
                .replace(R.id.content_frame, fragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();
    }

    public void switchToGenericPreferenceScreen(@XmlRes int xmlRes, String root)
    {
        Fragment fragment = GenericPreferenceScreen.newInstance(xmlRes, root);
        getSupportFragmentManager()
                .beginTransaction()
                .addToBackStack(null)
                .replace(R.id.content_frame, fragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();
    }

    protected void loadTaskerIntent() {
        Intent intent = getIntent();

        if (intent == null)
            return;

        Bundle bundle = intent.getBundleExtra("com.twofortyfouram.locale.intent.extra.BUNDLE");
        if (bundle == null)
            return;

        settingStorageBundle.putAll(bundle);
        for (String key : bundle.keySet())
        {
            if (!key.startsWith("setting_"))
            {
                settingStorageBundle.remove(key);
            }
        }
    }

    private void saveTaskerIntent()
    {
        Intent intent = new Intent();

        String description = getString(R.string.tasker_change_settings);

        Bundle taskerBundle = new Bundle();
        taskerBundle.putAll(settingStorageBundle);

        intent.putExtra("com.twofortyfouram.locale.intent.extra.BLURB", description);
        intent.putExtra("com.twofortyfouram.locale.intent.extra.BUNDLE", taskerBundle);
        setResult(RESULT_OK, intent);
    }

    @Override
    public void onBackPressed()
    {
        boolean exitingActivity = getFragmentManager().getBackStackEntryCount() == 0;
        if (exitingActivity)
            saveTaskerIntent();

        super.onBackPressed();
    }

    @Override
    public SharedPreferences getCustomPreferences()
    {
        return sharedPreferences;
    }
}
