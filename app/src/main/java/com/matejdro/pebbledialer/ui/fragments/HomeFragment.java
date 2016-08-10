package com.matejdro.pebbledialer.ui.fragments;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.matejdro.pebbledialer.R;
import com.matejdro.pebbledialer.notifications.AccesibilityNotificationListener;
import com.matejdro.pebbledialer.notifications.JellybeanNotificationListener;

public class HomeFragment extends Fragment
{
    private View notificationServiceWarningCard;

    @Override
    public void onResume()
    {
        super.onResume();

        boolean serviceActive = JellybeanNotificationListener.isActive() || AccesibilityNotificationListener.isActive();
        notificationServiceWarningCard.setVisibility(serviceActive ? View.GONE : View.VISIBLE);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        notificationServiceWarningCard = view.findViewById(R.id.tip_no_notification_service);
        notificationServiceWarningCard.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
                    startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
                else
                    startActivity(new Intent("android.settings.ACTION_ACCESSIBILITY_SETTINGS"));

                Toast.makeText(getContext(), R.string.notification_service_enabling_instructions, Toast.LENGTH_SHORT).show();
            }
        });

        view.findViewById(R.id.tip_tertiary_text).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                openLink("http://pebble.rickyayoub.com/");
            }
        });

        view.findViewById(R.id.tip_faq).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                openLink("https://docs.google.com/document/d/12EUvuYrydLCrfdAb6wLLvWn-v4C0HfI1f6WP2CT3bCE/pub");
            }
        });

        return view;
    }

    private void openLink(String url)
    {
        Intent intent = new Intent(Intent.ACTION_VIEW);

        intent.setData(Uri.parse(url));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try
        {
            getContext().startActivity(intent);
        }
        catch (ActivityNotFoundException e)
        {
            Toast.makeText(getContext(), R.string.error_no_web_browser, Toast.LENGTH_SHORT).show();
        }
    }

}
