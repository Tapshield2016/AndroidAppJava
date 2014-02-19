package com.tapshield.android.service;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinUserManager;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.manager.EmergencyManager;

public class AlertIdUpdateService extends IntentService {

	public AlertIdUpdateService() {
		super(AlertIdUpdateService.class.toString());
	}

	@Override
	protected void onHandleIntent(Intent i) {
		Log.i("tapshield", "AlertIdUpdateService checking for Alert ID and reporting findings.");

		JavelinClient javelin = JavelinClient.getInstance(AlertIdUpdateService.this,
				TapShieldApplication.JAVELIN_CONFIG);
		String alertId = JavelinUserManager.syncGetActiveAlertForLoggedUser(AlertIdUpdateService.this);
		
		if (alertId != null && alertId.trim().length() > 0) {
			javelin.getAlertManager().notifyId(alertId);
		} else {
			EmergencyManager.getInstance(AlertIdUpdateService.this).notifyAlertIdUpdaterRetry();
		}
	}

}
