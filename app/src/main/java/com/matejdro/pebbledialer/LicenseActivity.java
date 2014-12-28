package com.matejdro.pebbledialer;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

public class LicenseActivity extends Activity {
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        webView = new WebView(this);
        setContentView(webView);

        webView.loadData(getResources().getString(R.string.license), "text/html", "utf-8");
        super.onCreate(savedInstanceState);
    }
}
