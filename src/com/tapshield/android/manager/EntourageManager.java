package com.tapshield.android.manager;

import android.content.Context;

import com.google.android.gms.maps.GoogleMap;
import com.tapshield.android.api.googledirections.model.Route;


public class EntourageManager {

	private static EntourageManager mIt = null;
	
	private Context mContext;
	private String mDestination;
	private Route mRoute;
	private long mStartAt;
	private long mArriveAt;
	private long mArriveBuffer;
	private boolean mSet;
	
	public static EntourageManager get(Context context) {
		if (mIt == null) {
			mIt = new EntourageManager(context);
		}
		return mIt;
	}
	
	private EntourageManager(Context c) {
		mContext = c;
		load();
	}
	
	private void load() {
		
	}
	
	private void save() {
		
	}
	
	private void scheduleAlertAt(long at) {
		//schedule with the system's alarm manager
	}
	
	private void unscheduledAlert() {
		
	}
	
	public boolean isSet() {
		return mSet;
	}
	
	public Route getRoute() {
		return mRoute;
	}
	
	/*for start and stop, set:
	 * flag
	 * automatic alert
	 * 
	*/
	
	public void start(Route r) {
		if (isSet()) {
			return;
		}

		mSet = true;
		mRoute = r;
		mStartAt = System.currentTimeMillis();
		mArriveBuffer = 0; //extra time tbd - set by a % of the eta or something else
		mArriveAt = System.currentTimeMillis() + (mRoute.durationSeconds() * 1000) + mArriveBuffer;
		
		save();
	}
	
	public void stop() {
		if (!isSet()) {
			return;
		}
		
		mSet = false;
		mStartAt = mArriveAt = mArriveBuffer = 0;
		unscheduledAlert();
		
		save();
	}
	
	public void prepare(GoogleMap m) {
		//draw and position all entourage-specific elements in the map
	}
}
