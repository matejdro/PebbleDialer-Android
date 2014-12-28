package com.matejdro.pebbledialer.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;

import com.matejdro.pebbledialer.R;

public class WatchappHandler {

	public static boolean isFirstRun(SharedPreferences settings)
	{
        boolean firstRun = settings.getBoolean("FirstRun", true);

        if (firstRun)
        {
            Editor editor = settings.edit();
            editor.putBoolean("FirstRun", false);
            editor.apply();
        }
        return firstRun;
	}

	public static void install(Context context, Editor editor)
	{
        Intent intent = new Intent(Intent.ACTION_VIEW);

        intent.setData(Uri.parse("pebble://appstore/532323bf60c773c1420000a8"));
        try
        {
            context.startActivity(intent);
        }
        catch (ActivityNotFoundException e)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(R.string.openingPebbleAppFailed).setNegativeButton("OK", null).show();
        }
	}
}
