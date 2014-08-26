package com.tapshield.android.manager;

import java.util.List;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.LocationListener;
import com.tapshield.android.R;
import com.tapshield.android.api.JavelinAlertManager;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.model.Agency;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.location.LocationTracker;
import com.tapshield.android.manager.TwilioManager.OnStatusChangeListener;
import com.tapshield.android.manager.TwilioManager.Status;
import com.tapshield.android.service.AlertIdUpdateService;
import com.tapshield.android.service.ChatUpdateService;
import com.tapshield.android.ui.activity.MainActivity;
import com.tapshield.android.ui.activity.OutOfBoundsActivity;
import com.tapshield.android.utils.GeoUtils;
import com.tapshield.android.utils.HardwareUtils;
import com.tapshield.android.utils.NetUtils;
import com.tapshield.android.utils.UiUtils;

public class EmergencyManager implements LocationListener, OnStatusChangeListener {
	
	public static final String ACTION_EMERGENCY = "com.tapshield.android.action.EMERGENCY";
	public static final String ACTION_EMERGENCY_SUCCESS = "com.tapshield.android.action.EMERGENCY_SUCCESS";
	public static final String ACTION_EMERGENCY_COMPLETE = "com.tapshield.android.action.EMERGENCY_COMPLETE";
	public static final String ACTION_TWILIO_FAILED = "com.tapshield.android.action.TWILIO_FAILED";
	
	public static final String EXTRA_TYPE = "com.tapshield.android.manager.EmergencyManager.TYPE";
	public static final int TYPE_TIMER_AUTO = 0;
	public static final int TYPE_TIMER_SLIDER = 1;
	public static final int TYPE_START_DELAYED = 2;
	public static final int TYPE_START_REQUESTED = 3;
	public static final int TYPE_HEADSET_UNPLUGGED = 4;
	public static final int TYPE_CHAT = 5;
	
	private static enum InternalStatus {
		INITIALIZED,
		IDLE,
		SCHEDULED,
		STARTED
	}
	
	private static EmergencyManager mInstance;
	private Context mContext;
	private AlarmManager mAlarmManager;
	private TwilioManager mTwilio;
	private JavelinClient mJavelin;
	private JavelinAlertManager mJavelinAlert;
	private JavelinAlertManager.OnDispatcherAlertedListener mDispatcherAlertedListener;
	private LocationTracker mTracker;
	private Location mLatestLocation;
	private Agency mAgency;
	
	private int mType, mAlertIdUpdaterRetries = 0, mTwilioRetries = 0, mTwilioMaxRetries;
	private long mScheduledAt;
	private long mScheduledFor;
	private PendingIntent mBroadcastPendingIntent, mChatUpdaterPendingIntent, mAlertIdUpdaterPendingIntent;
	private boolean mCompleted = false, mAlerted = false, mCheckingConnection = false,
			mTwilioFailed = false, mTwilioFailurePrompt = false;
	private List<Location> mAgencyBoundaries;
	
	//keeps check whether the object was just initialized or not
	private InternalStatus mStatus = InternalStatus.INITIALIZED;
	//private boolean mNotified = false;
	
	private EmergencyManager(Context context) {
		mContext = context.getApplicationContext();
		mAlarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
		mTwilio = TwilioManager.getInstance(mContext);
		mTracker = LocationTracker.getInstance(mContext);
		mJavelin = JavelinClient.getInstance(mContext, TapShieldApplication.JAVELIN_CONFIG);
		mJavelinAlert = mJavelin.getAlertManager();
		Intent chatUpdater = new Intent(mContext, ChatUpdateService.class);
		mChatUpdaterPendingIntent = PendingIntent.getService(context, 1, chatUpdater, 0);
		Intent alertIdUpdater = new Intent(mContext, AlertIdUpdateService.class);
		mAlertIdUpdaterPendingIntent = PendingIntent.getService(context, 2, alertIdUpdater, 0);
		mDispatcherAlertedListener = new JavelinAlertManager.OnDispatcherAlertedListener() {
			
			@Override
			public void onDispatcherAlerted(Exception e) {
				if (e == null) {
					mAlerted = true;
					//notify any listeners about the started emergency via broadcast
					Intent actionEmergencyStarted = new Intent(ACTION_EMERGENCY_SUCCESS);
					mContext.sendBroadcast(actionEmergencyStarted);
					unscheduleAlertIdUpdater();
				} else {
					mJavelinAlert.create(mType, mLatestLocation);
				}
			}
		};
		
		mTwilioMaxRetries = mContext.getResources()
				.getInteger(R.integer.emergency_twilio_number_auto_retries);
	}
	
	public static EmergencyManager getInstance(Context context) {
		if (mInstance == null) {
			mInstance = new EmergencyManager(context);
		}
		
		if (mInstance.mStatus == InternalStatus.INITIALIZED) {
			mInstance.onInitialized();
			mInstance.mStatus = InternalStatus.IDLE;
		}
		return mInstance;
	}
	
