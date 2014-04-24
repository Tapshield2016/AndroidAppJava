package com.tapshield.android.ui.activity;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.tapshield.android.R;
import com.tapshield.android.manager.EmergencyManager;
import com.tapshield.android.ui.adapter.AlertFragmentPagerAdapter;
import com.tapshield.android.ui.fragment.DialpadFragment;
import com.tapshield.android.ui.view.AnimatedVerticalColorProgress;

public class AlertActivity extends FragmentActivity
		implements AnimatedVerticalColorProgress.Listener, OnPageChangeListener {

	private EmergencyManager mEmergencyManager;
	private FrameLayout mMapFrame;
	private GoogleMap mMap;
	private ViewPager mPager;
	private AlertFragmentPagerAdapter mAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_alertswipe);
		
		mEmergencyManager = EmergencyManager.getInstance(this);
		mMapFrame = (FrameLayout) findViewById(R.id.alert_frame);
		mMap = ((SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.alert_fragment_map)).getMap();
		mAdapter = new AlertFragmentPagerAdapter(getSupportFragmentManager());
		mPager = (ViewPager) findViewById(R.id.alert_pager);
		mPager.setAdapter(mAdapter);
		mPager.setOnPageChangeListener(this);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_alert:
			
			startAlertIfScheduled();
			
			//if dialpad, go to alert
			if (mPager.getCurrentItem() == 0) {
				mPager.setCurrentItem(1, true);
			}
			return true;
		case R.id.action_disarm:
			//if alert, go to dialpad
			if (mPager.getCurrentItem() == 1) {
				mPager.setCurrentItem(0, true);
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		switch (mPager.getCurrentItem()) {
		case 0:
			getMenuInflater().inflate(R.menu.alert, menu);
			return true;
		case 1:
			getMenuInflater().inflate(R.menu.disarm, menu);
			return true;
		}
		return false;
	}
	
	//Go to last page (alert page) once the animation ends
	@Override
	public void onEnd() {
		mPager.setCurrentItem(mAdapter.getCount() - 1, true);
	}

	@Override
	public void onPageScrollStateChanged(int arg0) {}

	@Override
	public void onPageScrolled(int position, float offset, int offsetPixels) {
		//assuming there will always be just 2 pages, the offset should be taking into account
		// at position 0, and apply its offset value as opacity to the map fragment
		float alpha = position == 0 ? offset : 1f;
		mMapFrame.setAlpha(alpha);
	}

	@Override
	public void onPageSelected(int position) {
		//alert view, start if not
		if (position == 1) {
			startAlertIfScheduled();
		}
		invalidateOptionsMenu();
	}
	
	private void startAlertIfScheduled() {
		
		boolean scheduled = mEmergencyManager.isRunning() && !mEmergencyManager.isTransmitting();
		
		if (scheduled) {
			mEmergencyManager.cancel();
			mEmergencyManager.startNow(EmergencyManager.TYPE_START_REQUESTED);
		}
		
		//also notify dialpad fragment to update indicator counting down
		((DialpadFragment) mAdapter.getItem(0)).notifyAlertStarted();
	}
	
	@Override
	public void onBackPressed() {
		mPager.setCurrentItem(0, true);
	}
}
