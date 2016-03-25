package com.tapshield.android.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.tapshield.android.manager.EmergencyManager;

public class EmergencyReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i("tapshield", "EmergencyReceiver.onReceive()");
		int type = intent.getIntExtra(EmergencyManager.EXTRA_TYPE, EmergencyManager.TYPE_START_REQUESTED);
		EmergencyManager manager = EmergencyManager.getInstance(context);
		manager.startNow(type);
	}
}
