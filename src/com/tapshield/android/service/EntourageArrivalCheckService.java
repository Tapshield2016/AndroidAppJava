package com.tapshield.android.service;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.location.LocationListener;
import com.google.gson.Gson;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.googledirections.model.Route;
import com.tapshield.android.api.model.User;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.location.LocationTracker;
import com.tapshield.android.manager.EntourageManager;
import com.tapshield.android.manager.EntourageManager.Listener;
import com.tapshield.android.manager.EntourageManager.SyncStatus;

public class EntourageArrivalCheckService extends Service implements LocationListener, Listener {

	private static final float ACCURACY_MINIMUM = 200f;
	private static final float DISTANCE_MINIMUM_FOR_ALERT = 200f;
	private boolean mCheckRunning = false;
	private int mSuccessfulMessages;
	private int mNumMessages;
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i("aaa", "service started");
		mCheckRunning = false;
		LocationTracker tracker = LocationTracker.getInstance(this);
		tracker.addLocationListener(this);
		tracker.start();
		
		return START_STICKY;
	}

	@Override
	public void onLocationChanged(Location location) {
		
		EntourageManager entourage = EntourageManager.get(this);
		
		Log.i("aaa", "service loc changed=" + location + " checkRunning=" + mCheckRunning);
		
		if (location.getAccuracy() <= ACCURACY_MINIMUM && !mCheckRunning && entourage.isSet()) {
			mCheckRunning = true;
			LocationTracker tracker = LocationTracker.getInstance(this);
			tracker.removeLocationListener(this);
			tracker.stop();
			mSuccessfulMessages = 0;

			
			Route r = entourage.getRoute();
			Log.i("aaa", "SERVICE R=" + new Gson().toJson(r).toString());
			Log.i("aaa", "SERVICE end=" + r.endLat() + "," + r.endLon());
			
			
			Log.i("aaa", "service loc=" + location + " dest=" + r.endLat() + "," + r.endLon());
			
			if (location != null) {
				float[] results = new float[1];
				
				Location.distanceBetween(
						location.getLatitude(),
						location.getLongitude(),
						r.endLat(),
						r.endLon(),
						results);
				float distance = results[0];
				
				Log.i("aaa", "service distance=" + distance);

				boolean destinationReached = distance < DISTANCE_MINIMUM_FOR_ALERT;

				entourage.addListener(this);
				
				User user = JavelinClient
						.getInstance(this, TapShieldApplication.JAVELIN_CONFIG)
						.getUserManager()
						.getUser();
				
				String name = user.firstName + " " + user.lastName;
				String destination = entourage.getRoute().endAddress();

				if (destinationReached) {
					mNumMessages = 1;
					String message = name + " has arrived at " + destination;
					entourage.messageMembers(message);
				} else {
					mNumMessages = 2;
					String firstMessage = name + " has not made it to "
							+ destination + ", within their estimated time of arrival.";
					String secondMessage = name + "\'s latest location"
							+ " http://maps.google.com/maps"
							+ "?q=" + location.getLatitude() + "," + location.getLongitude();
					
					entourage.messageMembers(firstMessage);
					entourage.messageMembers(secondMessage);
					entourage.notifyUserMissedETA();
				}
				
				entourage.stop();
			}
		}
	}
	
	@Override
	public void onMessageSent(final boolean ok, final String message, final String errorIfNotOk) {
		
		EntourageManager entourage = EntourageManager.get(this);
		LocationTracker tracker = LocationTracker.getInstance(this);
		
		Log.i("aaa", "service onmessagesent ok=" + ok + " m=" + (ok ? message : errorIfNotOk));
		if (ok) {
			mSuccessfulMessages++;
			
			if (mSuccessfulMessages >= mNumMessages) {
				tracker.removeLocationListener(this);
				entourage.removeListener(this);
				stopSelf();
			}
		} else {
			entourage.messageMembers(message);
		}
	}

	@Override
	public void onStatusChange(SyncStatus status, String extra) {}
}
