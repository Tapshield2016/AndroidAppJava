package com.tapshield.android.ui.adapter;

import com.tapshield.android.ui.fragment.AlertFragment;
import com.tapshield.android.ui.fragment.DialpadFragment;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class AlertFragmentPagerAdapter extends FragmentPagerAdapter {

	//only 2 fragments, dialpad, and alert
	private static final int PAGES = 2;
	
	public AlertFragmentPagerAdapter(FragmentManager fm) {
		super(fm);
	}

	@Override
	public Fragment getItem(int position) {
		Fragment f = null;
		
		switch (position) {
		case 0:
			f = new DialpadFragment();
			break;
		case 1:
			f = new AlertFragment();
			break;
		}
		
		return f;
	}

	@Override
	public int getCount() {
		return PAGES;
	}
}
