package com.tapshield.android.ui.activity;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.tapshield.android.R;
import com.tapshield.android.ui.fragment.BaseFragment.OnUserActionRequestedListener;

public class RegistrationActivity extends FragmentActivity
		implements OnUserActionRequestedListener {

	private FragmentManager mFragmentManager;
	
	private int mIndex = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_registration);
		
		mFragmentManager = getFragmentManager();
		actionBasedOnIndex(mIndex);
	}
	
	private void actionBasedOnIndex(int index) {
		Fragment fragment = instantiateFragmentByIndex(mIndex);
		
		if (fragment == null) {
			return;
		}
		
		FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
		fragmentTransaction.add(R.id.registration_container, fragment);
		fragmentTransaction.commit();
	}
	
	private Fragment instantiateFragmentByIndex(int index) {
		Fragment fragment = null;
		
		switch (index) {
		}
		
		return fragment;
	}

	/*
	interface implemented to either:
	1. proceed (next step in the registration process), or
	2. return (previous step), or
	3. abort (cancel/go back to the initial step)
	*/

	@Override
	public void onProceed() {
		//next step
	}
	
	@Override
	public void onReturn() {
		//previous step
	}
	
	@Override
	public void onAbort() {
		//cancel
	}
}
