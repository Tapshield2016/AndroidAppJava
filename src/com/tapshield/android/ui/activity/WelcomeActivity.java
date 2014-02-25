package com.tapshield.android.ui.activity;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.tapshield.android.R;
import com.tapshield.android.ui.fragment.BaseFragment;
import com.tapshield.android.ui.fragment.LoginFragment;

public class WelcomeActivity extends FragmentActivity {

	private FragmentManager mFragmentManager;
	
	@Override
	protected void onCreate(Bundle b) {
		super.onCreate(b);
		setContentView(R.layout.activity_welcome);
		
		mFragmentManager = getFragmentManager();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		Fragment topFragment = mFragmentManager.findFragmentById(R.id.welcome_fragment);
		boolean added = topFragment != null;
		
		FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
		topFragment = new LoginFragment();
		if (!added) {
			fragmentTransaction.add(R.id.welcome_fragment, topFragment);
		} else {
			fragmentTransaction.replace(R.id.welcome_fragment, topFragment);
		}
		
		fragmentTransaction.commit();
		
		String title = ((BaseFragment) topFragment).getTitle();
		getActionBar().setTitle(title);
	}
}
