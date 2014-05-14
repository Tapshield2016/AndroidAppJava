package com.tapshield.android.ui.activity;

import java.util.List;

import org.joda.time.DateTime;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapLoadedCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinUserManager;
import com.tapshield.android.api.googledirections.model.Route;
import com.tapshield.android.api.model.User;
import com.tapshield.android.api.spotcrime.SpotCrimeClient;
import com.tapshield.android.api.spotcrime.SpotCrimeClient.SpotCrimeCallback;
import com.tapshield.android.api.spotcrime.SpotCrimeRequest;
import com.tapshield.android.api.spotcrime.model.Crime;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.location.LocationTracker;
import com.tapshield.android.manager.EmergencyManager;
import com.tapshield.android.manager.EntourageManager;
import com.tapshield.android.manager.YankManager;
import com.tapshield.android.manager.YankManager.YankListener;
import com.tapshield.android.ui.adapter.CrimeInfoWindowAdapter;
import com.tapshield.android.ui.adapter.NavigationListAdapter.NavigationItem;
import com.tapshield.android.ui.fragment.NavigationFragment;
import com.tapshield.android.ui.fragment.NavigationFragment.OnNavigationItemClickListener;
import com.tapshield.android.ui.view.CircleButton;
import com.tapshield.android.utils.LocalTermConditionAgreement;
import com.tapshield.android.utils.MapUtils;
import com.tapshield.android.utils.SpotCrimeUtils;
import com.tapshield.android.utils.UiUtils;

