package com.matejdro.pebbledialer.ui;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.annotation.XmlRes;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.matejdro.pebblecommons.util.LogWriter;
import com.matejdro.pebbledialer.R;
import com.matejdro.pebbledialer.pebble.WatchappHandler;
import com.matejdro.pebbledialer.ui.fragments.settings.GenericPreferenceScreen;
import com.matejdro.pebbledialer.ui.fragments.HomeFragment;
import com.matejdro.pebbledialer.ui.fragments.settings.AboutSettingsFragment;
import com.matejdro.pebbledialer.ui.fragments.settings.CallscreenSettingsFragment;
import com.matejdro.pebbledialer.ui.fragments.settings.GeneralSettingsFragment;
import com.matejdro.pebbledialer.ui.fragments.settings.MessagingSettingsFragment;
import com.matejdro.pebbledialer.ui.fragments.settings.PreferenceActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, PreferenceActivity
{
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private Toolbar toolbar;

    private NavigationView navigationView;

    private SharedPreferences settings;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        navigationView = (NavigationView) findViewById(R.id.navigation_view);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open_drawer, R.string.close_drawer);
        drawerLayout.setDrawerListener(actionBarDrawerToggle);

        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(0);

        settings = PreferenceManager.getDefaultSharedPreferences(this);

        if (WatchappHandler.isFirstRun(settings))
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setMessage(R.string.pebbleAppInstallDialog).setNegativeButton(
                    "No", null).setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    WatchappHandler.openPebbleStore(MainActivity.this);
                }
            }).show();
        }
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        navigationView.setCheckedItem(R.id.home);
        onNavigationItemSelected(navigationView.getMenu().findItem(R.id.home));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            checkPermissions();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);
        actionBarDrawerToggle.syncState();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == R.id.pebble_store)
        {
            WatchappHandler.openPebbleStore(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item)
    {
        if (item.getItemId() == R.id.home)
        {
            switchToFragment(new HomeFragment());
        }
        else if (item.getItemId() == R.id.general_settings)
        {
            switchToFragment(new GeneralSettingsFragment());
        }
        else if (item.getItemId() == R.id.customize_callscreen)
        {
            switchToFragment(new CallscreenSettingsFragment());
        }
        else if (item.getItemId() == R.id.sms)
        {
            switchToFragment(new MessagingSettingsFragment());
        }
        else if (item.getItemId() == R.id.about)
        {
            switchToFragment(new AboutSettingsFragment());
        }

        drawerLayout.closeDrawers();
        return true;
    }

    private void switchToFragment(Fragment fragment)
    {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, fragment)
                .commit();
    }

    public void switchToGenericPreferenceScreen(@XmlRes int xmlRes, String root)
    {
        Fragment fragment = GenericPreferenceScreen.newInstance(xmlRes, root);
        getSupportFragmentManager()
                .beginTransaction()
                .addToBackStack(null)
                .replace(R.id.content_frame, fragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_DENIED)
                wantedPermissions.add(Manifest.permission.ANSWER_PHONE_CALLS);
        }

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
}
