package com.tapshield.android.service;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.location.LocationListener;
import com.tapshield.android.location.LocationTracker;
import com.tapshield.android.manager.EntourageManager;
import com.tapshield.android.manager.EntourageManager.Listener;

public class EntourageArrivalCheckService extends Service implements LocationListener, Listener {

	private static final float ACCURACY_MINIMUM = 200f;
	private static final float DISTANCE_MINIMUM_FOR_ALERT = 500f;
	private EntourageManager mEntourage;
	private LocationTracker mTracker;
	private Location mDestination;
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
		Log.i("aaa", "eacs create");
		super.onCreate();
		mTracker = LocationTracker.getInstance(this);
		mTracker.addLocationListener(this);
		
		mEntourage = EntourageManager.get(this);
		
		mDestination = new Location(EntourageManager.class.getSimpleName());
		mDestination.setLatitude(mEntourage.getRoute().endLat());
		mDestination.setLongitude(mEntourage.getRoute().endLon());
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i("aaa", "eacs start");
		mEntourage.stop();
		mTracker.start();
		return START_STICKY;
	}

	@Override
	public void onLocationChanged(Location location) {
		Log.i("aaa", "eacs location changed w acc=" + location.getAccuracy());
		
		if (location.getAccuracy() <= ACCURACY_MINIMUM) {

			mTracker.removeLocationListener(this);
			mTracker.stop();
			
			if (location != null && mDestination != null) {
				float distance = location.distanceTo(mDestination);
				
				Log.i("aaa", "eacs distance=" + distance + " min=" + DISTANCE_MINIMUM_FOR_ALERT);

				if (distance >= DISTANCE_MINIMUM_FOR_ALERT) {
					mEntourage.addListener(this);
					mEntourage.messageMembers("User has not reached their destination at the estimated time of arrival.");
					mEntourage.notifyUserMissedETA();
				}
			}
		}
	}
	
	@Override
	public void onMessageSent(final boolean ok, final String message, final String errorIfNotOk) {
		if (ok) {
			mEntourage.removeListener(this);
			stopSelf();
		} else {
			mEntourage.messageMembers(message);
		}
	}
}
