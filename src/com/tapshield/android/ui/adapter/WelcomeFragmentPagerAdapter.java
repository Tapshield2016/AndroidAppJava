package com.tapshield.android.ui.adapter;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.tapshield.android.ui.fragment.LoginFragment;
import com.tapshield.android.ui.fragment.TutorialFragment;

public class WelcomeFragmentPagerAdapter extends FragmentPagerAdapter {

	public WelcomeFragmentPagerAdapter(FragmentManager fm) {
		super(fm);
	}

	@Override
	public Fragment getItem(int position) {
		Fragment f = null;
		
		//special case for last fragment
		if (position == getCount() - 1) {
			f = new LoginFragment();
			return f;
		} 
		
		//continue if not last
		Bundle args = new Bundle();
		args.putString(TutorialFragment.EXTRA_TITLE, "Quick tutorial #" + (position + 1));
		
		f = new TutorialFragment();
		f.setArguments(args);
		return f;
	}

	@Override
	public int getCount() {
		return 4;
	}
}
