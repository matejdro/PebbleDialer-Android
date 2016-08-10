package com.matejdro.pebbledialer.ui.fragments.settings;

import android.os.Bundle;
import android.support.v7.preference.Preference;

import com.matejdro.pebbledialer.R;
import com.matejdro.pebbledialer.ui.StringListPopup;

public class MessagingSettingsFragment extends CustomStoragePreferenceFragment
{
    @Override
    public void onCreatePreferencesEx(Bundle bundle, String s)
    {
        addPreferencesFromResource(R.xml.settings_sms);

        findPreference("smsCannedResponsesDialog").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                StringListPopup popup = new StringListPopup(getContext(), R.string.settingCannedResponses,  getPreferenceManager().getSharedPreferences(), "smsCannedResponses");
                popup.show();

                return true;
            }
        });

        findPreference("smsWritingPhrasesDialog").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                StringListPopup popup = new StringListPopup(getContext(), R.string.settingWritingPhrases, getPreferenceManager().getSharedPreferences(), "smsWritingPhrases");
                popup.show();

                return true;
            }
        });

    }
}
