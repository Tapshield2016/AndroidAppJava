package com.tapshield.android.ui.fragment;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;

import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinUserManager.OnUserLogOutListener;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.manager.SessionManager;
import com.tapshield.android.ui.activity.AboutActivity;
import com.tapshield.android.ui.activity.MainActivity;
import com.tapshield.android.ui.activity.ResetPasscodePasswordActivity;
import com.tapshield.android.ui.activity.SetOrganizationActivity;
import com.tapshield.android.utils.UiUtils;

public class SettingsFragment extends PreferenceFragment
		implements OnPreferenceClickListener, OnUserLogOutListener {

	private Preference mAddChangeOrg;
	private Preference mChangePasscode;
	private Preference mSignOut;
	private Preference mFeedback;
	private Preference mAbout;
	
	private String mAddChangeOrgKey;
	private String mChangePasscodeKey;
	private String mSignOutKey;
	private String mFeedbackKey;
	private String mAboutKey;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);
		
		mAddChangeOrgKey = getString(R.string.ts_settings_org_key);
		mChangePasscodeKey = getString(R.string.ts_settings_passcode_key);
		mSignOutKey = getString(R.string.ts_settings_logout_key);
		mFeedbackKey = getString(R.string.ts_settings_feedback_key);
		mAboutKey = getString(R.string.ts_settings_about_key);
		
		mAddChangeOrg = (Preference) getPreferenceManager().findPreference(mAddChangeOrgKey);
		mChangePasscode = (Preference) getPreferenceManager().findPreference(mChangePasscodeKey);
		mSignOut = (Preference) getPreferenceManager().findPreference(mSignOutKey);
		mFeedback = (Preference) getPreferenceManager().findPreference(mFeedbackKey);
		mAbout = (Preference) getPreferenceManager().findPreference(mAboutKey);
		
		mAddChangeOrg.setOnPreferenceClickListener(this);
		mChangePasscode.setOnPreferenceClickListener(this);
		mSignOut.setOnPreferenceClickListener(this);
		mFeedback.setOnPreferenceClickListener(this);
		mAbout.setOnPreferenceClickListener(this);
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		String key = preference.getKey();
		boolean org = key.equals(mAddChangeOrgKey);
		boolean change = key.equals(mChangePasscodeKey);
		boolean signout = key.equals(mSignOutKey);
		boolean feedback = key.equals(mFeedbackKey);
		boolean about = key.equals(mAboutKey);
		
		Intent intent = null;
		
		if (org) {
			intent = new Intent(getActivity(), SetOrganizationActivity.class);
		} else if (change) {
			intent = new Intent(getActivity(), ResetPasscodePasswordActivity.class);
		} else if (signout) {
			SessionManager
					.getInstance(getActivity())
					.setSporadicChecks(true);
			JavelinClient
					.getInstance(getActivity(), TapShieldApplication.JAVELIN_CONFIG)
					.getUserManager()
					.logOut(this);
		} else if (feedback) {
			String email = getString(R.string.ts_misc_feedback_email);
			String subject = getString(R.string.ts_misc_feedback_subject) + " (" + getVersion() + ")";
			
			Intent send = new Intent(Intent.ACTION_SEND);
			send.setData(Uri.parse("mailto:"));
			send.setType("message/rfc822");
			send.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
			send.putExtra(Intent.EXTRA_SUBJECT, subject);
			
			intent = Intent.createChooser(send, "Choose an Email client:");
		} else if (about) {
			intent = new Intent(getActivity(), AboutActivity.class);
		}
		
		if (intent != null) {
			startActivity(intent);
		}
			
		return true;
	}
	
	private String getVersion() {
		try {
			PackageInfo pInfo = getActivity().getPackageManager().getPackageInfo(
					getActivity().getPackageName(), 0);
			return "v" + pInfo.versionName;
		} catch (Exception e) {
			return new String();
		}
	}

	@Override
	public void onUserLogOut(boolean successful, Throwable e) {
		if (successful) {
			UiUtils.startActivityNoStack(getActivity(), MainActivity.class);
		}
	}
}
