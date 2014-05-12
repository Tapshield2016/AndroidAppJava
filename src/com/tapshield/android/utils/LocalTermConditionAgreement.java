package com.tapshield.android.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class LocalTermConditionAgreement {

	private static final String KEY_TERMCONDITIONSACCEPTED = "com.tapshield.android.extra.termconditions";
	
	public static final void setTermConditionsAccepted(final Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putBoolean(KEY_TERMCONDITIONSACCEPTED, true);
		editor.commit();
	}
	
	public static final boolean getTermConditionsAccepted(final Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getBoolean(KEY_TERMCONDITIONSACCEPTED, false);
	}
}
