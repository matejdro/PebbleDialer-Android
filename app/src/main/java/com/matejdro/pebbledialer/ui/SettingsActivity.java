package com.matejdro.pebbledialer.ui;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.matejdro.pebbledialer.R;
import com.matejdro.pebbledialer.pebble.WatchappHandler;

public class SettingsActivity extends PreferenceActivity {
	private SharedPreferences settings;
	private SharedPreferences.Editor editor;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		editor = settings.edit();


        if (WatchappHandler.isFirstRun(settings))
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			
			builder.setMessage(R.string.pebbleAppInstallDialog).setNegativeButton(
					"No", null).setPositiveButton("Yes", new OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							WatchappHandler.install(SettingsActivity.this, editor);
						}
					}).show();
		}
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && isFirstLollipopRun(settings))
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.lollipopNotice).setPositiveButton("OK", null).show();
        }


        Preference displayedGroups = findPreference("displayedGroups");
		displayedGroups.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			
			@Override
			public boolean onPreferenceClick(Preference preference) {
				new ContactGroupsPickerDialog(SettingsActivity.this, settings, editor).show();
				return false;
			}
		});
		
		Preference help = findPreference("help");
		help.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse("https://docs.google.com/document/d/12EUvuYrydLCrfdAb6wLLvWn-v4C0HfI1f6WP2CT3bCE/pub"));
				startActivity(intent);
		        return false;
			}
		});
		
		
		CheckBoxPreference rootMethod = (CheckBoxPreference) findPreference("rootMode");
		rootMethod.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				boolean newChecked = (Boolean) newValue;
				
				if (newChecked == false)
					return true;
				
				if (!hasRoot())
				{
					Toast.makeText(SettingsActivity.this, "Sorry, you need ROOT for that method.", Toast.LENGTH_SHORT).show();
					return false;
				}
				
				return true;
			}
		});

        Preference notifierLicenseButton = findPreference("license");
        notifierLicenseButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {

                Intent intent = new Intent(SettingsActivity.this, LicenseActivity.class);
                startActivity(intent);
                return true;
            }
        });

        try
        {
            findPreference("version").setSummary( getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
        }
        catch (PackageManager.NameNotFoundException e)
        {

        }

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

        findPreference("enableServiceButton").setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference)
			{
				try
				{
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
                        startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
                    else
                        startActivity(new Intent("android.settings.ACTION_ACCESSIBILITY_SETTINGS"));
				}
				catch (ActivityNotFoundException e)
				{
					Toast.makeText(SettingsActivity.this, getString(R.string.open_settings_error), Toast.LENGTH_LONG).show();
				}
				return true;
            }
        });
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.settingsmenu, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    WatchappHandler.install(this, editor);
	    return true;
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

    public static boolean isFirstLollipopRun(SharedPreferences settings)
    {
        boolean firstRun = settings.getBoolean("FirstLollipopRun", true);

        if (firstRun)
        {
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("FirstLollipopRun", false);
            editor.apply();
        }
        return firstRun;
    }
}
