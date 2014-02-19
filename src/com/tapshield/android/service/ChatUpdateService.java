package com.tapshield.android.service;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.app.TapShieldApplication;

public class ChatUpdateService extends IntentService {

	public ChatUpdateService() {
		super(ChatUpdateService.class.toString());
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.i("tapshield", "ChatUpdateService requesting chat manager to check for new messages.");
		JavelinClient javelin = JavelinClient.getInstance(ChatUpdateService.this,
				TapShieldApplication.JAVELIN_CONFIG);
		javelin.getChatManager().fetchMessagesSinceLastCheck();
	}

}