	protected void onInitialized() {}
	
	public void start(long millisInTheFuture, int type) {
		Log.i("tapshield", "EmergencyManager start argument:" + type);
		
		mType = type;
		mScheduledAt = SystemClock.elapsedRealtime();
		mScheduledFor = mScheduledAt + millisInTheFuture;
		
		Intent actionIntent = new Intent(ACTION_EMERGENCY);
		actionIntent.putExtra(EXTRA_TYPE, mType);
		mBroadcastPendingIntent = PendingIntent.getBroadcast(mContext, 0, actionIntent, 0);
		mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, mScheduledFor, mBroadcastPendingIntent);
		
		mTracker.start();
		mTracker.addLocationListener(this);
		HardwareUtils.vibrate(mContext, new long[]{300,700}, 0);
		
		mStatus = InternalStatus.SCHEDULED;
	}

	public void startNow(int type) {
		if (!isRunning()) {
			//nullify if it's been called immediately! (example, no broadcast used == ! running) to avoid old
			// pendingintent.
			mBroadcastPendingIntent = null;
		}
		
		//set flag to true so any location updates will not affect behavior until reachability verified 
		mCheckingConnection = true;
		final ServerReacherListener listener = new ServerReacherListener() {

			@Override
			public void onFinish(boolean reachable) {
				//if reachable just set conn-related to false and let handling of latest data
				if (reachable) {
					mCheckingConnection = false;
					handleLatestLocation();
				} else {
					cancel();
					Bundle extras = new Bundle();
					extras.putBoolean(MainActivity.EXTRA_DISCONNECTED, true);
					UiUtils.startActivityNoStack(mContext, MainActivity.class, extras);
				}
			}
		};
		
		new ServerReacher(listener).execute(TapShieldApplication.JAVELIN_CONFIG.getBaseUrl());
		
		mType = type;
		Log.i("tapshield", "EmergencyManager startEmergency called with " + mType);
	
		mJavelinAlert.setOnDispatcherAlertedListener(mDispatcherAlertedListener);

		try {
			mAgency = mJavelin.getUserManager().getUser().agency;
			mAgencyBoundaries = mAgency.getBoundaries();
		} catch (NullPointerException e) {
			cancel();
			Intent home = new Intent(mContext, MainActivity.class);
			home.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
			mContext.startActivity(home);
			Toast.makeText(mContext,
					"There was an issue with the your data (or the organization\'s)," +
					" please try again.", Toast.LENGTH_LONG).show();
			return;
		}
		
		startTracker();
		HardwareUtils.vibrateStop(mContext);

		mAlerted = false;
		mStatus = InternalStatus.STARTED;

		mTwilioFailed = false;
		mTwilioFailurePrompt = false;
		mTwilioRetries = 0;
		mTwilio.addOnStatusChangeListener(this);
		
		handleLatestLocation();
		
		scheduleChatUpdater();
		scheduleAlertIdUpdater();
	}
	
	public void cancel() {
		mStatus = InternalStatus.IDLE;
		mAlerted = false;
		
		if (mBroadcastPendingIntent != null) {
			mAlarmManager.cancel(mBroadcastPendingIntent);
		}

		Notifier.getInstance(mContext).dismiss(Notifier.NOTIFICATION_TWILIO_FAILURE);
		mTwilio.removeOnStatusChangeListener(this);
		
		mTwilio.hangUp();
		
		mJavelinAlert.removeOnDispatcherAlertedListener(mDispatcherAlertedListener);
		mJavelinAlert.cancel();
		
		mScheduledAt = mScheduledFor = 0;
		
		mJavelin.getChatManager().notifyEnd();
		
		stopTracker();
		
		HardwareUtils.vibrateStop(mContext);
		HardwareUtils.toggleSpeakerphone(mContext, false);
		
		unscheduleChatUpdater();
		unscheduleAlertIdUpdater();
		
		mCompleted = false;
		
		mLatestLocation = null;
		
		/*
		 * IS THIS GOING TO BE NECESSARY?
		if (App.IN_APP_EMERGENCY_DIALOG_SHOWED) {
			App.IN_APP_EMERGENCY_DIALOG_SHOWED = false;
		}
		*/
	}
	
	public long getElapsed() {
		return SystemClock.elapsedRealtime() - mScheduledAt; 
	}
	
	public long getDuration() {
		return mScheduledFor - mScheduledAt;
	}
	
	public long getRemaining() {
		return mScheduledFor - SystemClock.elapsedRealtime();
	}
	
	public boolean isRunning() {
		return (mStatus == InternalStatus.SCHEDULED || mStatus == InternalStatus.STARTED);
	}
	
	public boolean isScheduled() {
		return mStatus == InternalStatus.SCHEDULED;
	}
	
	public boolean isTransmitting() {
		return mStatus == InternalStatus.STARTED;
	}
	
	public boolean isNotified() {
		return mAlerted && isTransmitting();
	}
	
	public boolean isCompleted() {
		return mCompleted;
	}
	
	private void startTracker() {
		mTracker.removeLocationListener(this);
		mTracker.addLocationListener(this);
		mTracker.start();
	}
	
	private void stopTracker() {
		mTracker.removeLocationListener(this);
		mTracker.stop();
	}
	
	public void notifyCompletion() {
		if (mCompleted) {
			return;
		}
		
		mCompleted = true;
		
		//notify any interested party
		Intent completion = new Intent(ACTION_EMERGENCY_COMPLETE);
		mContext.sendBroadcast(completion);
	}

	
	public void scheduleChatUpdater() {
		long interval = mContext.getResources().getInteger(R.integer.chat_periodic_check_millis);
		long at = SystemClock.elapsedRealtime() + interval;
		
		mAlarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
				at, interval, mChatUpdaterPendingIntent);
	}
	
	private void unscheduleChatUpdater() {
		mAlarmManager.cancel(mChatUpdaterPendingIntent);
	}
	
	private void scheduleAlertIdUpdater() {
		long interval = mContext.getResources()
				.getInteger(R.integer.emergency_alert_id_periodic_check_millis);
		long at = SystemClock.elapsedRealtime() + interval;
		
		mAlarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
				at, interval, mAlertIdUpdaterPendingIntent);
	}
	
	private void unscheduleAlertIdUpdater() {
		mAlarmManager.cancel(mAlertIdUpdaterPendingIntent);
		mAlertIdUpdaterRetries = 0;
	}
	
	public void notifyAlertIdUpdaterRetry() {
		if (!isRunning()) {
			unscheduleAlertIdUpdater();
			return;
		}
		
		mAlertIdUpdaterRetries++;
		
		int maxRetries = mContext.getResources()
				.getInteger(R.integer.emergency_alert_id_periodic_check_max_attempts);
		
		if (mAlertIdUpdaterRetries >= maxRetries) {
			UiUtils.startActivityNoStack(mContext, MainActivity.class);
			cancel();
			Notifier.getInstance(mContext).notify(Notifier.NOTIFICATION_FAILED_ALERT);
		}
	}
	
	@Override
	public void onLocationChanged(Location l) {
		mLatestLocation = l; 
		Log.i("tapshield", "EmergencyManager onLocationChanged: accuracy of " + mLatestLocation.getAccuracy()
				+ " m @ [" + mLatestLocation.getLatitude() + "," + mLatestLocation.getLongitude() +"]");
		handleLatestLocation();
	}
	
	private void handleLatestLocation() {
		if (!isRunning() || mLatestLocation == null || mCheckingConnection) {
			return;
		}

		//status flags
		boolean requestedCreation = isTransmitting();
		boolean created = mJavelinAlert.isRunning();
		boolean established = mJavelinAlert.isEstablished();

		//time and location-related flags
		float cutoff = mContext.getResources().getInteger(R.integer.location_cutoff_meters);
		float goodAccuracyMinimum = 
				mContext.getResources().getInteger(R.integer.location_accuracy_good_minimum_meters);
		
		boolean timeAcceptable = true;
		boolean inside = false;
		float distanceToBoundaries = Float.MAX_VALUE;
		
		if (mAgency == null && mJavelin.getUserManager().getUser().belongsToAgency()) {
			mAgency = mJavelin.getUserManager().getUser().agency;
		}
		
		//multiple geofence (via regions), otherwise, classic (simple/single geofence)
		if (mAgency.hasRegions()) {
			Log.i("javelin", "agency has regions, complex check");
			//* for the time being, time variable is also calculated via
			//  isLocationInsideRegions() method just below to do so with dispatch center
			//  of found region. Thus, time variable will stay true, and inside will go to false
			//  if no center is available *
			inside = GeoUtils.isLocationInsideAgency(mContext, mLatestLocation, mAgency);
			distanceToBoundaries = GeoUtils.minDistanceBetweenLocationAndRegions(mLatestLocation,
					mAgency.regions);
		} else {
			Log.i("javelin", "agency has no regions, simple check");
			inside = GeoUtils.isLocationInsideBoundaries(mContext, mLatestLocation,
					mAgencyBoundaries);
			distanceToBoundaries =
					GeoUtils.minDistanceBetweenLocationAndEdges(mLatestLocation, mAgencyBoundaries);
		}
		
		boolean thereIsOverhang = mLatestLocation.getAccuracy() > distanceToBoundaries;
		boolean goodAccuracy = mLatestLocation.getAccuracy() <= goodAccuracyMinimum;
		boolean insideForCutoff = (distanceToBoundaries <= cutoff && goodAccuracy) || (distanceToBoundaries > cutoff);
		
		//overhang is defined by the distance past the boundaries of the accuracy bubble
		//checks before creation of alert
		//	if it has an extremely high chance of being outside
		if (!created) {
			if (!timeAcceptable || (!inside && !thereIsOverhang)) {
				cancelAndWarn();
				return;
			}
				
		}

		//if alert is not requested to start via startNow() then return
		if (!requestedCreation) {
			return;
		}
		
		//if not created yet (but requested via startNow())
		if (!created) {
			//create only if inside AND insideForCutoff
			// 'insideForCutoff' is defined as at least ONE of the two:
			// 1. far enough of the boundaries (greater than cutoff distance), OR
			// 2. if within cutoff distance of the boundaries, with good accuracy
			if (inside && insideForCutoff) {
				callIfNotChat();
				mJavelinAlert.create(mType, mLatestLocation);
			} else {
				cancelAndWarn();
			}
			return;
		}

		if (established) {
			mJavelinAlert.update(mLatestLocation);
		}
	}
	
	public void requestRedial() {
		//method required making the phone call only if alert is being created or is already created
		boolean created = mJavelinAlert.isRunning();
		if (created) {
			mTwilioFailurePrompt = false;
			call();
		}
	}
	
	private void callIfNotChat() {
		//start twilio if an emergency other than chat
		if (mType != TYPE_CHAT) {
			call();
		}
	}
	
	private void call() {
		String phoneNumber = mJavelin.getUserManager().getUser().agency.primaryNumber;
		mTwilio.call(phoneNumber);
	}
	
	private void cancelAndWarn() {
		
		cancel();
		
		new CountDownTimer(500, 500) {
			
			@Override
			public void onTick(long millisUntilFinished) {}
			
			@Override
			public void onFinish() {
				UiUtils.startActivityNoStack(mContext, MainActivity.class);
				Intent oob = new Intent(mContext, OutOfBoundsActivity.class);
				oob.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				mContext.startActivity(oob);
				cancelRefMethod();
			}
		}.start();
	}
	
	//method to be called within CountDownTimer in cancelAndWarn() to avoid clash with its cancel()
	private void cancelRefMethod() {
		cancel();
	}
	
	public boolean isTwilioFailing() {
		return mTwilioFailed;
	}
	
	public void setTwilioFailureUserPrompted() {
		mTwilioFailurePrompt = true;
		Notifier.getInstance(mContext).dismiss(Notifier.NOTIFICATION_TWILIO_FAILURE);
	}
	
	public boolean isTwilioFailureUserPrompted() {
		return mTwilioFailurePrompt;
	}
	
	@Override
	public void onStatusChange(Status status) {
		if (status.equals(TwilioManager.Status.FAILED)) {
			
			Log.i("twilio", "manager detecting failure retries=" + mTwilioRetries + "/" + mTwilioMaxRetries);
			mTwilioRetries++;
			
			if (mTwilioRetries >= mTwilioMaxRetries) {
				Log.i("twilio", "failure set in place, broadcasting...");
				mTwilioFailed = true;
				Intent twilioFailureIntent = new Intent(ACTION_TWILIO_FAILED);
				Notifier.getInstance(mContext).notify(Notifier.NOTIFICATION_TWILIO_FAILURE);
				mContext.sendBroadcast(twilioFailureIntent);
			} else {
				Log.i("twilio", "requesting redial");
				requestRedial();
			}
		}
	}
	
	private class ServerReacher extends AsyncTask<String, String, Boolean> {

		private ServerReacherListener listener;
		private Thread timeout;
		
		public ServerReacher(ServerReacherListener l) {
			listener = l;
			timeout = new Thread() {
				@Override
				public void run() {
					try {
						Thread.sleep(12000);
					} catch (InterruptedException e) {}
					
					//if running, act as timeout and notify listener while cancelling asynctask
					if (getStatus() == AsyncTask.Status.RUNNING) {
						listener.onFinish(false);
						cancel(true);
					}
				}
			};
		}

		@Override
		protected void onPreExecute() {
			timeout.start();
			super.onPreExecute();
		}
		
		@Override
		protected Boolean doInBackground(String... names) {
			String name = names[0];
			long start = SystemClock.elapsedRealtime();
			boolean reachable = NetUtils.isSecureServerReachable(mContext, name);
			Log.i("tapshield-conn", "total time " + (SystemClock.elapsedRealtime() - start) + "ms");
			return reachable;
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			if (isCancelled()) {
				return;
			}
			
			if (this.listener != null) {
				listener.onFinish(result);
			}
		}
	}
	
	private interface ServerReacherListener {
		void onFinish(boolean reachable);
	}
}
