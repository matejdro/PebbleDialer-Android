package com.matejdro.pebbledialer.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.matejdro.pebblecommons.util.ListSerialization;
import com.matejdro.pebbledialer.R;

import java.util.ArrayList;
import java.util.List;

public class StringListPopup extends Dialog
{
    private int titleResurce;
    private SharedPreferences settings;
    private String setting;

    private List<String> storage = new ArrayList<String>();
    private List<TextView> listViews = new ArrayList<TextView>();
    private List<View> listSeparators = new ArrayList<View>();

    private DisplayMetrics displayMetrics;

    private LinearLayout listContainer;
    private TextView listEmptyText;
    private Button addButton;

    private boolean changed = false;

    public StringListPopup(Context context, int titleResurce, SharedPreferences settings, String setting)
    {
        super(context);
        this.titleResurce = titleResurce;
        this.settings = settings;
        this.setting = setting;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setTitle(titleResurce);
        setContentView(R.layout.stringlistpopup);

        ((Button) findViewById(R.id.closeButton)).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                dismiss();
            }
        });

        displayMetrics = getContext().getResources().getDisplayMetrics();

        addButton = (Button) findViewById(R.id.addButton);
        listContainer = (LinearLayout) findViewById(R.id.listContainer);
        listEmptyText = (TextView) findViewById(R.id.listEmptyText);

        addButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                openAddDialog("");
            }
        });

        load();

        setOnDismissListener(new OnDismissListener()
        {
            @Override
            public void onDismiss(DialogInterface dialog)
            {
                save();
            }
        });
    }

    private TextView createViewItem(String text)
    {
        TextView view = new TextView(getContext());
        view.setText(text);

        view.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                int id = listViews.indexOf(view);
                openEditDialog(id, storage.get(id));
            }
        });

        view.setBackgroundResource(R.drawable.list_background);

        return view;
    }

    private View createSeparatorView()
    {
        View view = new View(getContext());

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPixels(2));
        params.setMargins(dpToPixels(30), dpToPixels(2), dpToPixels(30), dpToPixels(10));
        view.setLayoutParams(params);
        view.setBackgroundColor(0xFFDDDDDD);
        return view;
    }

    protected void openAddDialog(String text)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        final EditText editField = new EditText(getContext());
        editField.setText(text);

        builder.setTitle(R.string.add);
        builder.setView(editField);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                add(editField.getText().toString());
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    protected void openEditDialog(final int id, String text)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        final EditText editField = new EditText(getContext());
        editField.setText(text);

        builder.setTitle(R.string.edit);
        builder.setView(editField);

        builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                update(id, editField.getText().toString());
            }
        });

        builder.setNeutralButton(R.string.delete, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                remove(id);
            }
        });

        builder.setNegativeButton(R.string.cancel, null);
        builder.show();

    }

    public void remove(int id)
    {
        changed = true;

        View textView = listViews.get(id);
        listContainer.removeView(textView);
        listViews.remove(id);

        if (storage.size() > 1)
        {
            int separatorToRemove = id;
            if (separatorToRemove >= storage.size())
                separatorToRemove--;

            View separatorView = listSeparators.get(separatorToRemove);
            listSeparators.remove(separatorToRemove);
            listContainer.removeView(separatorView);
        }

        storage.remove(id);

        if (storage.isEmpty())
            listEmptyText.setVisibility(View.VISIBLE);
    }

    public void add(String text)
    {
        changed = true;

        TextView listItem = createViewItem(text);
        View separator = createSeparatorView();

        storage.add(text);
        listViews.add(listItem);
        listSeparators.add(separator);

        if (storage.size() > 1)
            listContainer.addView(separator);
        listContainer.addView(listItem);

        listEmptyText.setVisibility(View.GONE);
    }

    public void update(int id, String text)
    {
        changed = true;

        storage.set(id, text);
        listViews.get(id).setText(text);
    }

    private int dpToPixels(int dp)
    {
        return (int)((dp * displayMetrics.density) + 0.5);
    }



    protected void load()
    {
        List<String> entries = ListSerialization.loadList(settings, setting);
        for (String item : entries)
            add(item);

        changed = false;
    }

    protected void save()
    {
        SharedPreferences.Editor editor = settings.edit();
        ListSerialization.saveList(editor, storage, setting);
        editor.apply();
    }
}
