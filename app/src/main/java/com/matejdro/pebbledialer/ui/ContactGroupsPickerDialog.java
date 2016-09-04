package com.matejdro.pebbledialer.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.matejdro.pebblecommons.util.ListSerialization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ContactGroupsPickerDialog
{
    private Context context;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor preferencesEditor;
    private List<ContactGroup> contactGroups;
    private boolean[] checkedItems;

    public ContactGroupsPickerDialog(Context context, SharedPreferences sharedPreferences, SharedPreferences.Editor preferencesEditor)
    {
        this.context = context;
        this.sharedPreferences = sharedPreferences;
        this.preferencesEditor = preferencesEditor;
    }

    public void show()
    {
        List<Integer> existingSettings = ListSerialization.loadIntegerList(sharedPreferences, "displayedGroupsListNew");

        contactGroups = getAllContactGroups(context);
        checkedItems = new boolean[contactGroups.size()];


        for (int i = 0; i < contactGroups.size(); i++)
        {
            if (existingSettings.contains(contactGroups.get(i).getId()))
            {
                checkedItems[i] = true;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMultiChoiceItems(getNameArray(contactGroups), checkedItems, new DialogInterface.OnMultiChoiceClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {

                checkedItems[which] = isChecked;
            }
        }).setPositiveButton("OK", new OKButtonListener())
          .setNegativeButton("Cancel", null).setTitle("Displayed contact groups").show();

    }

    private class OKButtonListener implements DialogInterface.OnClickListener
    {

        @Override
        public void onClick(DialogInterface dialog, int which)
        {
            List<Integer> pickedGroups = new ArrayList<Integer>();
            int numChecked = 0;

            for (int i = 0; i < contactGroups.size(); i++)
            {
                if (checkedItems[i])
                {
                    numChecked++;
                    if (numChecked > 20)
                    {
                        Toast.makeText(context, "Sorry, you can only pick up to 20 groups.", Toast.LENGTH_SHORT).show();
                        break;
                    }

                    pickedGroups.add(contactGroups.get(i).getId());
                }
            }

            ListSerialization.saveIntegerList(preferencesEditor, pickedGroups, "displayedGroupsListNew");

        }
    }

    public static List<ContactGroup> getAllContactGroups(Context context)
    {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
        {
            ContactGroup dummyGroup = new ContactGroup(-1, "No permission");
            return Collections.singletonList(dummyGroup);
        }

        List<ContactGroup> groups = new ArrayList<ContactGroup>();

        ContentResolver resolver = context.getContentResolver();

        Cursor cursor = resolver.query(ContactsContract.Groups.CONTENT_SUMMARY_URI, new String[] { ContactsContract.Groups._ID, ContactsContract.Groups.TITLE, ContactsContract.Groups.ACCOUNT_NAME, ContactsContract.Groups.SUMMARY_COUNT}, ContactsContract.Groups.SUMMARY_COUNT + " > 0", null, null);

        while (cursor.moveToNext())
        {
            int id = cursor.getInt(0);
            String name = cursor.getString(1) + " (" + cursor.getString(2) + ")";
            groups.add(new ContactGroup(id, name));
        }

        cursor.close();

        return groups;
    }

    public static List<ContactGroup> getSpecificContactGroups(Context context, List<Integer> picks)
    {
        List<ContactGroup> groups = new ArrayList<ContactGroup>();
        if (picks.size() == 0)
            return groups;

        String picksCommaSeparated = "";
        for (Integer i : picks)
            picksCommaSeparated += i + ",";
        picksCommaSeparated = picksCommaSeparated.substring(0, picksCommaSeparated.length() - 1);


        ContentResolver resolver = context.getContentResolver();

        Cursor cursor = resolver.query(ContactsContract.Groups.CONTENT_SUMMARY_URI, new String[] { ContactsContract.Groups._ID, ContactsContract.Groups.TITLE, ContactsContract.Groups.SUMMARY_COUNT}, ContactsContract.Groups.SUMMARY_COUNT + " > 0 AND " + ContactsContract.Groups._ID + " IN (" + picksCommaSeparated + ")", null, null);

        if (cursor != null)
        {
            while (cursor.moveToNext())
            {
                int id = cursor.getInt(0);
                String name = cursor.getString(1);
                if (name == null)
                    continue;

                groups.add(new ContactGroup(id, name));
            }

            cursor.close();
        }


        return groups;
    }

    public static String[] getNameArray(List<ContactGroup> groups)
    {
        String[] names = new String[groups.size()];
        for (int i = 0; i < groups.size(); i++)
            names[i] = groups.get(i).getName();

        return names;
    }

    public static class ContactGroup
    {
        private int id;
        private String name;

        private ContactGroup(int id, String name)
        {
            this.id = id;
            this.name = name;
        }

        public int getId()
        {
            return id;
        }

        public String getName()
        {
            return name;
        }
    }

}
