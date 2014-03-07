package com.tapshield.android.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinUserManager;
import com.tapshield.android.api.JavelinUserManager.OnTwilioTokenFetchListener;
import com.tapshield.android.app.TapShieldApplication;
import com.twilio.client.Connection;
import com.twilio.client.ConnectionListener;
import com.twilio.client.Device;
import com.twilio.client.Twilio;

public class TwilioManager
		implements Twilio.InitListener, ConnectionListener, OnTwilioTokenFetchListener {

	public enum Status {
		DISABLED,
		IDLE,
		CONNECTING,
		BUSY,
		FAILED
	}
	
	private static final String PARAM_TO = "To";
	
	private static TwilioManager mInstance;
	private static String mToken;
	
	private static boolean mCallAfterInitializing;
	private static String mCallPhoneNumber;
	
	private Context mContext;
	private JavelinUserManager mUserManager;
	
	private Device mDevice;
	private Connection mConnection;
	private Status mStatus = Status.IDLE;
	private List<OnStatusChangeListener> mListeners;
	
	private long mCallStartedAt;
	
	private boolean mCallAttemptedAtLeastOnce = false;
	
	public static TwilioManager getInstance(Context c) {
		if (mInstance == null) {
			mInstance = new TwilioManager(c);
		}
		return mInstance;
	}
	
	private TwilioManager(Context c) {
		mContext = c.getApplicationContext();
		JavelinClient javelin = JavelinClient.getInstance(mContext, TapShieldApplication.JAVELIN_CONFIG);
		mUserManager = javelin.getUserManager();
		mListeners = new ArrayList<OnStatusChangeListener>();
		mCallAfterInitializing = false;
	}
	
	private void initialize() {
		boolean isInitialized = Twilio.isInitialized();
		if (!isInitialized) {
			Twilio.initialize(mContext, this);
		} else {
			//since intialized, any request dependes on calling onInitialized to created a Device
			// with right token, setters, and call if necessary
			onInitialized();
		}
	}
	
	@Override
	public void onInitialized() {
		mDevice = Twilio.createDevice(mToken, null);
		mDevice.setDisconnectSoundEnabled(true);
		mDevice.setOutgoingSoundEnabled(true);
		
		if (mCallAfterInitializing) {
			mCallAfterInitializing = false;
			internalCall(mCallPhoneNumber);
		}
	}
	
	@Override
	public void onError(Exception e) {
		Log.e("twilio", "Error initializing Twilio", e);
		setStatus(Status.FAILED);
		setStatus(Status.DISABLED);
	}
	
	@Override
	protected void finalize() throws Throwable {
		if (mDevice != null) {
			mDevice.release();
		}
		if (mConnection != null) {
			mConnection.disconnect();
			mConnection = null;
		}
		setStatus(Status.IDLE);
		mCallAttemptedAtLeastOnce = false;
	}
	
	public void call(String phoneNumber) {
		if (getStatus().equals(Status.CONNECTING) || getStatus().equals(Status.BUSY)) {
			Log.w("twilio", "Client busy, request ignored.");
			return;
		}
		
		if (phoneNumber == null || phoneNumber.trim().length() == 0) {
			Log.e("twilio", "Phone number is null or empty, aborting call.");
			setStatus(Status.FAILED);
		}
		
		setStatus(Status.CONNECTING);
		
		mCallPhoneNumber = phoneNumber;
		mCallAfterInitializing = true;
		mCallAttemptedAtLeastOnce = true;
		
		mUserManager.getTwilioTokenForLoggedUser(this);
	}
	
	private void internalCall(String phoneNumber) {
		if (mDevice == null) {
			Log.e("twilio", "Device is null, aborting call");
			setStatus(Status.FAILED);
			return;
		}

		Map<String, String> params = new HashMap<String, String>();
		params.put(PARAM_TO, phoneNumber);
		
		mConnection = mDevice.connect(params, this);
		setStatus(Status.CONNECTING);
		
		if (mConnection == null) {
			Log.e("twilio", "Failed to connect");
			setStatus(Status.FAILED);
			setStatus(Status.IDLE);
		}
	}
	
	public void hangUp() {
		if (mConnection != null) {
			mConnection.disconnect();
			mConnection = null;
		}
		setStatus(Status.IDLE);
	}

	@Override
	public void onTwilioTokenFetch(boolean successful, String token, Throwable e) {
		if (successful && token != null && token.trim().length() > 0) {
			mToken = token;
			initialize();
		} else {
			Log.e("twilio", "Error retrieving capability token", e);
			setStatus(Status.FAILED);
			setStatus(Status.DISABLED);
		}
	}
	
	private void setStatus(Status s) {
		if (mStatus == s) {
			return;
		}
		
		if (s.equals(Status.BUSY)) {
			mCallStartedAt = SystemClock.elapsedRealtime();
		}
		
		mStatus = s;
		notifyStatusChange();
	}
	
	public Status getStatus() {
		return mStatus;
	}
	
	public long getCallStartedAt() {
		return mCallStartedAt;
	}
	
	public void notifyEnd() {
		mCallAttemptedAtLeastOnce = false;
	}
	
	public boolean wasCallAttempted() {
		return mCallAttemptedAtLeastOnce;
	}
	
	private void notifyStatusChange() {
		if (mListeners == null) {
			return;
		}
		
		for (OnStatusChangeListener l : mListeners) {
			if (l != null) {
				l.onStatusChange(mStatus);
			}
		}
	}
	
	public void addOnStatusChangeListener(OnStatusChangeListener l) {
		if (l != null) {
			mListeners.add(l);
			l.onStatusChange(mStatus);
		}
	}
	
	public boolean removeOnStatusChangeListener(OnStatusChangeListener l) {
		if (l != null) {
			return mListeners.remove(l);
		}
		return false;
	}
	
	public static interface OnStatusChangeListener {
		void onStatusChange(Status status);
	}

	@Override
	public void onConnected(Connection c) {
		setStatus(Status.BUSY);
	}

	@Override
	public void onConnecting(Connection c) {
		setStatus(Status.CONNECTING);
	}

	@Override
	public void onDisconnected(Connection c) {
		setStatus(Status.IDLE);
	}

	@Override
	public void onDisconnected(Connection c, int errorCode, String errorMessage) {
		Log.w("twilio", "Disconnected due to (" + errorCode + ")" + " " + errorMessage);
		setStatus(Status.FAILED);
		setStatus(Status.IDLE);
	}
}
