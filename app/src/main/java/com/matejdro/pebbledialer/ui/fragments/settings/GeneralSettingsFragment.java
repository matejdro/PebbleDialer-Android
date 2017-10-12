package com.matejdro.pebbledialer.ui.fragments.settings;

import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.text.format.DateFormat;
import android.widget.TimePicker;
import android.widget.Toast;

import com.matejdro.pebblecommons.util.RTLUtility;
import com.matejdro.pebbledialer.R;
import com.matejdro.pebbledialer.ui.ContactGroupsPickerDialog;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.TimeZone;

public class GeneralSettingsFragment extends CustomStoragePreferenceFragment
{
    private java.text.DateFormat dateFormat;
    private SharedPreferences sharedPreferences;

    @Override
    public void onCreatePreferencesEx(Bundle bundle, String s)
    {
        dateFormat = DateFormat.getTimeFormat(getContext());
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        sharedPreferences = getPreferenceManager().getSharedPreferences();

        addPreferencesFromResource(R.xml.settings_general);

        Preference rtlEnabled = findPreference("EnableRTL");
        rtlEnabled.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                RTLUtility.getInstance().setEnabled((Boolean) newValue);
                return true;
            }
        });

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

        setupQuietTimePreferences();
    }

    private void setupQuietTimePreferences()
    {
        final Preference quietFrom = findPreference("quietTimeStart");
        int startHour = sharedPreferences.getInt("quiteTimeStartHour", 0);
        int startMinute = sharedPreferences.getInt("quiteTimeStartMinute", 0);
        quietFrom.setSummary(formatTime(startHour, startMinute));
        quietFrom.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                int startHour = sharedPreferences.getInt("quiteTimeStartHour", 0);
                int startMinute = sharedPreferences.getInt("quiteTimeStartMinute", 0);

                TimePickerDialog dialog = new TimePickerDialog(getContext(), new TimePickerDialog.OnTimeSetListener() {

                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putInt("quiteTimeStartHour", hourOfDay);
                        editor.putInt("quiteTimeStartMinute", minute);
                        editor.apply();

                        quietFrom.setSummary(formatTime(hourOfDay, minute));
                    }
                }, startHour, startMinute, DateFormat.is24HourFormat(getContext()));
                dialog.show();

                return true;
            }
        });

        final Preference quietTo = findPreference("quietTimeEnd");
        int endHour = sharedPreferences.getInt("quiteTimeEndHour", 23);
        int endMinute = sharedPreferences.getInt("quiteTimeEndMinute", 59);
        quietTo.setSummary(formatTime(endHour, endMinute));
        quietTo.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                int startHour = sharedPreferences.getInt("quiteTimeEndHour", 0);
                int startMinute = sharedPreferences.getInt("quiteTimeEndMinute", 0);

                TimePickerDialog dialog = new TimePickerDialog(getContext(), new TimePickerDialog.OnTimeSetListener() {

                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putInt("quiteTimeEndHour", hourOfDay);
                        editor.putInt("quiteTimeEndMinute", minute);
                        editor.apply();

                        quietTo.setSummary(formatTime(hourOfDay, minute));
                    }
                }, startHour, startMinute, DateFormat.is24HourFormat(getContext()));
                dialog.show();

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

    public String formatTime(int hours, int minutes)
    {
        long datestampMs = (hours * 60 + minutes) * 60 * 1000;
        Date date = new Date(datestampMs);
        return dateFormat.format(date);
    }
}
