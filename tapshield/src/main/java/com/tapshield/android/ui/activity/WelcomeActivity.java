package com.tapshield.android.ui.activity;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinUserManager;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.ui.adapter.WelcomeFragmentPagerAdapter;
import com.tapshield.android.ui.fragment.LoginFragment;
import com.tapshield.android.ui.view.PageIndicator;
import com.tapshield.android.utils.UiUtils;

public class WelcomeActivity extends BaseFragmentActivity {

	private static final float INDICATOR_SKIP_HIDE_OFFSET = 0.25f;
	
	private ViewPager mPager;
	private WelcomeFragmentPagerAdapter mPagerAdapter;
	private TextView mSkip;
	private PageIndicator mPageIndicator;
	private LinearLayout mIndicatorAndSkip;
	private ImageView mSwipe;
	private Animation mSwipeAnimation;
	
	@Override
	protected void onCreate(Bundle b) {
		super.onCreate(b);
		setContentView(R.layout.activity_welcome);
		mPager = (ViewPager) findViewById(R.id.welcome_pager);
		mSkip = (TextView) findViewById(R.id.welcome_text_skip);
		mSwipe = (ImageView) findViewById(R.id.welcome_image_swipe);
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
			public void onPageScrolled(int position, float offset, int offsetPixels) {
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
				
				if (offset > 0.1f) {
					hideSwipingIndicator();
				}
			}
			
			@Override
			public void onPageScrollStateChanged(int arg0) {}
		});
		
		mSkip.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				hideSwipingIndicator();
				mPager.setCurrentItem(mPagerAdapter.getCount() - 1, true);
			}
		});
		
		mPageIndicator.setNumPages(mPagerAdapter.getCount());
		getActionBar().hide();
		
		mSwipeAnimation = AnimationUtils.loadAnimation(this, R.anim.swipe);
		mSwipeAnimation.setAnimationListener(new Animation.AnimationListener() {
			
			@Override
			public void onAnimationStart(Animation animation) {}
			
			@Override
			public void onAnimationRepeat(Animation animation) {}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				mSwipeAnimation.reset();
				mSwipeAnimation.start();
			}
		});
		mSwipe.startAnimation(mSwipeAnimation);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		UiUtils.checkLocationServicesEnabled(this);
		
		JavelinUserManager userManager = JavelinClient.getInstance(this,
				TapShieldApplication.JAVELIN_CONFIG).getUserManager();
		
		if (userManager.isPresent()) {
			finish();
		}
	}
	
	@Override
	public void onBackPressed() {
		boolean lastFragment = mPager.getCurrentItem() == mPagerAdapter.getCount() - 1;
		
		if (lastFragment) {
			
			LoginFragment lf = (LoginFragment) mPagerAdapter.getLastFragment();
			boolean alreadyHandled = lf == null ? false : lf.onBackPressed();

			if (alreadyHandled) {
				return;
			}
		}
		super.onBackPressed();
	}
	
	private void hideSwipingIndicator() {
		if (mSwipe.getVisibility() == View.GONE) {
			return;
		}
		
		mSwipeAnimation.setAnimationListener(null);
		mSwipe.clearAnimation();
		mSwipe.setVisibility(View.GONE);
	}
}
