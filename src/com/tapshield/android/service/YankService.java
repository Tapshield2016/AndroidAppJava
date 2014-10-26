package com.tapshield.android.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.tapshield.android.manager.YankManager;

public class YankService extends Service {

	private YankManager mYank;
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		mYank = YankManager.get(this);
		mYank.register();
		return START_STICKY;
	}
	
	@Override
	public IBinder onBind(Intent i) {
		return null;
	}
}
