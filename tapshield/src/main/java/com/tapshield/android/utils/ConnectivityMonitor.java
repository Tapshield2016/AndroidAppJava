package com.tapshield.android.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;

public class ConnectivityMonitor {

	public static final int REASON_UNKNOWN = 0;
	public static final int REASON_DATA_DISABLED = 1;
	public static final int REASON_RADIO_OFF = 2;
	
	private static final String CONTAINS_DATA = "data";
	private static final String CONTAINS_RADIO = "radio";
	
	private static ConnectivityMonitor mInstance;
	private Context mContext;
	private boolean mConnected;
	private boolean mFirst;
	private BroadcastReceiver mReceiver;
	private List<ConnectivityMonitorListener> mListeners;
	
	public static ConnectivityMonitor getInstance(Context context) {
		if (mInstance == null) {
			mInstance = new ConnectivityMonitor(context);
		}
		
		return mInstance;
	}
	
	private ConnectivityMonitor(Context context) {
		mContext = context.getApplicationContext();
		mListeners = new ArrayList<ConnectivityMonitorListener>();
		mReceiver = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				//inverse extra boolean value
				boolean connected = !intent.getBooleanExtra(
						ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
				
				//ignore if same status is reported as long as it is not the first one
				if (mConnected == connected && !mFirst) {
					return;
				}
				
				if (mFirst) {
					mFirst = false;
				}
				
				mConnected = connected;
				
				int reasonFlag = REASON_UNKNOWN;
				String reason = intent.getStringExtra(ConnectivityManager.EXTRA_REASON);
				
				if (!mConnected) {
					
					if (reason != null 
							&& reason.toLowerCase(Locale.getDefault()).contains(CONTAINS_RADIO)) {
						
						reasonFlag = REASON_RADIO_OFF;
					} else if (reason != null
							&& reason.toLowerCase(Locale.getDefault()).contains(CONTAINS_DATA)) {
						
						reasonFlag = REASON_DATA_DISABLED;
					}
				}
				
				for (ConnectivityMonitorListener l : mListeners) {
					l.onChanged(mConnected, reasonFlag, reason);
				}
			}
		};
	}

	private void register() {
		//next report will be the first one since register() is called to receive updates 
		mFirst = true;
		IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
		try {
			mContext.registerReceiver(mReceiver, filter);
		} catch (Exception e) {}
	}
	
	private void unregister() {
		try {
			mContext.unregisterReceiver(mReceiver);
		} catch (Exception e) {}
	}
	
	public void addListener(ConnectivityMonitorListener l) {
		if (!mListeners.contains(l)) {
			mListeners.add(l);
			
			//register receiver if first listener came
			if (mListeners.size() == 1) {
				register();
			}
		}
	}
	
	public void removeListener(ConnectivityMonitorListener l) {
		//unregister receiver if last listener left
		if (mListeners.remove(l) && mListeners.isEmpty()) {
			unregister();
		}
	}
	
	public interface ConnectivityMonitorListener {
		void onChanged(final boolean connected, final int reason, final String systemReason);
	}
}
