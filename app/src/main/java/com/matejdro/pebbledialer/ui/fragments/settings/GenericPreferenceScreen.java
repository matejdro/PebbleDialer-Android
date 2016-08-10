package com.matejdro.pebbledialer.ui.fragments.settings;

import android.os.Bundle;
import android.support.annotation.XmlRes;
import android.support.v7.preference.PreferenceFragmentCompat;

public class GenericPreferenceScreen extends CustomStoragePreferenceFragment
{
    private @XmlRes int preferenceXml;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        preferenceXml = getArguments().getInt("PreferencesXML", preferenceXml);

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreatePreferencesEx(Bundle bundle, String root)
    {
        setPreferencesFromResource(preferenceXml, root);
    }

    public static GenericPreferenceScreen newInstance(@XmlRes int preferenceXml, String root)
    {
        Bundle arguments = new Bundle();
        arguments.putInt("PreferencesXML", preferenceXml);
        arguments.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, root);

        GenericPreferenceScreen genericPreferenceScreen = new GenericPreferenceScreen();
        genericPreferenceScreen.setArguments(arguments);
        return genericPreferenceScreen;
    }
}
