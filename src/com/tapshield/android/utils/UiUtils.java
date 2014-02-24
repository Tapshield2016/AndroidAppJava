package com.tapshield.android.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

public class UiUtils {

	public static final void toastShort(Context context, String message) {
		toast(context, message, Toast.LENGTH_SHORT);
	}

	public static final void toastLong(Context context, String message) {
		toast(context, message, Toast.LENGTH_LONG);
	}

	private static final void toast(Context context, String message, int duration) {
		Toast.makeText(context, message, duration).show();
	}

	public static Typeface getCustomTypeface(Context context, String fontName) {
		try {
			return Typeface.createFromAsset(context.getAssets(), "fonts/" + fontName);
		} catch (Exception e) {
			return null;
		}
	}

	public static void hideKeyboard(Activity activity) {
		try {
			InputMethodManager inputManager = (InputMethodManager) 
					activity.getSystemService(Context.INPUT_METHOD_SERVICE); 
			inputManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(),
					InputMethodManager.HIDE_NOT_ALWAYS);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Makes a phone call...
	 * @param context ...from this context...
	 * @param phoneNumber ...to this number.
	 */
	public static void MakePhoneCall(Context context, String phoneNumber) {
		Intent call = new Intent(Intent.ACTION_CALL);
		call.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		call.setData(Uri.parse("tel:" + phoneNumber));
		context.startActivity(call);
	}
	
	public static void startActivityNoStack(Context fromContext,
			Class<? extends Activity> toActivityClass) {
		startActivityNoStack(fromContext, toActivityClass, null);
	}
	
	public static void startActivityNoStack(Context fromContext,
			Class<? extends Activity> toActivityClass, Bundle withExtras) {
		
		Intent intent = new Intent(fromContext, toActivityClass);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
		
		if (withExtras != null) {
			intent.putExtras(withExtras);
		}
		
		fromContext.startActivity(intent);
	}
}
