package com.tapshield.android.ui.activity;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.tapshield.android.app.TapShieldApplication;

public class BaseFragmentActivity extends FragmentActivity {

	@Override
	protected void onCreate(Bundle arg0) {
		//Get a Tracker (should auto-report)
		((TapShieldApplication) getApplication()).getTracker(TapShieldApplication.TrackerName.APP_TRACKER);
		super.onCreate(arg0);
	}
	
	@Override
	protected void onStart() {
		GoogleAnalytics.getInstance(this).reportActivityStart(this);
		super.onStart();
	}
	
	@Override
	protected void onStop() {
		GoogleAnalytics.getInstance(this).reportActivityStop(this);
		super.onStop();
	}
}
