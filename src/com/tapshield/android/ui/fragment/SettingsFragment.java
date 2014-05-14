package com.tapshield.android.ui.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;

import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinUserManager.OnUserLogOutListener;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.ui.activity.AboutActivity;
import com.tapshield.android.ui.activity.MainActivity;
import com.tapshield.android.utils.UiUtils;

public class SettingsFragment extends PreferenceFragment implements OnPreferenceClickListener, OnUserLogOutListener {

	Preference mChangePasscode;
	Preference mSignOut;
	Preference mFeedback;
	Preference mAbout;
	
	String mChangePasscodeKey;
	String mSignOutKey;
	String mFeedbackKey;
	String mAboutKey;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);
		
		mChangePasscodeKey = getString(R.string.ts_settings_passcode_key);
		mSignOutKey = getString(R.string.ts_settings_logout_key);
		mFeedbackKey = getString(R.string.ts_settings_feedback_key);
		mAboutKey = getString(R.string.ts_settings_about_key);
		
		mChangePasscode = (Preference) getPreferenceManager().findPreference(mChangePasscodeKey);
		mSignOut = (Preference) getPreferenceManager().findPreference(mSignOutKey);
		mFeedback = (Preference) getPreferenceManager().findPreference(mFeedbackKey);
		mAbout = (Preference) getPreferenceManager().findPreference(mAboutKey);
		
		mChangePasscode.setOnPreferenceClickListener(this);
		mSignOut.setOnPreferenceClickListener(this);
		mFeedback.setOnPreferenceClickListener(this);
		mAbout.setOnPreferenceClickListener(this);
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		String key = preference.getKey();
		boolean change = key.equals(mChangePasscodeKey);
		boolean signout = key.equals(mSignOutKey);
		boolean feedback = key.equals(mFeedbackKey);
		boolean about = key.equals(mAboutKey);
		
		if (change) {
			//Intent intent = new Intent(getActivity(), ChangePasscodeActivity.class);
			//startActivity(intent);
		} else if (signout) {
			JavelinClient
					.getInstance(getActivity(), TapShieldApplication.JAVELIN_CONFIG)
					.getUserManager()
					.logOut(this);
		} else if (feedback) {
			String email = getString(R.string.ts_misc_feedback_email);
			String subject = getString(R.string.ts_misc_feedback_subject);
			
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setData(Uri.parse("mailto:"));
			intent.setType("message/rfc822");
			intent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
			intent.putExtra(Intent.EXTRA_SUBJECT, subject);
			startActivity(Intent.createChooser(intent, "Choose an Email client:"));
		} else if (about) {
			Intent intent = new Intent(getActivity(), AboutActivity.class);
			startActivity(intent);
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
