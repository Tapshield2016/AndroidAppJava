package com.tapshield.android.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinUserManager;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.manager.EmergencyManager;
import com.tapshield.android.ui.activity.AlertActivity;
import com.tapshield.android.ui.activity.MainActivity;
import com.tapshield.android.utils.EmergencyManagerUtils;

public class PhoneCallReceiver extends BroadcastReceiver {

	private static final String TAG = "tapshield";
	
	private static String mPhoneNumber;
	private static String mState;
	private static long mStartTime = -1;

	@Override
	public void onReceive(Context context, Intent intent) {

		Log.i(TAG, "action=" + intent.getAction());
		
		JavelinUserManager userManager = JavelinClient
				.getInstance(context, TapShieldApplication.JAVELIN_CONFIG)
				.getUserManager();

		//no org? no need to track emergency call, cannot notify dispatchers
		if (!userManager.isPresent() || !userManager.getUser().belongsToAgency()) {
			return;
		}
		
		if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
			mPhoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
		} else {
			mState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
		}

		Log.i(TAG, "phone='" + mPhoneNumber + "' state=" + mState);
		
		//do not continue until we get all info
		if (mPhoneNumber == null || mState == null) {
			return;
		}
		
		//i.e. 911
		String emergNumber = EmergencyManagerUtils.getEmergencyNumber(context);
		Log.i(TAG, "emergNum='" + emergNumber);
		
		boolean isEmergNumber = mPhoneNumber.equals(emergNumber);
		boolean idle = mState.equals(TelephonyManager.EXTRA_STATE_IDLE);
		boolean ongoing = mState.equals(TelephonyManager.EXTRA_STATE_OFFHOOK);

		//no need to continue if it is not an emergency number
		if (!isEmergNumber) {
			return;
		}
		
		Log.i(TAG, Boolean.toString(isEmergNumber));
		
		/*
		 * scenarios for tracking emergency calls to notify dispatchers if possible
		 * 1. call ended/idle? is a 911 alert running? patch/update with call_length with seconds
		 * 2. call made to 911? create 911 alert if no alert is running, ignore otherwise 
		 */
		
		EmergencyManager manager = EmergencyManager.getInstance(context);

		Log.i(TAG, String.format("flags idle=%b ongoing=%b running=%b type=%d", idle, ongoing, manager.isRunning(), manager.getType()));
		
		//scenario 1
		if (idle
				&& manager.isRunning()
				&& manager.getType() == EmergencyManager.TYPE_911
				&& mStartTime > 0) {
			
			//milliseconds (long) to seconds (int)
			int callDuration = (int) ((SystemClock.elapsedRealtime() - mStartTime) / 1000);
			
			Log.i(TAG, "dur=" + callDuration);
			
			//no need to keep old data
			mPhoneNumber = mState = null;
			mStartTime = -1;
			
			manager.updateCallDuration(callDuration);
			
		//scenario 2
		} else if (ongoing
				&& !manager.isRunning()) {
			
			Log.i(TAG, "creating 911 alert");
			
			mStartTime = SystemClock.elapsedRealtime();
			
			manager.startNow(EmergencyManager.TYPE_911);
			
			/*
			if no activity is shown when yank is triggered, then it could lead to this stack: [A]
			A being alert, and then attempted to reposition main [M] and since there is any, would lead
			to [A, M] and when finishing M, A should pop-up again.
	
			Set back stack to re-position M back to the top clearing the bottom.
			*/

			Intent main = new Intent(context, MainActivity.class)
					.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

			Intent alert = new Intent(context, AlertActivity.class)
					.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

			Intent[] stack = new Intent[] {main, alert};

			context.startActivities(stack);
		}
	}
}
