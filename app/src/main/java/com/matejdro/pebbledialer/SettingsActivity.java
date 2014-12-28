package com.matejdro.pebbledialer;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

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
		
		Preference displayedGroups = findPreference("displayedGroups");
		displayedGroups.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			
			@Override
			public boolean onPreferenceClick(Preference preference) {
				List<String> existingSettings = ListSerialization.loadList(settings, "displayedGroupsList");
				
				final String[] contactGroups = getAllContactGroups(SettingsActivity.this).toArray(new String[0]);
				boolean[] checkedItems = new boolean[contactGroups.length];
				
				for (int i = 0; i < contactGroups.length; i++)
				{
					if (existingSettings.contains(contactGroups[i]))
					{
						checkedItems[i] = true;
					}
				}
				
				final boolean[] newItemState = checkedItems.clone();
				
				AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
				builder.setMultiChoiceItems(contactGroups, checkedItems, new OnMultiChoiceClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which, boolean isChecked) {
						
						newItemState[which] = isChecked;
					}
				}).setPositiveButton("OK", new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						List<String> checkedGroups = new ArrayList<String>();
						int numChecked = 0;
						
						for (int i = 0; i < contactGroups.length; i++)
						{
							if (newItemState[i])
							{
								numChecked++;
								if (numChecked > 20)
								{
									Toast.makeText(SettingsActivity.this, "Sorry, you can only pick up to 20 groups.", Toast.LENGTH_SHORT).show();
									break;
								}

								checkedGroups.add(contactGroups[i]);
							}
						}
						
						ListSerialization.saveList(editor, checkedGroups, "displayedGroupsList");
					}
				}).setNegativeButton("Cancel", null).setTitle("Displayed contact groups").show();
				
				return false;
			}
		});
		
		Preference help = findPreference("help");
		help.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Intent intent = new Intent(SettingsActivity.this, HelpActivity.class);
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
	
	private static List<String> getAllContactGroups(Context context)
	{
		List<String> groups = new ArrayList<String>();
		
		ContentResolver resolver = context.getContentResolver();
		
		Cursor cursor = resolver.query(ContactsContract.Groups.CONTENT_SUMMARY_URI, new String[] { ContactsContract.Groups.TITLE, ContactsContract.Groups.SUMMARY_COUNT}, ContactsContract.Groups.SUMMARY_COUNT + " > 0", null, null);
		
		while (cursor.moveToNext())
		{
			groups.add(cursor.getString(0));
		}
		
		return groups;
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
