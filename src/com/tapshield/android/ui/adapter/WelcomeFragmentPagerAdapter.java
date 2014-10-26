package com.tapshield.android.ui.adapter;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.tapshield.android.R;
import com.tapshield.android.ui.fragment.LoginFragment;
import com.tapshield.android.ui.fragment.TutorialFragment;

public class WelcomeFragmentPagerAdapter extends FragmentPagerAdapter {

	private Fragment mLastFragment;
	
	public WelcomeFragmentPagerAdapter(FragmentManager fm) {
		super(fm);
	}

	@Override
	public Fragment getItem(int position) {
		Fragment f = null;
		
		//special case for last fragment
		if (position == getCount() - 1) {
			f = new LoginFragment();
			mLastFragment = f;
			return f;
		} 
		
		//continue if not last
		
		Bundle args = new Bundle();
		args.putInt(TutorialFragment.EXTRA_TEXT, getTextResourceAt(position));
		args.putInt(TutorialFragment.EXTRA_IMAGE, getImageResourceAt(position));
		
		f = new TutorialFragment();
		f.setArguments(args);
		return f;
	}
	
	private int getTextResourceAt(int position) {
		switch (position) {
		case 0:
			return R.string.ts_welcome_intro_1;
		case 1:
			return R.string.ts_welcome_intro_2;
		case 2:
			return R.string.ts_welcome_intro_3;
		case 3:
			return R.string.ts_welcome_intro_4;
		case 4:
			return R.string.ts_welcome_intro_5;
		case 5:
			return R.string.ts_welcome_intro_6;
		default:
			return -1;
		}
	}
	
	private int getImageResourceAt(int position) {
		switch (position) {
		//no case at 0 since no image should be shown
		case 1:
			return R.drawable.ts_intro_2;
		case 2:
			return R.drawable.ts_intro_3;
		case 3:
			return R.drawable.ts_intro_4;
		case 4:
			return R.drawable.ts_intro_5;
		case 5:
			return R.drawable.ts_intro_6;
		default:
			return -1;
		}
	}

	@Override
	public int getCount() {
		return 7;
	}
	
	public Fragment getLastFragment() {
		return mLastFragment;
	}
}
