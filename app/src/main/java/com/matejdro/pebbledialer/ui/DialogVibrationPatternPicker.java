package com.matejdro.pebbledialer.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.matejdro.pebblecommons.vibration.VibrationPatternPicker;
import com.matejdro.pebbledialer.R;

public class DialogVibrationPatternPicker extends DialogFragment
{
    private VibrationPatternPicker vibrationPatternPicker;
    private SharedPreferences sharedPreferences;

    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);

        if (context instanceof PreferenceSource)
            sharedPreferences = ((PreferenceSource) context).getCustomPreferences();
        else
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.dialog_vibration_pattern, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        String currentPattern = sharedPreferences.getString("vibrationPattern", "100, 100, 100, 1000");

        vibrationPatternPicker = (VibrationPatternPicker) view.findViewById(R.id.vibration_pattern_picker);
        vibrationPatternPicker.setCurrentPattern(currentPattern);
        vibrationPatternPicker.setAddedPause(1000);

        view.findViewById(R.id.button_help).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showHelp();
            }
        });

        view.findViewById(R.id.button_ok).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                onOkPressed();
            }
        });

        view.findViewById(R.id.button_cancel).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                onCancelPressed();
            }
        });
    }

    private void showHelp()
    {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.setting_vibration_pattern)
                .setMessage(R.string.setting_vibration_pattern_dialog_description)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void onCancelPressed()
    {
        dismiss();
    }

    private void onOkPressed()
    {
        String newPattern = vibrationPatternPicker.validateAndGetCurrentPattern();
        if (newPattern != null)
        {
            dismiss();
            sharedPreferences.edit().putString("vibrationPattern", newPattern).apply();
        }
    }
}
