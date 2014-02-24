package com.tapshield.android.ui.activity;

import android.app.ActionBar;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.location.LocationTracker;
import com.tapshield.android.manager.EmergencyManager;
import com.tapshield.android.ui.fragment.NavigationFragment.OnNavigationItemClickListener;
import com.tapshield.android.utils.UiUtils;

public class MainActivity extends FragmentActivity implements OnNavigationItemClickListener,
		LocationListener {

	private static final String KEY_RESUME = "resuming";
	
	private DrawerLayout mDrawerLayout;
	private FrameLayout mDrawer;
	private GoogleMap mMap;
	private Circle mAccuracyBubble, mUser;
	private Button mEmergency;
	
	private EmergencyManager mEmergencyManager;
	private JavelinClient mJavelin;
	private LocationTracker mTracker;
	
	private boolean mResuming = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mDrawerLayout = (DrawerLayout) findViewById(R.id.main_drawer_layout);
		mDrawer = (FrameLayout) findViewById(R.id.main_drawer);
		mMap = ((SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.main_fragment_map)).getMap();
		mEmergency = (Button) findViewById(R.id.main_button);
		
		mEmergencyManager = EmergencyManager.getInstance(this);
		mJavelin = JavelinClient.getInstance(this, TapShieldApplication.JAVELIN_CONFIG);
		mTracker = LocationTracker.getInstance(this);
		
		ActionBar actionBar = getActionBar();
		actionBar.setHomeButtonEnabled(true);
		
		mEmergency.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mEmergencyManager.start(10000, EmergencyManager.TYPE_START_REQUESTED);
				UiUtils.startActivityNoStack(MainActivity.this, EmergencyActivity.class);
			}
		});
		
		if (savedInstanceState != null) {
			mResuming = savedInstanceState.getBoolean(KEY_RESUME);
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(KEY_RESUME, mResuming);
		super.onSaveInstanceState(outState);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		boolean userPresent = mJavelin.getUserManager().isPresent();
		
		if (!userPresent) {
			if (mResuming) {
				//if resuming but still no user, login was not successful, finish it
				finish();
			} else {
				mResuming = true;
				UiUtils.startActivityNoStack(this, WelcomeActivity.class);
			}
		}
		mTracker.start();
		mTracker.addLocationListener(this);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		mTracker.removeLocationListener(this);
		
		//stop only if not running--it will continue if user requests an emergency
		if (!EmergencyManager.getInstance(this).isRunning()) {
			mTracker.stop();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case android.R.id.home:
			boolean isOpened = mDrawerLayout.isDrawerOpen(mDrawer);
			if (isOpened) {
				mDrawerLayout.closeDrawer(mDrawer);
			} else {
				mDrawerLayout.openDrawer(mDrawer);
			}
			return true;
		}
		return false;
	}
	
	@Override
	public void onLocationChanged(Location location) {
		float maxZoom = mMap.getMaxZoomLevel();
		float standardZoom = 18;
		if (standardZoom > maxZoom) {
			standardZoom = maxZoom;
		}
		float currentZoom = mMap.getCameraPosition().zoom;
		float newZoom = currentZoom < standardZoom ? standardZoom : currentZoom;

		//animate camera
		LatLng cameraLatLng = new LatLng(location.getLatitude(), location.getLongitude());
		CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(cameraLatLng, newZoom);
		mMap.animateCamera(cameraUpdate);

		//accuracy
		LatLng center = cameraLatLng;
		float radius = location.getAccuracy();

		if (mAccuracyBubble == null) {
			CircleOptions bubbleOptions = new CircleOptions()
					.center(center)
					.radius(radius)
					.strokeWidth(4)
					.strokeColor(Color.parseColor("#FF009966"))
					.fillColor(Color.parseColor("#33009966"));

			mAccuracyBubble = mMap.addCircle(bubbleOptions);
		} else {
			mAccuracyBubble.setCenter(center);
			mAccuracyBubble.setRadius(radius);
		}
		
		//user
		if (mUser == null) {
			CircleOptions userOptions = new CircleOptions()
					.center(center)
					.radius(1)
					.strokeWidth(0)
					.fillColor(Color.parseColor("#FF009999"));
			mUser = mMap.addCircle(userOptions);
		} else {
			mUser.setCenter(center);
		}
	}

	@Override
	public void onNavigationItemClick(int position) {
		
	}
}
