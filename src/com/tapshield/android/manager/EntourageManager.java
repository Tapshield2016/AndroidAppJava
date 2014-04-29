package com.tapshield.android.manager;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;

import com.google.android.gms.maps.GoogleMap;
import com.google.gson.Gson;
import com.tapshield.android.api.googledirections.model.Route;
import com.tapshield.android.receiver.EntourageReceiver;


public class EntourageManager {

	private static final String PREFERENCES = "com.tapshield.android.preferences.entourage";
	private static final String KEY_SET = "com.tapshield.android.preferences.key.set";
	private static final String KEY_ROUTE = "com.tapshield.android.preferences.key.route";
	private static final String KEY_START = "com.tapshield.android.preferences.key.startat";

	private static final float ARRIVE_BUFFER_FACTOR = 0.0f;
	
	private static EntourageManager mIt = null;
	
	private Context mContext;
	private SharedPreferences mPreferences;
	private AlarmManager mAlarmManager;
	private Route mRoute;
	private long mStartAt;
	private boolean mSet;
	
	public static EntourageManager get(Context context) {
		if (mIt == null) {
			mIt = new EntourageManager(context);
		}
		return mIt;
	}
	
	private EntourageManager(Context c) {
		mContext = c.getApplicationContext();
		mPreferences = mContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
		mAlarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
		load();
	}
	
	private void load() {
		mSet = mPreferences.getBoolean(KEY_SET, false);
		if (mSet) {
			Gson gson = new Gson();
			mRoute = gson.fromJson(mPreferences.getString(KEY_ROUTE, null), Route.class);
			//load startAt here, setFlags() method will set the rest with this and the route
			mStartAt = mPreferences.getLong(KEY_START, 0);
			setFlags(mRoute);
		}
	}
	
	private void save() {
		SharedPreferences.Editor editor = mPreferences.edit();
		Gson gson = new Gson();
		editor.putBoolean(KEY_SET, mSet);
		editor.putString(KEY_ROUTE, gson.toJson(mRoute));
		editor.putLong(KEY_START, mStartAt);
		editor.apply();
	}
	
	private void scheduleAlertIn(long inMillseconds) {
		mAlarmManager.set(
				AlarmManager.ELAPSED_REALTIME_WAKEUP,
				SystemClock.elapsedRealtime() + inMillseconds,
				getPendingIntent());
	}
	
	private void unscheduledAlert() {
		mAlarmManager.cancel(getPendingIntent());
	}
	
	private PendingIntent getPendingIntent() {
		Intent receiver = new Intent(mContext, EntourageReceiver.class);
		PendingIntent operation = PendingIntent.getBroadcast(mContext, 1, receiver, 0);
		return operation;
	}
	
	public void start(Route r) {
		if (isSet()) {
			return;
		}
		//preset startAt here, setFlags() method will set the rest with this and the route
		mStartAt = System.currentTimeMillis();
		setFlags(r);
		long durationMilli = mRoute.durationSeconds() * 1000;
		long extraBuffer = (long) (durationMilli * ARRIVE_BUFFER_FACTOR);
		scheduleAlertIn(durationMilli + extraBuffer);
		save();
	}
	
	private void setFlags(Route r) {
		mSet = true;
		mRoute = r;
	}
	
	public void stop() {
		if (!isSet()) {
			return;
		}
		
		mSet = false;
		mRoute = null;
		mStartAt = 0;
		unscheduledAlert();
		save();
	}
	
	public boolean isSet() {
		return mSet;
	}
	
	public Route getRoute() {
		return mRoute;
	}
	
	public void drawOnMap(GoogleMap m) {
		//draw and position all entourage-specific elements in the map
	}
	
	public void notifyReceiverTriggered(Intent intent) {
		int type = intent.getIntExtra(EmergencyManager.EXTRA_TYPE, EmergencyManager.TYPE_START_REQUESTED);
		EmergencyManager manager = EmergencyManager.getInstance(mContext);
		manager.startNow(type);
	}
}
