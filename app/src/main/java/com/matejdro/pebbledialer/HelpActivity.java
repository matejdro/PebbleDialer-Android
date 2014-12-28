package com.matejdro.pebbledialer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebView;

public class HelpActivity extends Activity {
	private WebView webView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		webView = new WebView(this);
		setContentView(webView);
		
		webView.loadUrl(loadHelp().toString());
		super.onCreate(savedInstanceState);
	}
	
	private Uri loadHelp()
	{
		File publicDir = getExternalCacheDir();
		File helpFile = new File(publicDir, "help.html");

		//Copy file from assets
		try
		{
			InputStream myInput = getAssets().open("help.html");
			OutputStream myOutput = new FileOutputStream(helpFile);

			byte[] buffer = new byte[1024];
			int length;
			while ((length = myInput.read(buffer))>0){
				myOutput.write(buffer, 0, length);
			}

			myOutput.close();
			myInput.close();

		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		return Uri.fromFile(helpFile);
	}
}
