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
