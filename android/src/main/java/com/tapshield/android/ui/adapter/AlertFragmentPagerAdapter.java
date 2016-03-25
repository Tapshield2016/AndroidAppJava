package com.tapshield.android.ui.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.tapshield.android.ui.fragment.AlertFragment;
import com.tapshield.android.ui.fragment.DialpadFragment;

public class AlertFragmentPagerAdapter extends FragmentPagerAdapter {

	//only 2 fragments, dialpad, and alert fragments
	private static final int PAGES = 2;
	
	//keep reference since it it will always be a statically small number of fragments
	private Fragment[] mFragments = new Fragment[PAGES];
	
	public AlertFragmentPagerAdapter(FragmentManager fm) {
		super(fm);
	}

	@Override
	public Fragment getItem(int position) {
		Fragment f = null;
		
		if (mFragments[position] != null) {
			return mFragments[position];
		}
		
		switch (position) {
		case 0:
			f = new DialpadFragment();
			break;
		case 1:
			f = new AlertFragment();
			break;
		}
		
		mFragments[position] = f;
		return f;
	}

	@Override
	public int getCount() {
		return PAGES;
	}
}
