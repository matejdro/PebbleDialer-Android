package com.matejdro.pebbledialer.tasker;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.matejdro.pebbledialer.R;
import com.matejdro.pebbledialer.ui.fragments.settings.CallscreenSettingsFragment;
import com.matejdro.pebbledialer.ui.fragments.settings.GeneralSettingsFragment;
import com.matejdro.pebbledialer.ui.fragments.settings.MessagingSettingsFragment;

public class TaskerMenuFragment extends Fragment
{
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_tasker_menu, container, false);

        view.findViewById(R.id.general_settings).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                onGeneralSettingsClicked();
            }
        });
        view.findViewById(R.id.customize_callscreen).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                onCustomizeCallscreenClicked();
            }
        });
        view.findViewById(R.id.sms).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                onSmsClicked();
            }
        });

        return view;
    }

    private void onGeneralSettingsClicked()
    {
        ((TaskerSettingsActivity) getActivity()).swapFragment(new GeneralSettingsFragment());
    }

    private void onCustomizeCallscreenClicked()
    {
        ((TaskerSettingsActivity) getActivity()).swapFragment(new CallscreenSettingsFragment());
    }

    private void onSmsClicked()
    {
        ((TaskerSettingsActivity) getActivity()).swapFragment(new MessagingSettingsFragment());
    }
}
