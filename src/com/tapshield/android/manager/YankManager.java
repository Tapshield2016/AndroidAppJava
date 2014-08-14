package com.tapshield.android.manager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.service.YankService;
import com.tapshield.android.ui.activity.AlertActivity;
import com.tapshield.android.ui.activity.MainActivity;
import com.tapshield.android.utils.UiUtils;

public class YankManager {

	private static final String EXTRA_HEADSET_STATE = "state";
	private static final int HEADSET_STATE_OUT = 0;
	private static final int HEADSET_STATE_IN = 1;
	
	private static final String PREF_ENABLED = "com.tapshield.android.preferences.yank_status";
	private static final String PREF_ENABLEDRECENTLY = "com.tapshield.android.preferences.yank_enabledrecently";
	
	private static YankManager mInstance;
	
	private Context mContext;
	private BroadcastReceiver mReceiver;
	private Notifier mNotifier;
	private Intent mService;
	private SharedPreferences mPreferences;
	private int mStatus;
	private boolean mEnabledRecently = false;
	private YankListener mListener;
	
	private YankManager(Context c) {
		mContext = c.getApplicationContext();
		mNotifier = Notifier.getInstance(mContext);
		mService = new Intent(mContext, YankService.class);
		mPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
		load();
		
		mReceiver = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				int state = intent.getIntExtra(EXTRA_HEADSET_STATE, -1);
				
				if (state == HEADSET_STATE_IN) {
					headsetIn();
				} else if (state == HEADSET_STATE_OUT) {
					headsetOut();
				}
			}
		};
	}
	
	public static YankManager get(Context c) {
		
		if (mInstance == null) {
			mInstance = new YankManager(c);
		}
		
		return mInstance;
	}
	
	public void setEnabled(boolean toEnabled) {
		if ((toEnabled && isEnabled())
				|| (!toEnabled && isDisabled())) {
			notifyListener();
			return;
		}
		
		if (toEnabled) {
			mEnabledRecently = true;
			mContext.startService(mService);
		} else {
			unregister();
			mContext.stopService(mService);
			mNotifier.dismiss(Notifier.NOTIFICATION_YANK);
			setStatus(Status.DISABLED);
		}
	}

	public boolean isDisabled() {
		return getStatus() == Status.DISABLED;
	}
	
	public boolean isWaitingForHeadset() {
		return getStatus() == Status.WAITING_HEADSET;
	}
	
	public boolean isEnabled() {
		return getStatus() == Status.ENABLED;
	}

	public int getStatus() {
		return mStatus;
	}
	
	private void setStatus(int newStatus) {
		if (getStatus() != newStatus) {
			mStatus = newStatus;
			cache();
			
			if (getStatus() == Status.ENABLED) {
				mNotifier.notify(Notifier.NOTIFICATION_YANK);
			}
			
			notifyListener();
		}
	}
	
	private void load() {
		mStatus = mPreferences.getInt(PREF_ENABLED, 0);
		mEnabledRecently = mPreferences.getBoolean(PREF_ENABLEDRECENTLY, false);
	}
	
	private void cache() {
		mPreferences
				.edit()
				.putInt(PREF_ENABLED, mStatus)
				.putBoolean(PREF_ENABLEDRECENTLY, mEnabledRecently)
				.commit();
	}
	
	//public static method to be accessed by the service
	// which will keep it alive or recreate it if destroyed at any time
	public void register() {
		IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
		mContext.registerReceiver(mReceiver, filter);
	}
	
	//private since it is called internally, in contrast to the possible-often-called register from
	//	the service
	private void unregister() {
		mContext.unregisterReceiver(mReceiver);
	}
	
	private void headsetIn() {
		//if inserted then set setting flag to false (enabled recently)
		if (mEnabledRecently) {
			mEnabledRecently = false;
			setStatus(Status.ENABLED);
		}
	}
	
	private void headsetOut() {
		//ignore if it is first report after registering
		if (mEnabledRecently) {
			setStatus(Status.WAITING_HEADSET);
			return;
		}
		
		boolean orgPresent = JavelinClient.getInstance(mContext,
				TapShieldApplication.JAVELIN_CONFIG).getUserManager().getUser().belongsToAgency();
		
		if (orgPresent) {

			//alert, start alert activity, and disable yank
			long duration = (long) mContext
					.getResources()
					.getInteger(R.integer.timer_emergency_yank_millis);
			EmergencyManager
					.getInstance(mContext)
					.start(duration, EmergencyManager.TYPE_HEADSET_UNPLUGGED);

			/*
			if no activity is shown when yank is triggered, then it could lead to this stack: [A]
			A being alert, and then attempted to reposition main [M] and since there is any, would lead
			to [A, M] and when finishing M, A should pop-up again.
	
			Set back stack to re-position M back to the top clearing the bottom.
			*/

			Intent main = new Intent(mContext, MainActivity.class)
					.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

			Intent alert = new Intent(mContext, AlertActivity.class)
					.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

			Intent[] stack = new Intent[] {main, alert};

			mContext.startActivities(stack);
		} else {
			
			String defaultEmergencyNumber = mContext.getString(R.string.ts_no_org_emergency_number);
			UiUtils.MakePhoneCall(mContext, defaultEmergencyNumber);
		}
		
		setEnabled(false);
	}
	
	public void setListener(YankListener l) {
		mListener = l;
	}
	
	public void removeListener(YankListener l) {
		if (mListener.equals(l)) {
			mListener = null;
		}
	}
	
	private void notifyListener() {
		if (mListener != null) {
			mListener.onStatusChange(mStatus);
		}
	}
	
	public static class Status {
		public static final int DISABLED = 0;
		public static final int WAITING_HEADSET = 1;
		public static final int ENABLED = 2;
	}
	
	public interface YankListener {
		void onStatusChange(int newStatus);
	}
}
