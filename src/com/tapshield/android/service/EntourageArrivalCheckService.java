package com.tapshield.android.service;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;

import com.google.android.gms.location.LocationListener;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.model.User;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.location.LocationTracker;
import com.tapshield.android.manager.EntourageManager;
import com.tapshield.android.manager.EntourageManager.Listener;

public class EntourageArrivalCheckService extends Service implements LocationListener, Listener {

	private static final float ACCURACY_MINIMUM = 200f;
	private static final float DISTANCE_MINIMUM_FOR_ALERT = 200f;
	private EntourageManager mEntourage;
	private LocationTracker mTracker;
	private Location mDestination;
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
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
		mTracker.start();
		return START_STICKY;
	}

	@Override
	public void onLocationChanged(Location location) {
		
		if (location.getAccuracy() <= ACCURACY_MINIMUM) {

			mTracker.removeLocationListener(this);
			mTracker.stop();

			if (location != null && mDestination != null) {
				float distance = location.distanceTo(mDestination);

				User user = JavelinClient
						.getInstance(this, TapShieldApplication.JAVELIN_CONFIG)
						.getUserManager()
						.getUser();
				
				String name = user.firstName + " " + user.lastName;
				String destination = mEntourage.getRoute().endAddress();
				
				String message = name + " has not reached their destination("
						+ destination + ") at the estimated time of arrival."
						+ " They might need your help at"
						+ " http://maps.google.com/maps"
						+ "?q=" + location.getLatitude() + "," + location.getLongitude();
				
				if (distance >= DISTANCE_MINIMUM_FOR_ALERT) {
					mEntourage.addListener(this);
					mEntourage.messageMembers(message);
					mEntourage.notifyUserMissedETA();
				}
				mEntourage.stop();
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
