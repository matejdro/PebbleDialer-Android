package com.matejdro.pebbledialer;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

public class ContactUtils {
	public static String convertNumberType(int id, String label)
	{
		switch (id)
		{
			case ContactsContract.CommonDataKinds.Phone.TYPE_ASSISTANT: return "Assistant";
			case ContactsContract.CommonDataKinds.Phone.TYPE_CALLBACK: return "Callback";
			case ContactsContract.CommonDataKinds.Phone.TYPE_CAR: return "Car";
			case ContactsContract.CommonDataKinds.Phone.TYPE_COMPANY_MAIN: return "Company";
			case ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME: return "Fax Home";
			case ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK: return "Fax Work";
			case ContactsContract.CommonDataKinds.Phone.TYPE_HOME: return "Home";
			case ContactsContract.CommonDataKinds.Phone.TYPE_ISDN: return "ISDN";
			case ContactsContract.CommonDataKinds.Phone.TYPE_MAIN: return "Main";
			case ContactsContract.CommonDataKinds.Phone.TYPE_MMS: return "MMS";
			case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE: return "Mobile";
			case ContactsContract.CommonDataKinds.Phone.TYPE_OTHER_FAX: return "Other fax";
			case ContactsContract.CommonDataKinds.Phone.TYPE_PAGER: return "Pager";
			case ContactsContract.CommonDataKinds.Phone.TYPE_RADIO: return "Radio";
			case ContactsContract.CommonDataKinds.Phone.TYPE_TELEX: return "Telex";
			case ContactsContract.CommonDataKinds.Phone.TYPE_TTY_TDD: return "TTY TDD";
			case ContactsContract.CommonDataKinds.Phone.TYPE_WORK: return "Work";
			case ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE: return "Work Mobile";
			case ContactsContract.CommonDataKinds.Phone.TYPE_WORK_PAGER: return "Work Pager";
			case ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM: return label == null ? "Other" : label;
		}
		
		return "Other";
	}		
	
	public static void call(String number, Context context)
	{
		number = PhoneNumberUtils.stripSeparators(number);
		Log.d("PebbleDialer", "Calling " + number);

		Intent intent = new Intent(Intent.ACTION_CALL);
		intent.setData(Uri.parse("tel:" + number));
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);

	}
}
