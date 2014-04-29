package com.tapshield.android.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.tapshield.android.manager.EntourageManager;

public class EntourageReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		EntourageManager
				.get(context)
				.notifyReceiverTriggered(intent);
	}
}
