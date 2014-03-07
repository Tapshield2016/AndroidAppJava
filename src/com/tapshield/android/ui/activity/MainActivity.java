package com.tapshield.android.ui.activity;

import java.util.List;

import org.joda.time.DateTime;

import android.app.ActionBar;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
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
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinUserManager;
import com.tapshield.android.api.model.User;
import com.tapshield.android.api.spotcrime.SpotCrimeClient;
import com.tapshield.android.api.spotcrime.SpotCrimeClient.SpotCrimeCallback;
import com.tapshield.android.api.spotcrime.SpotCrimeRequest;
import com.tapshield.android.api.spotcrime.model.Crime;
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
	private boolean mSpotCrimeDataDownloaded = false;
	
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
		
		loadMapSettings();
		loadAgencyBoundaries();
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
		
		JavelinUserManager userManager = mJavelin.getUserManager();
		
		if (userManager.isPresent()
				&& userManager.getUser().agency != null
				&& userManager.getUser().agency.displayCommandAlert) {
			getMenuInflater().inflate(R.menu.massalerts, menu);
		}
		
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
		case R.id.action_massalerts:
			Intent massAlerts = new Intent(MainActivity.this, MassAlertsActivity.class);
			startActivity(massAlerts);
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
		
		loadSpotCrimeDataAt(location.getLatitude(), location.getLongitude());
	}
	
	private void loadMapSettings() {
		UiSettings mapSettings = mMap.getUiSettings();
		mapSettings.setRotateGesturesEnabled(false);
		mapSettings.setTiltGesturesEnabled(false);
		mapSettings.setScrollGesturesEnabled(false);
		mapSettings.setZoomGesturesEnabled(false);
	}
	
	private void loadAgencyBoundaries() {

		JavelinUserManager userManager = mJavelin.getUserManager();

		if (!userManager.isPresent()) {
			return;
		}

		User user = userManager.getUser();

		if (user.agency == null || !user.agency.hasBoundaries()) {
			return;
		}

		PolygonOptions polygonOptions = new PolygonOptions()
				.strokeWidth(3)
				.strokeColor(Color.parseColor("#FF6600FF"))
				.fillColor(Color.parseColor("#336600FF"));

		for (Location l : user.agency.getBoundaries()) {
			LatLng point = new LatLng(l.getLatitude(), l.getLongitude());
			polygonOptions.add(point);
		}
		
		mMap.addPolygon(polygonOptions);
	}
	
	private void loadSpotCrimeDataAt(double latitude, double longitude) {
		if (mSpotCrimeDataDownloaded) {
			return;
		}
		mSpotCrimeDataDownloaded = true;
		
		long millisOfYesterday = new DateTime().minusDays(1).getMillis();
		
		SpotCrimeRequest request =
				new SpotCrimeRequest(TapShieldApplication.SPOTCRIME_CONFIG,
						latitude, longitude, 0.01f)
				.setMaxRecords(5)
				.setSince(millisOfYesterday)
				.setSortBy(SpotCrimeRequest.SORT_BY_DISTANCE)
				.setSortOrder(SpotCrimeRequest.SORT_ORDER_ASCENDING);
		
		SpotCrimeCallback callback = new SpotCrimeCallback() {
			
			@Override
			public void onRequest(boolean ok, List<Crime> results, String errorIfNotOk) {
				Log.i("spotcrime", "callback ok=" + ok + " results=" + (results != null) + " error=" + errorIfNotOk);
				if (ok) {
					if (results == null) {
						return;
					}
					
					for (Crime c : results) {
						LatLng position = new LatLng(c.getLatitude(), c.getLongitude());
						MarkerOptions markerOptions = new MarkerOptions()
								.draggable(false)
								.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
								.position(position)
								.title(c.getType() + " " + c.getDate())
								.snippet(c.getDescription());
						mMap.addMarker(markerOptions);
					}
				}
			}
		};
		
		SpotCrimeClient spotCrime = SpotCrimeClient.getInstance(TapShieldApplication.SPOTCRIME_CONFIG);
		spotCrime.request(request, callback);
	}

	@Override
	public void onNavigationItemClick(int position) {
		
	}
}
