package com.tapshield.android.ui.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.location.LocationTracker;
import com.tapshield.android.manager.EmergencyManager;
import com.tapshield.android.ui.adapter.AlertFragmentPagerAdapter;
import com.tapshield.android.ui.fragment.DialpadFragment;
import com.tapshield.android.ui.view.AnimatedVerticalColorProgress;
import com.tapshield.android.utils.MapUtils;
import com.tapshield.android.utils.UiUtils;

public class AlertActivity extends BaseFragmentActivity
		implements AnimatedVerticalColorProgress.Listener, OnPageChangeListener, LocationListener {

	private EmergencyManager mEmergencyManager;
	private FrameLayout mMapFrame;
	private GoogleMap mMap;
	private ViewPager mPager;
	private AlertFragmentPagerAdapter mAdapter;
	private LocationTracker mTracker;
	private BroadcastReceiver mCompletionReceiver;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_alertswipe);

		//attempt to set 'Alert: <agency name>' e.g.: 'Alert: TapShield' as activity title
		
		String agencyName = null;
		
		try {
			agencyName = JavelinClient
					.getInstance(this, TapShieldApplication.JAVELIN_CONFIG)
					.getUserManager()
					.getUser()
					.agency
					.name;
		} catch (Exception e) {
			agencyName = null;
		}
		
		if (agencyName != null && !agencyName.isEmpty()) {
			getActionBar().setTitle(getString(R.string.ts_screen_alert) + ": " + agencyName);
		}
		
		mEmergencyManager = EmergencyManager.getInstance(this);
		mMapFrame = (FrameLayout) findViewById(R.id.alert_frame);
		mMap = ((SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.alert_fragment_map)).getMap();
		mAdapter = new AlertFragmentPagerAdapter(getSupportFragmentManager());
		mPager = (ViewPager) findViewById(R.id.alert_pager);
		mPager.setAdapter(mAdapter);
		mPager.setOnPageChangeListener(this);
		
		mTracker = LocationTracker.getInstance(this);
		
		mCompletionReceiver = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				UiUtils.startActivityNoStack(AlertActivity.this, MainActivity.class);
			}
		};
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
	
	@Override
	protected void onResume() {
		super.onResume();
		mTracker.addLocationListener(this);
		mTracker.start();
		
		IntentFilter completionFilter=  new IntentFilter(EmergencyManager.ACTION_EMERGENCY_COMPLETE);
		registerReceiver(mCompletionReceiver, completionFilter);
	}
	
	@Override
	protected void onPause() {
		unregisterReceiver(mCompletionReceiver);
		
		mTracker.removeLocationListener(this);
		mTracker.stop();
		super.onPause();
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

	@Override
	public void onLocationChanged(Location location) {
		MapUtils.displayUserPositionWithAccuracy(this, mMap, location.getLatitude(),
				location.getLongitude(), location.getAccuracy());
		CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(
				location.getLatitude(), location.getLongitude()), 12);
		mMap.moveCamera(cameraUpdate);
	}
}
