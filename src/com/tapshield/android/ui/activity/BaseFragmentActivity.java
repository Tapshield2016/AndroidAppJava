package com.tapshield.android.ui.activity;

import android.support.v4.app.FragmentActivity;

import com.google.analytics.tracking.android.EasyTracker;

public class BaseFragmentActivity extends FragmentActivity {

	@Override
	protected void onStart() {
		EasyTracker.getInstance(this).activityStart(this);
		super.onStart();
	}
	
	@Override
	protected void onStop() {
		EasyTracker.getInstance(this).activityStop(this);
		super.onStop();
	}
}
