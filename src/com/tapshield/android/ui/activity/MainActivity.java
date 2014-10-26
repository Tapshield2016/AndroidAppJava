package com.tapshield.android.ui.activity;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActionBarDrawerToggle;
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
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.algo.NonHierarchicalDistanceBasedAlgorithm;
import com.google.maps.android.clustering.algo.PreCachingAlgorithmDecorator;
import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinSocialReportingManager;
import com.tapshield.android.api.JavelinSocialReportingManager.SocialReportingListener;
import com.tapshield.android.api.JavelinUserManager;
import com.tapshield.android.api.googledirections.model.Route;
import com.tapshield.android.api.model.SocialCrime;
import com.tapshield.android.api.model.SocialCrime.SocialCrimes;
import com.tapshield.android.api.spotcrime.SpotCrimeClient;
import com.tapshield.android.api.spotcrime.SpotCrimeClient.SpotCrimeCallback;
import com.tapshield.android.api.spotcrime.SpotCrimeRequest;
import com.tapshield.android.api.spotcrime.model.Crime;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.location.LocationTracker;
import com.tapshield.android.manager.EmergencyManager;
import com.tapshield.android.manager.EntourageManager;
import com.tapshield.android.manager.SessionManager;
import com.tapshield.android.manager.YankManager;
import com.tapshield.android.manager.YankManager.YankListener;
import com.tapshield.android.model.CrimeClusterItem;
import com.tapshield.android.model.SocialCrimeClusterItem;
import com.tapshield.android.ui.adapter.CrimeInfoWindowAdapter;
import com.tapshield.android.ui.adapter.NavigationListAdapter.NavigationItem;
import com.tapshield.android.ui.dialog.TalkOptionsDialog;
import com.tapshield.android.ui.dialog.TalkOptionsDialog.TalkOptionsListener;
import com.tapshield.android.ui.fragment.NavigationFragment;
import com.tapshield.android.ui.fragment.NavigationFragment.OnNavigationItemClickListener;
import com.tapshield.android.ui.view.CircleButton;
import com.tapshield.android.ui.view.TickerTextSwitcher;
import com.tapshield.android.utils.ConnectivityMonitor;
import com.tapshield.android.utils.ConnectivityMonitor.ConnectivityMonitorListener;
import com.tapshield.android.utils.CrimeMapClusterRenderer;
import com.tapshield.android.utils.EmergencyManagerUtils;
import com.tapshield.android.utils.MapUtils;
import com.tapshield.android.utils.SocialCrimeMapClusterRenderer;
import com.tapshield.android.utils.SocialReportsUtils;
import com.tapshield.android.utils.SpotCrimeUtils;
import com.tapshield.android.utils.UiUtils;

