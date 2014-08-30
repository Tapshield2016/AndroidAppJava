package com.tapshield.android.utils;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.tapshield.android.R;
import com.tapshield.android.ui.view.StepIndicator;

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
	
	public static boolean checkLocationServicesEnabled(final Activity activity) {
		
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity)
				.setCancelable(false)
				.setMessage(R.string.ts_dialog_location_services_disabled_message)
				.setNegativeButton(R.string.ts_common_cancel, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						activity.finish();
					}
				})
				.setPositiveButton(R.string.ts_common_settings, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Intent settings = new Intent(Settings.ACTION_SETTINGS);
						activity.startActivity(settings);
					}
				});
		
		boolean networkEnabled = GeoUtils.isProviderEnabled(GeoUtils.PROVIDER_NETWORK, activity);
		boolean gpsEnabled = GeoUtils.isProviderEnabled(GeoUtils.PROVIDER_GPS, activity);

		if (!networkEnabled) {
			dialogBuilder.setTitle(R.string.ts_dialog_location_services_disabled_title_network);
		} else if (!gpsEnabled) {
			dialogBuilder.setTitle(R.string.ts_dialog_location_services_disabled_title_gps);
		}
		
		boolean locationServicesEnabled = networkEnabled && gpsEnabled;
		
		if (!locationServicesEnabled) {
			dialogBuilder.create().show();
		}
		
		return locationServicesEnabled;
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
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		
		if (withExtras != null) {
			intent.putExtras(withExtras);
		}
		
		fromContext.startActivity(intent);
	}
	
	public static void showTutorialTipDialog(final Context context, final int title,
			final int message, final String keySuffix) {
		
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		
		final String key = "com.tapshield.android.preferences.tutorialtip." + keySuffix;
		final boolean show = preferences.getBoolean(key, true);
		
		if (show) {
			final View content = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
					.inflate(R.layout.dialog_tip_tutorial, null);
			final TextView text = (TextView)
					content.findViewById(R.id.dialog_tip_tutorial_text);
			text.setText(message);
			
			final CheckBox checkbox = (CheckBox)
					content.findViewById(R.id.dialog_tip_tutorial_checkbox);
			
			new AlertDialog.Builder(context)
					.setTitle(title)
					.setView(content)
					.setCancelable(false)
					.setPositiveButton(R.string.ts_common_ok, new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							final boolean dontShowAnymore = checkbox.isChecked();
							if (dontShowAnymore) {
								SharedPreferences.Editor editor = preferences.edit();
								editor.putBoolean(key, false);
								editor.commit();
							}
						}
					})
					.create()
					.show();
		}
	}
	
	public static void setStepIndicatorInActionBar(final Activity activity, final int stepCurrent,
			final int stepCount, final int titleResourceId) {
		setStepIndicatorInActionBar(activity, stepCurrent, stepCount,
				activity.getString(titleResourceId));
	}
	
	public static void setStepIndicatorInActionBar(final Activity activity, final int stepCurrent,
			final int stepCount, final String title) {
		
		View actionBarCustomView = activity.getLayoutInflater().inflate(R.layout.actionbar_steps, null);
		actionBarCustomView.setLayoutParams(new ActionBar.LayoutParams(
				ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT));
		
		TextView stepTitle = (TextView) actionBarCustomView.findViewById(R.id.actionbar_steps_text);
		StepIndicator stepIndicator = (StepIndicator)
				actionBarCustomView.findViewById(R.id.actionbar_steps_stepindicator);
		
		ActionBar actionBar = activity.getActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setDisplayShowCustomEnabled(true);
		actionBar.setCustomView(actionBarCustomView);
		
		stepIndicator.setCurrentStep(stepCurrent);
		stepIndicator.setNumSteps(stepCount);
		stepTitle.setText(title);
	}
}
