package com.tapshield.android.location;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.BatteryManager;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

public class LocationTracker
		implements GooglePlayServicesClient.ConnectionCallbacks, 
		GooglePlayServicesClient.OnConnectionFailedListener,
		LocationListener {

	private static final long INTERVAL_HIGH = 15000;
	private static final long INTERVAL_NORMAL = 20000;
	private static final long INTERVAL_LOW = 30000;
	private static final long INTERVAL_DEFAULT = 40000;

	//minimum battery % for each range
	private static final float BATTERY_LEVEL_HIGH = 50;
	private static final float BATTERY_LEVEL_NORMAL = 20;
	private static final float BATTERY_LEVEL_LOW = 10;

	private static LocationTracker mInstance;
	private Context mContext;

	private ArrayList<LocationListener> mListeners;

	private LocationClient mLocationClient;
	private LocationRequest mLocationRequest;

	private BroadcastReceiver mBatteryWatcher;

	private boolean mServicesAvailable = false;
	private long mInterval = INTERVAL_DEFAULT;
	private Location mLatestLocation;

	private LocationTracker(Context context) {
		mContext = context;
		mListeners = new ArrayList<LocationListener>();

		mBatteryWatcher = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
				int max = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
				float percent = 100 * ((float) level / (float) max);

				long latestInterval = INTERVAL_DEFAULT;

				if (percent >= BATTERY_LEVEL_HIGH) {
					latestInterval = INTERVAL_HIGH;
				} else if (percent >= BATTERY_LEVEL_NORMAL) {
					latestInterval = INTERVAL_NORMAL;
				} else if (percent >= BATTERY_LEVEL_LOW) {
					latestInterval = INTERVAL_LOW;
				}

				if (mInterval != latestInterval) {
					mInterval = latestInterval;
					mLocationRequest.setInterval(mInterval);
					mLocationRequest.setFastestInterval(mInterval);
					requestUpdates();
				}
			}
		};

		mLocationClient = new LocationClient(mContext, this, this);
		mLocationRequest = LocationRequest.create();
		mLocationRequest
			.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
			.setInterval(INTERVAL_DEFAULT)
			.setFastestInterval(INTERVAL_DEFAULT);

		mServicesAvailable = servicesAvailable();
	}

	public static LocationTracker getInstance(Context context) {
		if (mInstance == null) {
			mInstance = new LocationTracker(context.getApplicationContext());
		}
		return mInstance;
	}

	private boolean servicesAvailable() {
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(mContext);

		if (ConnectionResult.SUCCESS == resultCode) {
			return true;
		} else {
			return false;
		}
	}

	public void start() {
		if (mServicesAvailable && !(mLocationClient.isConnected() || mLocationClient.isConnecting())) {
			mLocationClient.connect();

			IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
			mContext.registerReceiver(mBatteryWatcher, filter);
		}
	}

	private void requestUpdates() {
		if (mLocationClient.isConnected()) {
			mLocationClient.requestLocationUpdates(mLocationRequest, this);
		}
	}

	public void stop() {
		if (mServicesAvailable && mLocationClient.isConnected()) {
			mLocationClient.disconnect();

			mContext.unregisterReceiver(mBatteryWatcher);
		}
	}

	public void addLocationListener(LocationListener locationListener) {
		if (mLatestLocation != null) {
			locationListener.onLocationChanged(mLatestLocation);
		}
		mListeners.add(locationListener);
	}

	public void removeLocationListener(LocationListener locationListener) {
		mListeners.remove(locationListener);
	}

	@Override
	public void onConnectionFailed(ConnectionResult result) {}

	@Override
	public void onConnected(Bundle bundle) {
		requestUpdates();
	}

	@Override
	public void onDisconnected() {
		mLocationClient.removeLocationUpdates(this);
		mLatestLocation = null;
	}

	@Override
	public void onLocationChanged(Location location) {
		mLatestLocation = location;
		for (LocationListener listener : mListeners) {
			listener.onLocationChanged(location);
		}
	}
}