public class MainActivity extends BaseFragmentActivity implements OnNavigationItemClickListener,
		LocationListener, YankListener {

	public static final String EXTRA_DISCONNECTED = "com.tapshield.android.extra.disconnected";
	
	private static final float DECLUSTER_RADIUS = 0.00005f;
	private static final String KEY_RESUME = "resuming";

	private ActionBarDrawerToggle mDrawerToggle;
	private DrawerLayout mDrawerLayout;
	private FrameLayout mDrawer;
	private Location mUserLocation;
	private GoogleMap mMap;
	private ImageButton mYankToggle;
	private ImageButton mLocateMe;
	private CircleButton mTalk;
	private CircleButton mTrack;
	private CircleButton mReport;
	private TickerTextSwitcher mConnectionTicker;
	private TalkOptionsDialog mTalkOptionsDialog;
	private TalkOptionsListener mTalkOptionsListener;
	
	private EmergencyManager mEmergencyManager;
	private JavelinClient mJavelin;
	private LocationTracker mTracker;
	private YankManager mYank;
	private EntourageManager mEntourageManager;
	private ConnectivityMonitor mConnectionMonitor;
	private ConnectivityMonitorListener mConnectionMonitorListener;
	
	private ClusterManager<CrimeClusterItem> mMapCrimeClusterManager;
	private ClusterManager<SocialCrimeClusterItem> mMapSocialCrimeClusterManager;
	
	private GroundOverlay[] mLogoOverlays;
	private BroadcastReceiver mLogoUpdatedReceiver;
	
	private AlertDialog mYankDialog;
	private AlertDialog mDisconnectedDialog;

	private boolean mTrackUser = true;
	private boolean mResuming = false;
	private boolean mUserBelongsToAgency = false;
	
	private Marker[] mDeclusteredMarkers = null;
	private boolean mDeclusteredGroup = false;
	private CameraPosition mDeclusteredCameraPosition = null;
	private Map<Marker, SocialCrime> mDeclusteredSocialCrimeMap = new HashMap<Marker, SocialCrime>();
	private Map<Marker, Crime> mDeclusteredCrimeMap = new HashMap<Marker, Crime>();

	//crimes (spotcrime, social crimes)
	
	private Handler mCrimesHandler = new Handler();
	
	private ConcurrentMap<Integer, Crime> mSpotCrimeRecords = new ConcurrentHashMap<Integer, Crime>();
	private Runnable mSpotCrimesUpdater;
	private boolean mSpotCrimeError = false;
	private boolean mSpotCrimeWideSearch = false;
	private long mSpotCrimeSince = new DateTime()
			.minusHours(TapShieldApplication.CRIMES_PERIOD_HOURS)
			.getMillis();
	private int mSpotCrimeNumRecords = TapShieldApplication.SPOTCRIME_RECORDS_MAX;
	
	private ConcurrentMap<String, SocialCrime> mSocialCrimesRecords = new ConcurrentHashMap<String, SocialCrime>();
	private boolean mSocialCrimesError = false;
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
				getActionBar().setTitle(R.string.ts_screen_home);
				invalidateOptionsMenu();
			}
		};
		mDrawerToggle.setDrawerIndicatorEnabled(true);
		mDrawerLayout.setDrawerListener(mDrawerToggle);
		
		mDrawer = (FrameLayout) findViewById(R.id.main_drawer);
		
		mLocateMe = (ImageButton) findViewById(R.id.main_imagebutton_locateuser);
		mYankToggle = (ImageButton) findViewById(R.id.main_imagebutton_yank);
		mTalk = (CircleButton) findViewById(R.id.main_circlebutton_alert);
		mTrack = (CircleButton) findViewById(R.id.main_circlebutton_track);
		mReport = (CircleButton) findViewById(R.id.main_circlebutton_report);
		mConnectionTicker = (TickerTextSwitcher) findViewById(R.id.main_ticker);
		mTalkOptionsDialog = new TalkOptionsDialog();
		mTalkOptionsListener = new TalkOptionsListener() {
			
			@Override
			public void onOptionSelect(int option) {
				switch (option) {
				case TalkOptionsDialog.OPTION_ORG:
					
					if (mUserBelongsToAgency) {
						mEmergencyManager.startNow(EmergencyManager.TYPE_START_REQUESTED);
						UiUtils.startActivityNoStack(MainActivity.this, AlertActivity.class);
					} else {
						UiUtils.MakePhoneCall(MainActivity.this,
								EmergencyManagerUtils.getEmergencyNumber(MainActivity.this));
					}
					break;
				case TalkOptionsDialog.OPTION_911:
					UiUtils.MakePhoneCall(MainActivity.this,
							EmergencyManagerUtils.getEmergencyNumber(MainActivity.this));
					break;
				case TalkOptionsDialog.OPTION_CHAT:
					UiUtils.startActivityNoStack(MainActivity.this, ChatActivity.class);
					break;
				}
			}
		};
		
		mEmergencyManager = EmergencyManager.getInstance(this);
		mJavelin = JavelinClient.getInstance(this, TapShieldApplication.JAVELIN_CONFIG);
		mTracker = LocationTracker.getInstance(this);
		mYank = YankManager.get(this);
		mEntourageManager = EntourageManager.get(this);
		mConnectionMonitor = ConnectivityMonitor.getInstance(this);
		mConnectionMonitorListener = new ConnectivityMonitorListener() {
			
			@Override
			public void onChanged(boolean connected, int reason, String systemReason) {
				mConnectionTicker.clearText();
				if (!connected) {
					mConnectionTicker.addText("No Internet Connection");
					if (reason == ConnectivityMonitor.REASON_UNKNOWN && systemReason != null) {
						mConnectionTicker.addText(String.format("(%s)", systemReason));
					} else if (reason == ConnectivityMonitor.REASON_RADIO_OFF) {
						mConnectionTicker.addText("Phone Service Unavailable");
					} else if (reason == ConnectivityMonitor.REASON_DATA_DISABLED) {
						mConnectionTicker.addText("Mobile Data Disabled");
					}
				}
			}
		};
		
		mConnectionTicker.set(3000);
		mConnectionTicker.setBackgroundColor(getResources().getColor(R.color.ts_alert_red));
		
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
		
		mYankToggle.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//toggle yank, i.e. set enable if disabled
				mYank.setEnabled(mYank.isDisabled());
			}
		});
		
		mTrack.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//Skip selection activities if Entourage is already set (running)
				
				/*
				Class<? extends Activity> clss = EntourageManager.get(MainActivity.this).isSet() ?
						PickArrivalContacts.class : PickDestinationActivity.class;
				Intent activity = new Intent(MainActivity.this, clss);
				startActivity(activity);
				*/
				
				startActivity(
						new Intent(MainActivity.this, EntourageDestinationActivity.class));
			}
		});
		
		mLocateMe.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				setTrackUser(true);
				moveCameraToUser(true);
				clearDecluster();
			}
		});
		
		mTalk.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				if (!mTalkOptionsDialog.isVisible()) {
					mTalkOptionsDialog.show(MainActivity.this);
				}
			}
		});
		
		mReport.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent reporting = new Intent(MainActivity.this, ReportListActivity.class);
				startActivity(reporting);
			}
		});
		
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
			//load all map-related except for entourage
			loadMapSettings();
			loadAgencyBoundaries();
			loadAgencyLogo();
			
			//if entourage is not set, load entourage and related views
			//meaning there is no need to wait until map loads to display routes, lock drawer, etc
			if (!mEntourageManager.isSet()) {
				loadEntourage();
			}

			//two cluster managers for the independent clustering between spotcrime pins and
			// social crime pins
			
			mMap.setInfoWindowAdapter(new CrimeInfoWindowAdapter(this));

			mMapCrimeClusterManager = new ClusterManager<CrimeClusterItem>(this, mMap);
			mMapCrimeClusterManager.setAlgorithm(
					new PreCachingAlgorithmDecorator<CrimeClusterItem>(
							new NonHierarchicalDistanceBasedAlgorithm<CrimeClusterItem>()));
			mMapCrimeClusterManager.setRenderer(
					new CrimeMapClusterRenderer(this, mMap, mMapCrimeClusterManager));
			mMapCrimeClusterManager.setOnClusterClickListener(
					new ClusterManager.OnClusterClickListener<CrimeClusterItem>() {

						@Override
						public boolean onClusterClick(Cluster<CrimeClusterItem> cluster) {
							//returning true means it has to be declustered
							if (focusOnCluster(cluster)) {
								declusterCrimeCluster(cluster);
							}
							return true;
						}
					});
			mMapCrimeClusterManager.setOnClusterItemInfoWindowClickListener(
					new ClusterManager.OnClusterItemInfoWindowClickListener<CrimeClusterItem>() {

						@Override
						public void onClusterItemInfoWindowClick(CrimeClusterItem item) {
							getCrimeDetails(item.getCrime());
						}
					});
			
			mMapSocialCrimeClusterManager = new ClusterManager<SocialCrimeClusterItem>(this, mMap);
			mMapSocialCrimeClusterManager.setAlgorithm(
					new PreCachingAlgorithmDecorator<SocialCrimeClusterItem>(
							new NonHierarchicalDistanceBasedAlgorithm<SocialCrimeClusterItem>()));
			mMapSocialCrimeClusterManager.setRenderer(
					new SocialCrimeMapClusterRenderer(this, mMap, mMapSocialCrimeClusterManager));
			mMapSocialCrimeClusterManager.setOnClusterClickListener(
					new ClusterManager.OnClusterClickListener<SocialCrimeClusterItem>() {

						@Override
						public boolean onClusterClick(Cluster<SocialCrimeClusterItem> cluster) {
							//returning true means it has to be declustered
							if (focusOnCluster(cluster)) {
								declusterSocialCrimeCluster(cluster);
							}
							return true;
						}
					});
			mMapSocialCrimeClusterManager.setOnClusterItemInfoWindowClickListener(
					new ClusterManager.OnClusterItemInfoWindowClickListener<SocialCrimeClusterItem>() {

						@Override
						public void onClusterItemInfoWindowClick(SocialCrimeClusterItem item) {
							getSocialCrimeDetails(item.getSocialCrime());
						}
					});
			
			mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
				
				@Override
				public void onCameraChange(CameraPosition cameraPosition) {
					removeDeclusteredGroup(cameraPosition);
					mMapCrimeClusterManager.onCameraChange(cameraPosition);
					mMapSocialCrimeClusterManager.onCameraChange(cameraPosition);
				}
			});
			
			mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
				
				@Override
				public boolean onMarkerClick(Marker marker) {
					//return true to the handle of the event of at least one, false otherwise
					return mMapCrimeClusterManager.onMarkerClick(marker)
							|| mMapSocialCrimeClusterManager.onMarkerClick(marker);
				}
			});
			
			mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
				
				@Override
				public void onInfoWindowClick(Marker marker) {

					if (mDeclusteredCrimeMap.containsKey(marker)) {
						getCrimeDetails(mDeclusteredCrimeMap.get(marker));
					} else if (mDeclusteredSocialCrimeMap.containsKey(marker)) {
						getSocialCrimeDetails(mDeclusteredSocialCrimeMap.get(marker));
					}
					
					mMapCrimeClusterManager.onInfoWindowClick(marker);
					mMapSocialCrimeClusterManager.onInfoWindowClick(marker);
				}
			});
			
			mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
				
				@Override
				public void onMapLoaded() {
					//just load if set, if not, the rest has been loaded at onPostCreate()
					if (mEntourageManager.isSet()) {
						loadEntourage();
					}
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
		
		mTalkOptionsDialog.addListener(mTalkOptionsListener);
		
		mConnectionMonitor.addListener(mConnectionMonitorListener);
		
		IntentFilter filter = new IntentFilter(JavelinUserManager.ACTION_AGENCY_LOGOS_UPDATED);
		registerReceiver(mLogoUpdatedReceiver, filter);
		
		mYank.setListener(this);
		setYankToggleIcon();
		
		final JavelinUserManager userManager = mJavelin.getUserManager();
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
			//at this point let the SessionManager class deal with what's missing
			SessionManager.getInstance(this).check(this);
			
			mUserBelongsToAgency = userManager.getUser().belongsToAgency();
			
			//enable/disable chat button if part or not of an organzation
			mTalkOptionsDialog.setOptionEnable(TalkOptionsDialog.OPTION_CHAT, mUserBelongsToAgency);
			
			if (getIntent() != null && getIntent().getBooleanExtra(EXTRA_DISCONNECTED, false)) {
				mDisconnectedDialog = getDisconnectedDialog();
				mDisconnectedDialog.show();
			}
		}
		
		mTracker.start();
		mTracker.addLocationListener(this);
		
		boolean locationServicesEnabled = UiUtils.checkLocationServicesEnabled(this);
		
		if (!locationServicesEnabled) {
			return;
		}
		
		mCrimesHandler.post(mSpotCrimesUpdater);
		mCrimesHandler.post(mSocialCrimesUpdater);
		
		if (EmergencyManagerUtils.isRealEmergencyActive(mEmergencyManager)) {
			Intent alert = new Intent(this, AlertActivity.class);
			startActivity(alert);
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();

		mTalkOptionsDialog.removeListener(mTalkOptionsListener);
		
		mConnectionMonitor.removeListener(mConnectionMonitorListener);
		
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
			/*
			 * placeholder for eventual entourage (people) menu item to display right-side drawer
			 * 
			int menuRes = R.menu.entourage_people_placeholder;
			getMenuInflater().inflate(menuRes, menu);
			*/
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
		}
		return false;
	}
	
	@Override
	public void onBackPressed() {
		if (mDrawerLayout.isDrawerOpen(mDrawer)) {
			mDrawerLayout.closeDrawer(mDrawer);
		} else {
			super.onBackPressed();
		}
	}
	
	private void setYankToggleIcon() {
		int resId = mYank.isEnabled() ?
				R.drawable.ic_actionbar_yank : R.drawable.ic_actionbar_yank_disabled;
		mYankToggle.setImageResource(resId);
	}
	
	private void setTrackUser(boolean enabled) {
		mTrackUser = enabled;
		int iconResource = mTrackUser ? R.drawable.ts_icon_locateme : R.drawable.ts_icon_locateme_disabled;
		mLocateMe.setImageResource(iconResource);
	}
	
	private void animateZoomInCameraTo(double latitude, double longitude, float newZoom) {
		LatLng to = new LatLng(latitude, longitude);
		CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(to, newZoom);
		mMap.animateCamera(cameraUpdate, 500, null);
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
	
	private void loadEntourage() {

		boolean entourageSet = mEntourageManager.isSet();
		
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(!entourageSet);
		actionBar.setHomeButtonEnabled(!entourageSet);
		actionBar.setTitle(R.string.ts_screen_home);
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
			setTrackUser(false);
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
		
		SpotCrimeRequest request =
				new SpotCrimeRequest(TapShieldApplication.SPOTCRIME_CONFIG,
						mUserLocation.getLatitude(), mUserLocation.getLongitude(),
						TapShieldApplication.SPOTCRIME_RADIUS)
				.setSince(mSpotCrimeSince)
				.setSortBy(SpotCrimeRequest.SORT_BY_DATE)
				.setSortOrder(SpotCrimeRequest.SORT_ORDER_DESCENDING)
				.setMaxRecords(mSpotCrimeNumRecords);
		
		SpotCrimeCallback callback = new SpotCrimeCallback() {
			
			@Override
			public void onRequest(boolean ok, List<Crime> results, String errorIfNotOk) {
				
				mSpotCrimeError = !ok;
				
				if (ok) {
					
					if (isFinishing() || results == null || mMap == null) {
						return;
					}
					
					/*
					make a wide search if
					-first fetch (current list empty and no previous wide search), and
					-fetched less than 'spotcrime records min'
					*/
					if (mSpotCrimeRecords.isEmpty() && !mSpotCrimeWideSearch
							&& results.size() < TapShieldApplication.SPOTCRIME_RECORDS_MIN) {
						
						//avoid further assigning the same values, set 'wide search' flag to true
						mSpotCrimeWideSearch = true;
						
						//to make wide search, from now on set 'since' flag for 'spotcrime extra days'
						mSpotCrimeSince = new DateTime()
								.minusDays(TapShieldApplication.SPOTCRIME_EXTRA_PERIOD_DAYS)
								.getMillis();
						//to make wide search, from now on set 'max' for 'spotcrime extra records max'
						mSpotCrimeNumRecords = TapShieldApplication.SPOTCRIME_EXTRA_RECORDS_MAX;
						
						//call a new search (to use wide search params)
						//  and return before handling results
						loadNearbySpotCrime();
						return;
					}
					
					handleSpotCrimeResults(results);
				}
			}

			@Override
			public void onDetail(boolean ok, Crime crime, String errorIfNotOk) {}
		};
		
		SpotCrimeClient spotCrime = SpotCrimeClient.getInstance(TapShieldApplication.SPOTCRIME_CONFIG);
		spotCrime.request(request, callback);
	}
	
	private void handleSpotCrimeResults(List<Crime> results) {
		DateTime limit = new DateTime(mSpotCrimeSince);
		
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
				mMapCrimeClusterManager.addItem(new CrimeClusterItem(crime));
			}
		}
		
		//remove old ones
		/*
		for (Crime crime : mSpotCrimeRecords.values()) {
			boolean old = SpotCrimeUtils.getDateTimeFromCrime(crime).isBefore(limit);
			
			if (old) {
				mMapCrimeClusterManager.removeItem(new CrimeClusterItem(crime));
				
				//remove stored record
				if (mSpotCrimeRecords.containsKey(crime.getId())) {
					mSpotCrimeRecords.remove(crime.getId());
				}
			}
		}
		*/
		
		mMapCrimeClusterManager.cluster();
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
						//.minusHours(TapShieldApplication.CRIMES_PERIOD_HOURS);
						.minusDays(14);

				//add new ones (records and markers)
				for (SocialCrime crime : socialCrimes.getSocialCrimes()) {
					boolean old = crime.getDate().isBefore(limit);

					//add non-duplicates and ones within the timeframe

					if (!old && !mSocialCrimesRecords.containsKey(crime.getUrl())) {
						mSocialCrimesRecords.put(crime.getUrl(), crime);;
						mMapSocialCrimeClusterManager.addItem(new SocialCrimeClusterItem(crime));
					}
				}

				//remove old ones (markers and records)
				/*
				for (SocialCrime crime : mSocialCrimesRecords.values()) {
					boolean old = crime.getDate().isBefore(limit);

					if (old) {
						mMapSocialCrimeClusterManager.removeItem(new SocialCrimeClusterItem(crime));
						
						//remove stored record
						if (mSocialCrimesRecords.containsKey(crime.getUrl())) {
							mSocialCrimesRecords.remove(crime.getUrl());
						}
					}
				}
				*/
				
				mMapSocialCrimeClusterManager.cluster();
			}

			@Override
			public void onDetails(boolean ok, int code, SocialCrime socialCrime,
					String errorIfNotOk) {}

			@Override
			public void onDelete(boolean ok, int code, SocialCrime socialCrime,
					String errorIfNotOk) {}
		};
		
		JavelinSocialReportingManager socialReporting = mJavelin.getSocialReportingManager();
		socialReporting.getReportsAt(mUserLocation.getLatitude(), mUserLocation.getLongitude(),
				TapShieldApplication.SOCIAL_CRIMES_RADIUS, callback);
	}
	
	private void drawUser() {
		MapUtils.displayUserPositionWithAccuracy(this, mMap, mUserLocation.getLatitude(),
				mUserLocation.getLongitude(), mUserLocation.getAccuracy());
		
		if (mTrackUser) {
			//do not animate if it is just resuming
			moveCameraToUser(!mResuming);
		}
	}
	
	private void getCrimeDetails(Crime crime) {
		Intent details = new Intent(MainActivity.this, ReportDetailsActivity.class);
		details.putExtra(ReportDetailsActivity.EXTRA_REPORT_TYPE,
				ReportDetailsActivity.TYPE_SPOTCRIME);
		details.putExtra(ReportDetailsActivity.EXTRA_REPORT_ID,
				crime.getId());
		startActivity(details);
	}
	
	private void getSocialCrimeDetails(SocialCrime socialCrime) {
		Intent details = new Intent(MainActivity.this, ReportDetailsActivity.class);
		details.putExtra(ReportDetailsActivity.EXTRA_REPORT_TYPE,
				ReportDetailsActivity.TYPE_SOCIALCRIME);
		details.putExtra(ReportDetailsActivity.EXTRA_REPORT_ID,
				socialCrime.getUrl());
		startActivity(details);
	}
	
	private boolean focusOnCluster(Cluster<? extends ClusterItem> cluster) {
		
		clearDecluster();
		
		//stop tracking user
		setTrackUser(false);
		
		float zoom = mMap.getCameraPosition().zoom;
		float zoomMax = mMap.getMaxZoomLevel();
		
		if (zoom >= zoomMax) {
			//if max level, decluster group
			return true;
		} else {
			//else:
			// get the remaining zoom levels to reach max, get part of it (e.g. a fourth of it)
			// increase to 1 of floored down to 0
			// add remaining value to current zoom value
			// exception: if zoom difference is about 'difToFullZoom' and
			//   cluster contains items with the same position, then decluster
			final float difToFullZoom = 3;
			
			float zoomDif = zoomMax - zoom;
			float zoomNew;
			
			//exception
			if (zoomDif <= difToFullZoom) {
				zoomNew = zoomMax;
			} else {
			
				int zoomDifPart = (int) (Math.floor(zoomDif) / 4f);
				
				if (zoomDifPart <= 0) {
					zoomDifPart = 1;
				}
				zoomNew = zoom + zoomDifPart;
			}
			animateZoomInCameraTo(cluster.getPosition().latitude,
					cluster.getPosition().longitude, zoomNew);
		}
		return false;
	}
	
	private void removeDeclusteredGroup(final CameraPosition camera) {
		if (!mDeclusteredGroup || mDeclusteredCameraPosition == null) {
			return;
		}
		
		Location clusterPosition = new Location("");
		clusterPosition.setLatitude(mDeclusteredCameraPosition.target.latitude);
		clusterPosition.setLongitude(mDeclusteredCameraPosition.target.longitude);
		
		Location cameraPosition = new Location("");
		cameraPosition.setLatitude(camera.target.latitude);
		cameraPosition.setLongitude(camera.target.longitude);
		
		boolean clusterFarFromCamera = clusterPosition.distanceTo(cameraPosition) >= 25;
		
		//remove declustered items if user wants to be tracked, zoom changed, or distance
		//  of the cluster is over X amount
		if (mTrackUser
				|| camera.zoom != mDeclusteredCameraPosition.zoom
				|| clusterFarFromCamera) {
			clearDecluster();
		}
	}
	
	private void setDecluster(final int size) {
		mDeclusteredGroup = true;
		mDeclusteredCameraPosition = mMap.getCameraPosition();
		mDeclusteredMarkers = new Marker[size];
	}
	
	private void clearDecluster() {
		mDeclusteredCameraPosition = null;
		mDeclusteredGroup = false;
		if (mDeclusteredMarkers != null) {
			for (Marker m : mDeclusteredMarkers) {
				m.remove();
			}
		}
		
		if (mDeclusteredCrimeMap != null) {
			mDeclusteredCrimeMap.clear();
		}
		
		if (mDeclusteredSocialCrimeMap != null) {
			mDeclusteredSocialCrimeMap.clear();
		}
	}
	
	private void declusterSocialCrimeCluster(Cluster<SocialCrimeClusterItem> cluster) {
		
		//get size and pass all cluster items to array for iteration
		final int size = cluster.getSize();
		final SocialCrimeClusterItem[] items = new SocialCrimeClusterItem[size];
		cluster.getItems().toArray(items);

		//initialize decluster flags
		setDecluster(size);
		
		//get final variables to be used
		final float radius = DECLUSTER_RADIUS;
		final float eachAngle = 360f/(float)size;
		
		
		for (int i = 0; i < cluster.getSize(); i++) {
			
			//build marker options, set radial position, create marker via map and store reference
			
			MarkerOptions defaultOptions = SocialReportsUtils.getMarkerOptionsOf(this,
					items[i].getSocialCrime(), false);
			
			defaultOptions.position(
					getCircleLatLngWithAngle(eachAngle * i, cluster.getPosition(), radius));
			
			mDeclusteredMarkers[i] = mMap.addMarker(defaultOptions);
			mDeclusteredSocialCrimeMap.put(mDeclusteredMarkers[i], items[i].getSocialCrime());
		}
	}
	
	private void declusterCrimeCluster(Cluster<CrimeClusterItem> cluster) {

		//get size and pass all cluster items to array for iteration
		final int size = cluster.getSize();
		final CrimeClusterItem[] items = new CrimeClusterItem[size];
		cluster.getItems().toArray(items);

		//initialize decluster flags
		setDecluster(size);
		
		//get final variables to be used
		final float radius = DECLUSTER_RADIUS;
		final float eachAngle = 360f/(float)size;

		for (int i = 0; i < cluster.getSize(); i++) {

			//build marker options, set radial position, create marker via map and store reference

			MarkerOptions defaultOptions = SpotCrimeUtils.getMarkerOptionsOf(this,
					items[i].getCrime(), false);

			LatLng pos = getCircleLatLngWithAngle(eachAngle * i, cluster.getPosition(), radius);

			defaultOptions.position(pos);

			mDeclusteredMarkers[i] = mMap.addMarker(defaultOptions);
			mDeclusteredCrimeMap.put(mDeclusteredMarkers[i], items[i].getCrime());
		}
	}
	
	private LatLng getCircleLatLngWithAngle(float angle, LatLng center, float radius) {
		//lat:y, lon:x (y uses sin, x uses cos)
		float radians = (float) (angle * Math.PI / 180f);
		return new LatLng(
				center.latitude + radius * Math.sin(radians),
				center.longitude + radius * Math.cos(radians));
	}
	
	private AlertDialog getYankDialog() {
		return new AlertDialog.Builder(this)
				.setCancelable(true)
				.setTitle(R.string.ts_main_dialog_yank_title)
				.setMessage(R.string.ts_main_dialog_yank_message)
				.setNegativeButton(R.string.ts_common_cancel, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mYankDialog.cancel();
					}
				})
				.setOnCancelListener(new DialogInterface.OnCancelListener() {
					
					@Override
					public void onCancel(DialogInterface dialog) {
						mYank.setEnabled(false);
					}
				})
				.create();
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
		
		Log.i("tapshield", "Yank status=" + newStatus);
		
		setYankToggleIcon();
		
		switch (newStatus) {
		case YankManager.Status.DISABLED:
			UiUtils.toastShort(this, getString(R.string.ts_main_toast_yank_disabled));
			break;
		case YankManager.Status.WAITING_HEADSET:
			mYankDialog.dismiss();
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
