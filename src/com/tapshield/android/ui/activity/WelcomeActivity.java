package com.tapshield.android.ui.activity;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinUserManager;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.ui.adapter.WelcomeFragmentPagerAdapter;
import com.tapshield.android.ui.view.PageIndicator;

public class WelcomeActivity extends FragmentActivity {

	private static final float INDICATOR_SKIP_HIDE_OFFSET = 0.25f;
	
	private ViewPager mPager;
	private WelcomeFragmentPagerAdapter mPagerAdapter;
	private TextView mSkip;
	private PageIndicator mPageIndicator;
	private LinearLayout mIndicatorAndSkip;
	
	@Override
	protected void onCreate(Bundle b) {
		super.onCreate(b);
		setContentView(R.layout.activity_welcome);
		mPager = (ViewPager) findViewById(R.id.welcome_pager);
		mSkip = (TextView) findViewById(R.id.welcome_text_skip);
		mPageIndicator = (PageIndicator) findViewById(R.id.welcome_pageindicator);
		mIndicatorAndSkip = (LinearLayout) findViewById(R.id.welcome_linear_indicatorskip);
		
		mPagerAdapter = new WelcomeFragmentPagerAdapter(getSupportFragmentManager());
		mPager.setAdapter(mPagerAdapter);
		mPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			
			@Override
			public void onPageSelected(int position) {
				mPageIndicator.setSelectedPage(position);
				//show if not last, since it could have been hidden
				if (position < mPagerAdapter.getCount() - 1
						&& mIndicatorAndSkip.getVisibility() == View.INVISIBLE) {
					mIndicatorAndSkip.setVisibility(View.VISIBLE);
				}
			}
			
			@Override
			public void onPageScrolled(int position, float offset, int arg2) {
				//hide if it's already scrolling to last position
				//  (which is count() - 1 - another 1 since it is the previous + offset)
				if (position >= mPagerAdapter.getCount() - 2
						&& offset >= INDICATOR_SKIP_HIDE_OFFSET
						&& mIndicatorAndSkip.getVisibility() == View.VISIBLE) {
					mIndicatorAndSkip.setVisibility(View.INVISIBLE);
				} else if (position <= mPagerAdapter.getCount() - 2
						&& offset < INDICATOR_SKIP_HIDE_OFFSET
						&& mIndicatorAndSkip.getVisibility() == View.INVISIBLE) {
					mIndicatorAndSkip.setVisibility(View.VISIBLE);
				}
			}
			
			@Override
			public void onPageScrollStateChanged(int arg0) {}
		});
		
		mSkip.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mPager.setCurrentItem(mPagerAdapter.getCount() - 1, true);
			}
		});
		
		mPageIndicator.setNumPages(mPagerAdapter.getCount());
		getActionBar().hide();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		JavelinUserManager userManager = JavelinClient.getInstance(this,
				TapShieldApplication.JAVELIN_CONFIG).getUserManager();
		
		if (userManager.isPresent()) {
			finish();
		}
	}
}
