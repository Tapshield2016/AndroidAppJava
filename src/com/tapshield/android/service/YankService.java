package com.tapshield.android.service;

import com.tapshield.android.manager.YankManager;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class YankService extends Service {

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		YankManager
				.get(this)
				.register();
		return START_STICKY;
	}
	
	@Override
	public IBinder onBind(Intent i) {
		return null;
	}
}
