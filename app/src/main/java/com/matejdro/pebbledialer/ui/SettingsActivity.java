package com.matejdro.pebbledialer.ui;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import android.Manifest;
import android.annotation.TargetApi;
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
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.matejdro.pebblecommons.util.LogWriter;
import com.matejdro.pebbledialer.R;
import com.matejdro.pebbledialer.pebble.WatchappHandler;

import de.psdev.licensesdialog.LicensesDialog;

public class SettingsActivity extends PreferenceActivity {
	private SharedPreferences settings;
	private SharedPreferences.Editor editor;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

		initSuper();

		settings = getPreferenceManager().getSharedPreferences();
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
        notifierLicenseButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
		{

			@Override
			public boolean onPreferenceClick(Preference preference)
			{

				new LicensesDialog.Builder(SettingsActivity.this)
						.setNotices(R.raw.notices)
						.setIncludeOwnLicense(true)
						.build()
						.show();
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

        findPreference("enableServiceButton").setOnPreferenceClickListener(new OnPreferenceClickListener()
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
					Toast.makeText(SettingsActivity.this, getString(R.string.openSettingsError), Toast.LENGTH_LONG).show();
				}
				return true;
			}
		});

		findPreference("smsCannedResponsesDialog").setOnPreferenceClickListener(new OnPreferenceClickListener()
		{
			@Override
			public boolean onPreferenceClick(Preference preference)
			{
				StringListPopup popup = new StringListPopup(SettingsActivity.this, R.string.settingCannedResponses,  getPreferenceManager().getSharedPreferences(), "smsCannedResponses");
				popup.show();

				return true;
			}
		});

		findPreference("smsWritingPhrasesDialog").setOnPreferenceClickListener(new OnPreferenceClickListener()
		{
			@Override
			public boolean onPreferenceClick(Preference preference)
			{
				StringListPopup popup = new StringListPopup(SettingsActivity.this, R.string.settingWritingPhrases, getPreferenceManager().getSharedPreferences(), "smsWritingPhrases");
				popup.show();

				return true;
			}
		});

		findPreference(LogWriter.SETTING_ENABLE_LOG_WRITING).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
		{
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue)
			{
				if (((Boolean) newValue) && ContextCompat.checkSelfPermission(SettingsActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
				{
					ActivityCompat.requestPermissions(SettingsActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
					return false;
				}
				return true;
			}
		});

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
			checkPermissions();
	}

	@TargetApi(Build.VERSION_CODES.M)
	private void checkPermissions()
	{
		List<String> wantedPermissions = new ArrayList<>();

		if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED)
			wantedPermissions.add(Manifest.permission.RECORD_AUDIO);
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_DENIED)
			wantedPermissions.add(Manifest.permission.BLUETOOTH);
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_DENIED)
			wantedPermissions.add(Manifest.permission.READ_CONTACTS);
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_DENIED)
			wantedPermissions.add(Manifest.permission.CALL_PHONE);
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_DENIED)
			wantedPermissions.add(Manifest.permission.READ_PHONE_STATE);
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.PROCESS_OUTGOING_CALLS) == PackageManager.PERMISSION_DENIED)
			wantedPermissions.add(Manifest.permission.PROCESS_OUTGOING_CALLS);
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_DENIED)
			wantedPermissions.add(Manifest.permission.READ_CALL_LOG);
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_DENIED)
			wantedPermissions.add(Manifest.permission.READ_SMS);
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_DENIED)
			wantedPermissions.add(Manifest.permission.SEND_SMS);

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

		//Log writer needs to access external storage
		if (preferences.getBoolean(LogWriter.SETTING_ENABLE_LOG_WRITING, false) &&
				ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
		{
			wantedPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
		}


		if (!wantedPermissions.isEmpty())
			ActivityCompat.requestPermissions(this, wantedPermissions.toArray(new String[wantedPermissions.size()]), 0);
	}


	protected void initSuper()
	{

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
