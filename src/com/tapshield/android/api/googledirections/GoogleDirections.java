package com.tapshield.android.api.googledirections;

import android.util.Log;

import com.tapshield.android.api.JavelinComms;
import com.tapshield.android.api.JavelinComms.JavelinCommsCallback;
import com.tapshield.android.api.JavelinComms.JavelinCommsRequestResponse;

public class GoogleDirections {

	public static final void request(final GoogleDirectionsRequest request,
			final GoogleDirectionsListener l) {
		
		JavelinCommsCallback internalCallback = new JavelinCommsCallback() {
			
			@Override
			public void onEnd(JavelinCommsRequestResponse response) {
				Log.i("aaa", "ok=" + response.successful);
				Log.i("aaa", "response=" + response.response);
			}
		};
		
		Log.i("aaa", "directions url=" + request.url());
		JavelinComms.httpGet(request.url(), null, null, null, internalCallback);
	}
	
	public interface GoogleDirectionsListener {
	}
}
