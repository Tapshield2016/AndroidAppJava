package com.tapshield.android.service;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;

import com.google.android.gms.location.LocationListener;
import com.tapshield.android.R;
import com.tapshield.android.location.LocationTracker;
import com.tapshield.android.manager.EmergencyManager;
import com.tapshield.android.manager.EntourageManager;
import com.tapshield.android.manager.EntourageManager.Listener;
import com.tapshield.android.ui.activity.AlertActivity;
import com.tapshield.android.ui.activity.MainActivity;

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
		if (location.getAccuracy() <= ACCURACY_MINIMUM
				&& location.distanceTo(mDestination) >= DISTANCE_MINIMUM_FOR_ALERT) {
			report();
			EmergencyManager
					.getInstance(this)
					.start(getResources().getInteger(R.integer.timer_emergency_requested_millis),
							EmergencyManager.TYPE_START_REQUESTED);
			
			Intent home = new Intent(this, MainActivity.class);
			home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			home.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			
			Intent alert = new Intent(this, AlertActivity.class);
			alert.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			alert.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

			Intent[] fullAlert = new Intent[] {home, alert};
			
			startActivities(fullAlert);
		}
	}
	
	private void report() {
		mTracker.removeLocationListener(this);
		mTracker.stop();
		mEntourage.addListener(this);
		mEntourage.messageMembers("User has not reached their destination at the estimated time of arrival.");
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