public class MainActivity extends FragmentActivity implements OnNavigationItemClickListener,
		LocationListener, YankListener, OnMapLoadedCallback {

	public static final String EXTRA_DISCONNECTED = "com.tapshield.android.extra.disconnected";
	
	private static final String KEY_RESUME = "resuming";

	private ActionBarDrawerToggle mDrawerToggle;
	private DrawerLayout mDrawerLayout;
	private FrameLayout mDrawer;
	private Location mUserLocation;
	private GoogleMap mMap;
	private ImageButton mEntourage;
	private ImageButton mLocateMe;
	private CircleButton mEmergency;
	private CircleButton mChat;
	private CircleButton mReport;
	
	private EmergencyManager mEmergencyManager;
	private JavelinClient mJavelin;
	private LocationTracker mTracker;
	private YankManager mYank;
	private EntourageManager mEntourageManager;
	
	private AlertDialog mYankDialog;
	private AlertDialog mDisconnectedDialog;

	private boolean mMapLoaded = false;
	private boolean mUserScrollingMap = false;
	private boolean mTrackUser = true;
	private boolean mResuming = false;
	private boolean mSpotCrimeError = false;
	private boolean mUserBelongsToAgency = false;

	private static final int MINIMUM_NUMBER_CRIMES = 50;
	private long mCrimeSince = new DateTime().minusHours(5).getMillis();
	private float mCrimeRadius = 0.03f;
	private List<Crime> mCrimeRecords;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		if (savedInstanceState != null) {
			mResuming = savedInstanceState.getBoolean(KEY_RESUME);
		}
		
		mDrawerLayout = (DrawerLayout) findViewById(R.id.main_drawer_layout);
		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.ic_drawer, 0, 0) {
			@Override
			public void onDrawerOpened(View drawerView) {
				super.onDrawerOpened(drawerView);
				getActionBar().setTitle(R.string.app_name);
				invalidateOptionsMenu();
			}
			
			@Override
			public void onDrawerClosed(View drawerView) {
				super.onDrawerClosed(drawerView);
				getActionBar().setTitle(R.string.ts_home);
				invalidateOptionsMenu();
			}
		};
		mDrawerToggle.setDrawerIndicatorEnabled(true);
		mDrawerLayout.setDrawerListener(mDrawerToggle);
		
		mDrawer = (FrameLayout) findViewById(R.id.main_drawer);
		mMap = ((SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.main_fragment_map)).getMap();
		mMap.setOnMapLoadedCallback(this);
		mMap.setInfoWindowAdapter(new CrimeInfoWindowAdapter(this));
		
		mEntourage = (ImageButton) findViewById(R.id.main_imagebutton_entourage);
		mLocateMe = (ImageButton) findViewById(R.id.main_imagebutton_locateuser);
		mEmergency = (CircleButton) findViewById(R.id.main_circlebutton_alert);
		mChat = (CircleButton) findViewById(R.id.main_circlebutton_chat);
		mReport = (CircleButton) findViewById(R.id.main_circlebutton_report);
		
		mEmergencyManager = EmergencyManager.getInstance(this);
		mJavelin = JavelinClient.getInstance(this, TapShieldApplication.JAVELIN_CONFIG);
		mTracker = LocationTracker.getInstance(this);
		mYank = YankManager.get(this);
		mEntourageManager = EntourageManager.get(this);
		
		mYankDialog = getYankDialog();
		
		mEntourage.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//Skip selection activities if Entourage is already set (running)
				Class<? extends Activity> clss = EntourageManager.get(MainActivity.this).isSet() ?
						PickArrivalContacts.class : PickDestinationActivity.class;
				Intent activity = new Intent(MainActivity.this, clss);
				startActivity(activity);
			}
		});
		
		mLocateMe.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mTrackUser = true;
				moveCameraToUser(true);
			}
		});
		
		mEmergency.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				if (mUserBelongsToAgency) {
				
					long duration = (long)
							getResources().getInteger(R.integer.timer_emergency_requested_millis);
					mEmergencyManager.start(duration, EmergencyManager.TYPE_START_REQUESTED);
					UiUtils.startActivityNoStack(MainActivity.this, AlertActivity.class);
				} else {
					
					String defaultEmergencyNumber = getString(R.string.ts_no_org_emergency_number);
					UiUtils.MakePhoneCall(MainActivity.this, defaultEmergencyNumber);
				}
			}
		});
		
		mChat.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				UiUtils.startActivityNoStack(MainActivity.this, ChatActivity.class);
			}
		});
		
		mReport.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent reporting = new Intent(MainActivity.this, ReportListActivity.class);
				startActivity(reporting);
			}
		});
		
		mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
			
			@Override
			public void onCameraChange(CameraPosition position) {
				if (mUserScrollingMap && mTrackUser) {
					mTrackUser = false;
				}
			}
		});
	}
	
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		mDrawerToggle.syncState();
	}
	
	@Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(KEY_RESUME, mResuming);
		super.onSaveInstanceState(outState);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		mYank.setListener(this);
		
		JavelinUserManager userManager = mJavelin.getUserManager();
		boolean userPresent = userManager.isPresent();
		
		if (!userPresent) {
			if (mResuming) {
				//if resuming but still no user, login was not successful, finish it
				finish();
			} else {
				mResuming = true;
				UiUtils.startActivityNoStack(this, WelcomeActivity.class);
			}
		} else {
			mUserBelongsToAgency = userManager.getUser().belongsToAgency();

			boolean verifyPhone  = mUserBelongsToAgency && !userManager.getUser().isPhoneNumberVerified();
			boolean acceptedContidions = LocalTermConditionAgreement.getTermConditionsAccepted(this);
			
			if (!mUserBelongsToAgency) {
				mChat.setEnabled(false);
			}
			
			if (verifyPhone || !acceptedContidions) {
				Intent welcome = new Intent(this, WelcomeActivity.class);
				Intent finishStep = new Intent(this, RegistrationActivity.class);
				
				if (verifyPhone) {
					finishStep.putExtra(RegistrationActivity.EXTRA_SET_STEP,
							RegistrationActivity.STEP_PHONEVERIFICATION);
				} else if (!acceptedContidions) {
					finishStep.putExtra(RegistrationActivity.EXTRA_SET_STEP,
							RegistrationActivity.STEP_TERMSCONDITIONS);
				}
				
				Intent[] stack = new Intent[]{welcome, finishStep};
				startActivities(stack);
			} else {
				if (getIntent() != null && getIntent().getBooleanExtra(EXTRA_DISCONNECTED, false)) {
					mDisconnectedDialog = getDisconnectedDialog();
					mDisconnectedDialog.show();
				}
			}
		}
		
		mTracker.start();
		mTracker.addLocationListener(this);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		mYank.removeListener(this);
		
		//disable if no headset is plugged in
		if (mYank.isWaitingForHeadset()) {
			mYank.setEnabled(false);
		}
		
		mTracker.removeLocationListener(this);
		
		//stop only if not running--it will continue if user requests an emergency
		if (!EmergencyManager.getInstance(this).isRunning()) {
			mTracker.stop();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (!mDrawerLayout.isDrawerOpen(mDrawer)) {
			int yankMenu = mYank.isEnabled() ? R.menu.yank : R.menu.yank_disabled;
			getMenuInflater().inflate(yankMenu, menu);
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
		case R.id.action_yank:
			mYank.setEnabled(false);
			break;
		case R.id.action_yank_disabled:
			mYank.setEnabled(true);
			break;
		}
		return false;
	}
	
	@Override
	public void onMapLoaded() {
		mMapLoaded = true;
		loadMapSettings();
		loadAgencyBoundaries();
		loadNearbyCrimes(false);
		loadOnEntourage();
	}
	
	private void moveCameraToUser(boolean animate) {
		if (mUserLocation == null) {
			return;
		}
		
		float maxZoom = mMap.getMaxZoomLevel();
		float standardZoom = 18;
		if (standardZoom > maxZoom) {
			standardZoom = maxZoom;
		}
		float currentZoom = mMap.getCameraPosition().zoom;
		float newZoom = currentZoom < standardZoom ? standardZoom : currentZoom;
		
		LatLng cameraLatLng = new LatLng(mUserLocation.getLatitude(), mUserLocation.getLongitude());
		CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(cameraLatLng, newZoom);
		
		if (animate) {
			mMap.animateCamera(cameraUpdate);
		} else {
			mMap.moveCamera(cameraUpdate);
		}
	}
	
	private void loadOnEntourage() {

		boolean entourageSet = mEntourageManager.isSet();
		
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(!entourageSet);
		actionBar.setHomeButtonEnabled(!entourageSet);
		actionBar.setTitle(R.string.ts_home);
		actionBar.setDisplayShowTitleEnabled(!entourageSet);
		actionBar.setDisplayShowCustomEnabled(entourageSet);

		int drawerLockMode = entourageSet ?
				DrawerLayout.LOCK_MODE_LOCKED_CLOSED :
						DrawerLayout.LOCK_MODE_UNLOCKED;
		mDrawerLayout.setDrawerLockMode(drawerLockMode);
		
		//set custom view with entourage-related information
		if (entourageSet) {
			View entourageActionBarView = getLayoutInflater().inflate(R.layout.actionbar_main_entourage, null);
			
			Route r = mEntourageManager.getRoute();
			
			String destinationString = r.destinationName() != null ? r.destinationName() : r.endAddress();

			long etaMilli = mEntourageManager.getStartAt() + mEntourageManager.getDurationMilli();
			String etaString = new DateTime(etaMilli).toString("MMM dd hh:mm aa");
			
			TextView destination = (TextView)
					entourageActionBarView.findViewById(R.id.actionbar_main_entourage_text_destination);
			TextView eta = (TextView)
					entourageActionBarView.findViewById(R.id.actionbar_main_entourage_text_eta);
			
			destination.setText(destinationString);
			eta.setText(etaString);
			
			actionBar.setCustomView(entourageActionBarView);
			
			//allow entourage to handle the map, user can tap 'locate me' if needed
			mTrackUser = false;
			mEntourageManager.drawOnMap(mMap);
		}
	}
	
	@Override
	public void onLocationChanged(Location location) {
		mUserLocation = location;
		drawUser();
		loadNearbyCrimes(false);
	}
	
	private void loadMapSettings() {
		UiSettings mapSettings = mMap.getUiSettings();
		mapSettings.setRotateGesturesEnabled(false);
		mapSettings.setTiltGesturesEnabled(false);
		mapSettings.setScrollGesturesEnabled(true);
		mapSettings.setZoomGesturesEnabled(true);
		mapSettings.setZoomControlsEnabled(false);
		mMap.setIndoorEnabled(false);
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
	
	private void loadNearbyCrimes(boolean broaderSearch) {
		//stop here if map is not loaded yet or there was an error
		if (!mMapLoaded || mSpotCrimeError || mUserLocation == null) {
			return;
		}
		
		//since this method can be called recursively check for this flag and
		//	return if a broader search is not requested with a non-null list
		//	this way onLocationChanged method is not requesting this one more than once
		if (broaderSearch && mCrimeRecords == null) {
			return;
		}
		
		SpotCrimeRequest request =
				new SpotCrimeRequest(TapShieldApplication.SPOTCRIME_CONFIG,
						mUserLocation.getLatitude(), mUserLocation.getLongitude(), mCrimeRadius)
				.setSince(mCrimeSince)
				.setSortBy(SpotCrimeRequest.SORT_BY_DISTANCE)
				.setSortOrder(SpotCrimeRequest.SORT_ORDER_ASCENDING);
		
		SpotCrimeCallback callback = new SpotCrimeCallback() {
			
			@Override
			public void onRequest(boolean ok, List<Crime> results, String errorIfNotOk) {
				Log.i("spotcrime",
						"callback ok=" + ok
						+ " results=" + (results == null? results : results.size())
						+ " error=" + errorIfNotOk
						+ (ok ? " for=" + new DateTime(mCrimeSince) + " " + mCrimeRadius + "mi" : new String()));
				if (ok) {
					if (results == null) {
						return;
					}

					mCrimeRecords = results;
					
					//broaden search parameter if less than minimum number of crimes
					if (results.size() < MINIMUM_NUMBER_CRIMES) {
						broadenCrimeRetrievalCriteria();
						loadNearbyCrimes(true);
						return;
					}

					addCrimeMarkers();
				} else {
					UiUtils.toastShort(MainActivity.this, "Error loading crimes:" + errorIfNotOk);
				}
			}
		};
		
		SpotCrimeClient spotCrime = SpotCrimeClient.getInstance(TapShieldApplication.SPOTCRIME_CONFIG);
		spotCrime.request(request, callback);
	}
	
	private void addCrimeMarkers() {
		for (Crime c : mCrimeRecords) {
			String type = c.getType();
			
			//for the time being, ignore type 'other.' thus, if (type != other)
			if (!type.equals(SpotCrimeClient.TYPE_OTHER)) {
				int markerDrawableResource = SpotCrimeUtils.getMarkerResourceOfType(type);
				
				LatLng position = new LatLng(c.getLatitude(), c.getLongitude());
				MarkerOptions markerOptions = new MarkerOptions()
						.draggable(false)
						.icon(BitmapDescriptorFactory.fromResource(markerDrawableResource))
						.anchor(0.5f, 1.0f)
						.position(position)
						.title(c.getType())
						.snippet(c.getDate() + CrimeInfoWindowAdapter.SEPARATOR + c.getAddress());
				mMap.addMarker(markerOptions);
			}
		}
	}
	
	private void broadenCrimeRetrievalCriteria() {
		mCrimeSince = new DateTime(mCrimeSince).minusHours(5).getMillis();
		mCrimeRadius += 0.005;
	}
	
	private void drawUser() {
		MapUtils.displayUserPositionWithAccuracy(this, mMap, mUserLocation.getLatitude(),
				mUserLocation.getLongitude(), mUserLocation.getAccuracy());
		
		if (mTrackUser) {
			//do not animate if it is just resuming
			moveCameraToUser(!mResuming);
		}
	}
	
	private AlertDialog getYankDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this)
				.setCancelable(true)
				.setTitle(R.string.ts_main_dialog_yank_title)
				.setMessage(R.string.ts_main_dialog_yank_message)
				.setNegativeButton(R.string.ts_common_cancel, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mYank.setEnabled(false);
					}
				});
		return builder.create();
	}
	
	private AlertDialog getDisconnectedDialog() {
		final String secondaryPhone = mJavelin
				.getUserManager()
				.getUser()
				.agency
				.secondaryNumber;
		String call = getString(R.string.ts_main_dialog_disconnected_button_call_preffix)
				+ " " + secondaryPhone;
		return new AlertDialog.Builder(this)
				.setTitle(R.string.ts_main_dialog_disconnected_title)
				.setPositiveButton(call, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						UiUtils.MakePhoneCall(MainActivity.this, secondaryPhone);
					}
				})
				.setNegativeButton(R.string.ts_common_cancel, null)
				.create();
	}

	@Override
	public void onNavigationItemClick(NavigationItem navItem) {
		
		Intent newActivity = null;
		
		switch (navItem.getId()) {
		case NavigationFragment.NAV_ID_PROFILE:
			newActivity = new Intent(this, FullProfileActivity.class);
			break;
		case NavigationFragment.NAV_ID_NOTIFICATION:
			newActivity = new Intent(this, MassAlertsActivity.class);
			break;
		case NavigationFragment.NAV_ID_HOME:
		case NavigationFragment.NAV_ID_HELP:
		case NavigationFragment.NAV_ID_SETTINGS:
			newActivity = new Intent(this, SettingsActivity.class);
			break;
		case NavigationFragment.NAV_ID_ABOUT:
		}

		if (newActivity != null) {
			startActivity(newActivity);
		}
		
		mDrawerLayout.closeDrawers();
	}

	//yank manager interface
	@Override
	public void onStatusChange(int newStatus) {
		
		invalidateOptionsMenu();
		
		switch (newStatus) {
		case YankManager.Status.DISABLED:
			UiUtils.toastShort(this, getString(R.string.ts_main_toast_yank_disabled));
			break;
		case YankManager.Status.WAITING_HEADSET:
			mYankDialog.show();
			break;
		case YankManager.Status.ENABLED:
			if (mYankDialog.isShowing()) {
				mYankDialog.dismiss();
			}
			UiUtils.toastShort(this, getString(R.string.ts_main_toast_yank_enabled));
			break;
		}
	}
}
