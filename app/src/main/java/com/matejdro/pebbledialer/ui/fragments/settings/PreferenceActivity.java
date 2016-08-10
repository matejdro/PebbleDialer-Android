package com.matejdro.pebbledialer.ui.fragments.settings;

import android.support.annotation.XmlRes;

public interface PreferenceActivity
{
    public void switchToGenericPreferenceScreen(@XmlRes int xmlRes, String root);
}
