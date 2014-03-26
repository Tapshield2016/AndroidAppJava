package com.tapshield.android.ui.activity;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.TextView;

import com.tapshield.android.R;
import com.tapshield.android.ui.fragment.BaseFragment;
import com.tapshield.android.ui.fragment.BaseFragment.OnUserActionRequestedListener;
import com.tapshield.android.ui.fragment.EmailConfirmationFragment;
import com.tapshield.android.ui.fragment.OrganizationSelectionFragment;
import com.tapshield.android.ui.fragment.PhoneConfirmationFragment;
import com.tapshield.android.ui.fragment.RequiredInfoFragment;
import com.tapshield.android.ui.view.StepIndicator;

public class RegistrationActivity extends FragmentActivity
		implements OnUserActionRequestedListener {
	
	public static final String EXTRA_SKIP_ORG = "com.tapshield.android.extra.skip_org";
	
	private static final int NUM_FRAGMENTS = 4;
	private static final int mFragmentContainer = R.id.registration_container;

	private TextView mStepTitle;
	private StepIndicator mStepIndicator;
	private FragmentManager mFragmentManager;
	
	private int mIndex = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_registration);
		mFragmentManager = getSupportFragmentManager();
		
		View actionBarCustomView = getLayoutInflater().inflate(R.layout.actionbar_steps, null);
		actionBarCustomView.setLayoutParams(new ActionBar.LayoutParams(
				ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT));
		
		mStepTitle = (TextView) actionBarCustomView.findViewById(R.id.actionbar_steps_text);
		mStepIndicator = (StepIndicator)
				actionBarCustomView.findViewById(R.id.actionbar_steps_stepindicator);
		
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setDisplayShowCustomEnabled(true);
		actionBar.setCustomView(actionBarCustomView);

		Intent i = getIntent();
		if (i != null) {
			Bundle e = i.getExtras();
			if (e != null && e.containsKey(EXTRA_SKIP_ORG)) {
				boolean skipOrg = e.getBoolean(EXTRA_SKIP_ORG, false);
				if (skipOrg) {
					mIndex = 1;
				}
			}
		}
		
		setFragmentByIndex();
		mStepIndicator.setNumSteps(NUM_FRAGMENTS);
	}
	
	private void setFragmentByIndex() {
		setFragmentByIndex(null);
	}
	
	private void setFragmentByIndex(Bundle extras) {
		boolean finish = mIndex < 0;
		
		if (finish) {
			finish();
			return;
		}

		//set flag to either add or replace the fragments based on what the container has
		BaseFragment fragment = (BaseFragment) mFragmentManager.findFragmentById(mFragmentContainer);
		boolean add = fragment == null;
		
		fragment = instantiateFragmentByIndex();
		
		if (fragment == null) {
			finish();
			return;
		}
		
		if (extras != null) {
			fragment.setArguments(extras);
		}
		
		//add/replace instantiated fragment
		FragmentTransaction transaction = mFragmentManager.beginTransaction();
		if (add) {
			transaction.add(mFragmentContainer, fragment);
		} else {
			transaction.replace(mFragmentContainer, fragment);
		}
		transaction.commit();
		
		mStepTitle.setText(fragment.getTitle());
		mStepIndicator.setCurrentStep(mIndex);
	}
	
	private BaseFragment instantiateFragmentByIndex() {
		BaseFragment f = null;
		switch (mIndex) {
		case 0:
			f = new OrganizationSelectionFragment();
			f.setTitle("pick your organization");
			break;
		case 1:
			f = new RequiredInfoFragment();
			f.setTitle("create account");
			break;
		case 2:
			f = new EmailConfirmationFragment();
			f.setTitle("email verification");
			break;
		case 3:
			f = new PhoneConfirmationFragment();
			f.setTitle("phone confirmation");
			break;
		case 4:
			break;
		}
		return f;
	}

	/*
	interface implemented to either:
	1. proceed (next step in the registration process), or
	2. return (previous step)
	*/

	@Override
	public void onProceed(Bundle extras) {
		mIndex++;
		setFragmentByIndex(extras);
	}
	
	@Override
	public void onReturn() {
		mIndex--;
		setFragmentByIndex();
	}
	
	@Override
	public void onBackPressed() {
		onReturn();
	}
}
