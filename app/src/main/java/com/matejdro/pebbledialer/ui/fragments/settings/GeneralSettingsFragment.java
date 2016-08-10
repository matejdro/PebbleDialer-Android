package com.matejdro.pebbledialer.ui.fragments.settings;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.widget.Toast;

import com.matejdro.pebblecommons.util.LogWriter;
import com.matejdro.pebbledialer.R;
import com.matejdro.pebbledialer.ui.ContactGroupsPickerDialog;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class GeneralSettingsFragment extends CustomStoragePreferenceFragment
{
    @Override
    public void onCreatePreferencesEx(Bundle bundle, String s)
    {
        addPreferencesFromResource(R.xml.settings_general);

        Preference displayedGroups = findPreference("displayedGroups");
        displayedGroups.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                new ContactGroupsPickerDialog(getContext(), getPreferenceManager().getSharedPreferences(), getPreferenceManager().getSharedPreferences().edit()).show();
                return false;
            }
        });

        Preference help = findPreference("help");
        help.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://docs.google.com/document/d/12EUvuYrydLCrfdAb6wLLvWn-v4C0HfI1f6WP2CT3bCE/pub"));
                startActivity(intent);
                return false;
            }
        });


        CheckBoxPreference rootMethod = (CheckBoxPreference) findPreference("rootMode");
        rootMethod.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean newChecked = (Boolean) newValue;

                if (!newChecked)
                    return true;

                if (!hasRoot())
                {
                    Toast.makeText(getContext(), "Sorry, you need ROOT for that method.", Toast.LENGTH_SHORT).show();
                    return false;
                }

                return true;
            }
        });

        findPreference("enableServiceButton").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                try
                {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
                        startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
                    else
                        startActivity(new Intent("android.settings.ACTION_ACCESSIBILITY_SETTINGS"));
                } catch (ActivityNotFoundException e)
                {
                    Toast.makeText(getContext(), getString(R.string.openSettingsError), Toast.LENGTH_LONG).show();
                }
                return true;
            }
        });
    }

    private static boolean hasRoot()
    {
        try {
            Process proces = Runtime.getRuntime().exec("su");

            DataOutputStream out = new DataOutputStream(proces.getOutputStream());
            BufferedReader in = new BufferedReader(new InputStreamReader(proces.getInputStream()));
            out.writeBytes("id\n");
            out.flush();

            String line = in.readLine();
            if (line == null) return false;

            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
