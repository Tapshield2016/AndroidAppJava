package com.tapshield.android.ui.activity;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.joda.time.DateTime;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
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
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinSocialReportingManager;
import com.tapshield.android.api.JavelinSocialReportingManager.SocialReportingListener;
import com.tapshield.android.api.JavelinUserManager;
import com.tapshield.android.api.googledirections.model.Route;
import com.tapshield.android.api.model.SocialCrime;
import com.tapshield.android.api.model.SocialCrime.SocialCrimes;
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
import com.tapshield.android.utils.DateTimeUtils;
import com.tapshield.android.utils.LocalTermConditionAgreement;
import com.tapshield.android.utils.MapUtils;
import com.tapshield.android.utils.SocialReportsUtils;
import com.tapshield.android.utils.SpotCrimeUtils;
import com.tapshield.android.utils.UiUtils;

public class MainActivity extends BaseFragmentActivity implements OnNavigationItemClickListener,
		LocationListener, YankListener {

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
	
	private GroundOverlay[] mLogoOverlays;
	private BroadcastReceiver mLogoUpdatedReceiver;
	
	private AlertDialog mYankDialog;
	private AlertDialog mDisconnectedDialog;

	private boolean mUserScrollingMap = false;
	private boolean mTrackUser = true;
	private boolean mResuming = false;
	private boolean mUserBelongsToAgency = false;
	
	private ConcurrentMap<Integer, Crime> mSpotCrimeRecords = new ConcurrentHashMap<Integer, Crime>();
	private ConcurrentMap<Crime, Marker> mSpotCrimeMarkers = new ConcurrentHashMap<Crime, Marker>();
	private boolean mSpotCrimeError = false;
	
	private ConcurrentMap<String, SocialCrime> mSocialCrimesRecords = new ConcurrentHashMap<String, SocialCrime>();
	private ConcurrentMap<SocialCrime, Marker> mSocialCrimesMarkers = new ConcurrentHashMap<SocialCrime, Marker>();
	
	private boolean mSocialCrimesError = false;
	
	private Handler mCrimesHandler = new Handler();
	private Runnable mSpotCrimesUpdater;
	private Runnable mSocialCrimesUpdater;
	
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

		mLogoUpdatedReceiver = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				if (mLogoOverlays != null) {
					for (GroundOverlay overlay : mLogoOverlays) {
						overlay.remove();
					}
				}
				loadAgencyLogo();
			}
		};
		
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
		
		//load all map-related except for Entourage, that will be loaded once map has loaded
		loadMapSettings();
		loadAgencyBoundaries();
		loadAgencyLogo();
		
		//define runnables for periodic updates on crimes (to be started/stopped at onStart/onStop)
		mSpotCrimesUpdater = new Runnable() {
			
			@Override
			public void run() {
				loadNearbySpotCrime();
				mCrimesHandler.postDelayed(mSpotCrimesUpdater,
						TapShieldApplication.SPOTCRIME_UPDATE_FREQUENCY_SECONDS * 1000);
			}
		};
		
		mSocialCrimesUpdater = new Runnable() {
			
			@Override
			public void run() {
				loadNearbySocialCrimes();
				mCrimesHandler.postDelayed(mSocialCrimesUpdater,
						TapShieldApplication.SOCIAL_CRIMES_UPDATE_FREQUENCY_SECONDS * 1000);
			}
		};
	}
	
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		mDrawerToggle.syncState();
		
		mMap = ((SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.main_fragment_map)).getMap();
		
		if (mMap != null) {
			//load all map-related except for Entourage, that will be loaded once map has loaded
			loadMapSettings();
			loadAgencyBoundaries();
			
			mMap.setInfoWindowAdapter(new CrimeInfoWindowAdapter(this));
			mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
				
				@Override
				public void onInfoWindowClick(Marker marker) {
					if (marker == null) {
						return;
					}
					
					boolean found = false;
					Intent details = new Intent(MainActivity.this, ReportDetailsActivity.class);
					
					//iterate through the records until a matching marker is found
					Iterator<Crime> spotCrimeIter = mSpotCrimeMarkers.keySet().iterator();
					while (spotCrimeIter != null && spotCrimeIter.hasNext()) {
						Crime c = spotCrimeIter.next();
						if (marker.equals(mSpotCrimeMarkers.get(c))) {
							found = true;
							details.putExtra(ReportDetailsActivity.EXTRA_REPORT_TYPE,
									ReportDetailsActivity.TYPE_SPOTCRIME);
							details.putExtra(ReportDetailsActivity.EXTRA_REPORT_ID, c.getId());
							break;
						}
					}
					
					//iterate through records if not found yet
					if (!found) {
						Iterator<SocialCrime> socialCrimesIter = mSocialCrimesMarkers.keySet().iterator();
						while (socialCrimesIter != null && socialCrimesIter.hasNext()) {
							SocialCrime sc = socialCrimesIter.next();
							if (marker.equals(mSocialCrimesMarkers.get(sc))) {
								found = true;
								details.putExtra(ReportDetailsActivity.EXTRA_REPORT_TYPE,
										ReportDetailsActivity.TYPE_SOCIALCRIME);
								details.putExtra(ReportDetailsActivity.EXTRA_REPORT_ID, sc.getUrl());
								break;
							}
						}
					}
					
					if (found) {
						startActivity(details);
					}
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
			
			mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
				
				@Override
				public void onMapLoaded() {
					loadOnEntourage();
				}
			});
		} else {
			UiUtils.toastLong(this,
					"There's a problem with Google Maps. Please try accessing the app again.");
			finish();
		}
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
		
		IntentFilter filter = new IntentFilter(JavelinUserManager.ACTION_AGENCY_LOGOS_UPDATED);
		registerReceiver(mLogoUpdatedReceiver, filter);
		
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
		
		mCrimesHandler.post(mSpotCrimesUpdater);
		mCrimesHandler.post(mSocialCrimesUpdater);
	}
	
	@Override
	protected void onPause() {
		super.onPause();

		mCrimesHandler.removeCallbacks(mSpotCrimesUpdater);
		mCrimesHandler.removeCallbacks(mSocialCrimesUpdater);
		
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
		
		unregisterReceiver(mLogoUpdatedReceiver);
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
			UiUtils.showTutorialTipDialog(
					this,
					R.string.ts_entourage_warning_limitations_and_boundaries_title,
					R.string.ts_entourage_warning_limitations_and_boundaries_message,
					"entourage.warning_limitations_and_boundaries");
			
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
		//load crimes from this method only once (how? mUserLocation should be null just once)
		boolean firstUpdate = mUserLocation == null;
		mUserLocation = location;
		drawUser();

		
		if (firstUpdate) {
			loadNearbySpotCrime();
			loadNearbySocialCrimes();
		}
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
		
		MapUtils.displayAgencyBoundaries(MainActivity.this, mMap, userManager.getUser().agency);
	}
	
	private void loadAgencyLogo() {
		JavelinUserManager userManager = mJavelin.getUserManager();

		if (!userManager.isPresent()) {
			return;
		}
		
		mLogoOverlays = MapUtils.displayAgencyLogo(this, mMap);
	}
	
	private void loadNearbySpotCrime() {
		if (mUserLocation == null || mSpotCrimeError) {
			return;
		}
		
		final long since = new DateTime()
				.minusHours(TapShieldApplication.CRIMES_PERIOD_HOURS)
				.getMillis();
		
		SpotCrimeRequest request =
				new SpotCrimeRequest(TapShieldApplication.SPOTCRIME_CONFIG,
						mUserLocation.getLatitude(), mUserLocation.getLongitude(),
						TapShieldApplication.SPOTCRIME_RADIUS)
				.setSince(since)
				.setSortBy(SpotCrimeRequest.SORT_BY_DATE)
				.setSortOrder(SpotCrimeRequest.SORT_ORDER_DESCENDING)
				.setMaxRecords(500);
		
		SpotCrimeCallback callback = new SpotCrimeCallback() {
			
			@Override
			public void onRequest(boolean ok, List<Crime> results, String errorIfNotOk) {
				
				mSpotCrimeError = !ok;
				
				if (ok) {
					
					if (isFinishing() || results == null || mMap == null) {
						return;
					}

					DateTime limit = new DateTime()
							.minusHours(TapShieldApplication.CRIMES_PERIOD_HOURS);
					
					//add new ones (records and markers)
					for (Crime crime : results) {
						DateTime crimeDateTime = SpotCrimeUtils.getDateTimeFromCrime(crime);
						boolean old = crimeDateTime.isBefore(limit);
						boolean notOther = !crime
								.getType()
								.trim()
								.toLowerCase(Locale.getDefault())
								.equals(SpotCrimeClient.TYPE_OTHER);
						
						//add non-duplicates and ones within the timeframe
						if (!old && notOther && !mSpotCrimeRecords.containsKey(crime.getId())) {
							mSpotCrimeRecords.put(crime.getId(), crime);
							mSpotCrimeMarkers.put(crime, addCrimeMarker(crime, crimeDateTime));
						}
					}
					
					//remove old ones
					for (Crime crime : mSpotCrimeRecords.values()) {
						boolean old = SpotCrimeUtils.getDateTimeFromCrime(crime).isBefore(limit);
						
						if (old) {
							//remove stored marker
							if (mSpotCrimeMarkers.containsKey(crime)) {
								mSpotCrimeMarkers.get(crime).remove();
								mSpotCrimeMarkers.remove(crime);
							}
							
							//remove stored record
							if (mSpotCrimeRecords.containsKey(crime.getId())) {
								mSpotCrimeRecords.remove(crime.getId());
							}
						}
					}
				}
			}

			@Override
			public void onDetail(boolean ok, Crime crime, String errorIfNotOk) {}
		};
		
		SpotCrimeClient spotCrime = SpotCrimeClient.getInstance(TapShieldApplication.SPOTCRIME_CONFIG);
		spotCrime.request(request, callback);
	}
	
	private Marker addCrimeMarker(Crime crime, DateTime crimeDateTime) {
		//by passing the crime DateTime it avoids parsing another time for adding the marker info
		
		final String type = crime.getType();
		final int markerDrawableResource = SpotCrimeUtils.getDrawableOfType(type, true);
		final String timeDifference = DateTimeUtils.getTimeLabelFor(crimeDateTime);
		
		//set snippet with mandatory time label and source (optional address if not null)
		final String source = getString(R.string.ts_misc_credits_spotcrime);
		final String address = crime.getAddress() != null ?
				CrimeInfoWindowAdapter.SEPARATOR + crime.getAddress() : new String();
		final String snippet = timeDifference
				+ CrimeInfoWindowAdapter.SEPARATOR + source + address;
		
		LatLng position = new LatLng(crime.getLatitude(), crime.getLongitude());
		MarkerOptions markerOptions = new MarkerOptions()
				.draggable(false)
				.icon(BitmapDescriptorFactory.fromResource(markerDrawableResource))
				.anchor(0.5f, 1.0f)
				.alpha(getOpacityOffTimeframeAt(crimeDateTime.getMillis()))
				.position(position)
				.title(type)
				.snippet(snippet);
		return mMap.addMarker(markerOptions);
	}
	
	private void loadNearbySocialCrimes() {
		if (mUserLocation == null || mSocialCrimesError) {
			return;
		}
		
		SocialReportingListener callback = new SocialReportingListener() {

			@Override
			public void onReport(boolean ok, int code, String errorIfNotOk) {}

			@Override
			public void onFetch(boolean ok, int code, SocialCrimes socialCrimes,
					String errorIfNotOk) {

				mSocialCrimesError = !ok;

				if (isFinishing() || !ok || socialCrimes == null
						|| socialCrimes.getSocialCrimes() == null || mMap == null) {
					return;
				}

				DateTime limit = new DateTime()
						.minusHours(TapShieldApplication.CRIMES_PERIOD_HOURS);

				//add new ones (records and markers)
				for (SocialCrime crime : socialCrimes.getSocialCrimes()) {
					boolean old = crime.getDate().isBefore(limit);

					//add non-duplicates and ones within the timeframe

					if (!old && !mSocialCrimesRecords.containsKey(crime.getUrl())) {
						mSocialCrimesRecords.put(crime.getUrl(), crime);;
						mSocialCrimesMarkers.put(crime,
								addSocialCrimeMarker(crime, crime.getDate()));
					}
				}

				//remove old ones (markers and records)
				for (SocialCrime crime : mSocialCrimesRecords.values()) {
					boolean old = crime.getDate().isBefore(limit);

					if (old) {
						//remove stored marker
						if (mSocialCrimesMarkers.containsKey(crime)) {
							mSocialCrimesMarkers.get(crime).remove();
							mSocialCrimesMarkers.remove(crime);
						}

						//remove stored record
						if (mSocialCrimesRecords.containsKey(crime.getUrl())) {
							mSocialCrimesRecords.remove(crime.getUrl());
						}
					}
				}
			}

			@Override
			public void onDetails(boolean ok, int code, SocialCrime socialCrime,
					String errorIfNotOk) {}
		};
		
		JavelinSocialReportingManager socialReporting = mJavelin.getSocialReportingManager();
		socialReporting.getReportsAt(mUserLocation.getLatitude(), mUserLocation.getLongitude(),
				TapShieldApplication.SOCIAL_CRIMES_RADIUS, callback);
	}
	
	private Marker addSocialCrimeMarker(SocialCrime crime, DateTime crimeDateTime) {
		//by passing the crime DateTime it avoids parsing another time for adding the marker info

		final String type = crime.getTypeName();
		final int markerDrawableResource = SocialReportsUtils.getDrawableOfType(type, true);
		final String timeDifference = DateTimeUtils.getTimeLabelFor(crimeDateTime);

		//set snippet with mandatory time label and source
		final String source = getString(R.string.ts_misc_credits_socialcrimes);
		final String snippet = timeDifference + CrimeInfoWindowAdapter.SEPARATOR + source;
		
		LatLng position = new LatLng(crime.getLatitude(), crime.getLongitude());
		MarkerOptions markerOptions = new MarkerOptions()
				.draggable(false)
				.icon(BitmapDescriptorFactory.fromResource(markerDrawableResource))
				.anchor(0.5f, 1.0f)
				.alpha(getOpacityOffTimeframeAt(crimeDateTime.getMillis()))
				.position(position)
				.title(type)
				.snippet(snippet);
		return mMap.addMarker(markerOptions);
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
			break;
		case NavigationFragment.NAV_ID_HELP:
			newActivity = new Intent(this, WebHelpActivity.class);
			break;
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
	
	private float getOpacityOffTimeframeAt(long atMillis) {
		float opacity = 1.0f;
		float opacityMin = TapShieldApplication.CRIMES_MARKER_OPACITY_MINIMUM;
		float opacityRange = 1.0f - opacityMin;
		int timeRangeHours = TapShieldApplication.CRIMES_PERIOD_HOURS;
		long timeMax = new DateTime().minusHours(timeRangeHours).getMillis();
		long timeRange = new DateTime().getMillis() - timeMax;
		float timeRangeRatio = (float) (((double) (atMillis - timeMax)) / (double) timeRange);
		float opacityRangeRatio = opacityRange * timeRangeRatio;
		opacity = opacityMin + opacityRangeRatio;
		return opacity;
	}
}
