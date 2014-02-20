package com.tapshield.android.utils;

import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.client.methods.HttpHead;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class NetUtils {

	public static final boolean isSecureServerReachable(Context context, String name) {
		//connected to network
		ConnectivityManager manager = (ConnectivityManager)
				context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetwork = manager.getActiveNetworkInfo();

		//continue if at least connecting
		if (activeNetwork == null || !activeNetwork.isConnectedOrConnecting()) {
			Log.e("tapshield-conn", "Server reachability failed, no connection to a network");
			return false;
		}

		Log.d("tapshield-conn", "activeNetwork=" + activeNetwork.getTypeName());

		//can make connection
		HttpsURLConnection conn = null;
		boolean reachable = false;
		try {
			URL url = new URL(name);
			conn = (HttpsURLConnection) url.openConnection();
			conn.setRequestMethod(HttpHead.METHOD_NAME);
			conn.setConnectTimeout(6000);
			conn.setReadTimeout(6000);
			conn.connect();
		} catch (Exception e) {
			Log.e("tapshield-conn", "Server reachability failed", e);
			reachable = false;
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
			reachable = true;
		}
		Log.i("tapshield-conn", "Server reachable");
		return reachable;
	}
}
