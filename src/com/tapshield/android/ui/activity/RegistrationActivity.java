package com.tapshield.android.ui.activity;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.tapshield.android.R;
import com.tapshield.android.ui.fragment.BaseFragment.OnUserActionRequestedListener;
import com.tapshield.android.ui.fragment.EmailConfirmationFragment;
import com.tapshield.android.ui.fragment.RequiredInfoFragment;

public class RegistrationActivity extends FragmentActivity
		implements OnUserActionRequestedListener {

	
	private static final int mFragmentContainer = R.id.registration_container;
	private FragmentManager mFragmentManager;
	private int mIndex = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_registration);
		
		mFragmentManager = getFragmentManager();
		setFragmentByIndex();
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
		Fragment fragment = mFragmentManager.findFragmentById(mFragmentContainer);
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
	}
	
	private Fragment instantiateFragmentByIndex() {
		switch (mIndex) {
		case 0:
			return new RequiredInfoFragment();
		case 1:
			return new EmailConfirmationFragment();
		default:
			return null;
		}
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
