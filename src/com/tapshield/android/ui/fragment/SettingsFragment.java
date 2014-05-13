package com.tapshield.android.ui.fragment;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;

import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinUserManager.OnUserLogOutListener;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.ui.activity.MainActivity;
import com.tapshield.android.utils.UiUtils;

public class SettingsFragment extends PreferenceFragment implements OnPreferenceClickListener, OnUserLogOutListener {

	Preference mChangePasscode;
	Preference mSignOut;
	Preference mAbout;
	
	String mChangePasscodeKey;
	String mSignOutKey;
	String mAboutKey;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);
		
		mChangePasscodeKey = getString(R.string.ts_settings_passcode_key);
		mSignOutKey = getString(R.string.ts_settings_logout_key);
		mAboutKey = getString(R.string.ts_settings_about_key);
		
		mChangePasscode = (Preference) getPreferenceManager().findPreference(mChangePasscodeKey);
		mSignOut = (Preference) getPreferenceManager().findPreference(mSignOutKey);
		mAbout = (Preference) getPreferenceManager().findPreference(mAboutKey);
		
		mChangePasscode.setOnPreferenceClickListener(this);
		mSignOut.setOnPreferenceClickListener(this);
		mAbout.setOnPreferenceClickListener(this);
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		String key = preference.getKey();
		boolean change = key.equals(mChangePasscodeKey);
		boolean signout = key.equals(mSignOutKey);
		boolean about = key.equals(mAboutKey);
		
		if (change) {
			//Intent changePasscode = new Intent(getActivity(), ChangePasscodeActivity.class);
			//startActivity(changePasscode);
		} else if (signout) {
			JavelinClient
					.getInstance(getActivity(), TapShieldApplication.JAVELIN_CONFIG)
					.getUserManager()
					.logOut(this);
		} else if (about) {
			//Intent about = new Intent(getActivity(), AboutActivity.class);
			//startActivity(about);
		}
			
		return true;
	}

	@Override
	public void onUserLogOut(boolean successful, Throwable e) {
		if (successful) {
			UiUtils.startActivityNoStack(getActivity(), MainActivity.class);
		}
	}
}
