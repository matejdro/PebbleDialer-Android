package com.matejdro.pebbledialer.ui.fragments.settings;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.Preference;

import com.matejdro.pebblecommons.util.LogWriter;
import com.matejdro.pebbledialer.R;

import de.psdev.licensesdialog.LicensesDialog;

public class AboutSettingsFragment extends CustomStoragePreferenceFragment
{
    @Override
    public void onCreatePreferencesEx(Bundle bundle, String s)
    {
        addPreferencesFromResource(R.xml.settings_about);

        try
        {
            findPreference("version").setSummary( getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0).versionName);
        }
        catch (PackageManager.NameNotFoundException e)
        {

        }

        Preference notifierLicenseButton = findPreference("license");
        notifierLicenseButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {

            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                new LicensesDialog.Builder(getContext())
                        .setNotices(R.raw.notices)
                        .setIncludeOwnLicense(true)
                        .build()
                        .show();
                return true;
            }
        });


        findPreference("donateButton").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=5HV63YFE6SW44"));
                startActivity(intent);
                return true;
            }
        });

        findPreference(LogWriter.SETTING_ENABLE_LOG_WRITING).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                if (((Boolean) newValue) && ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
                {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
                    return false;
                }
                return true;
            }
        });
    }
}
