package com.tapshield.android.api.spotcrime;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.tapshield.android.api.JavelinComms;
import com.tapshield.android.api.JavelinComms.JavelinCommsCallback;
import com.tapshield.android.api.JavelinComms.JavelinCommsRequestResponse;
import com.tapshield.android.api.spotcrime.model.Crime;

public class SpotCrimeClient {

	private static SpotCrimeClient mInstance;
	
	private SpotCrimeConfig mConfig;
	
	private SpotCrimeClient(SpotCrimeConfig config) {
		mConfig = config;
	}
	
	public static SpotCrimeClient getInstance(SpotCrimeConfig config) {
		if (mInstance == null) {
			mInstance = new SpotCrimeClient(config);
		}
		return mInstance;
	}
	
	public void request(final SpotCrimeRequest request, final SpotCrimeCallback callback) {
		final String url = mConfig.getUrl();
		
		JavelinCommsCallback internalCallback = new JavelinCommsCallback() {
			
			@Override
			public void onEnd(JavelinCommsRequestResponse response) {

				List<Crime> listCrimes = null;
				
				if (response.successful) {
					try {
						
						listCrimes = new ArrayList<Crime>();
						JSONArray crimes = response.jsonResponse.getJSONArray("crimes");
						for (int i = 0; i < crimes.length(); i++) {
							JSONObject crime = crimes.getJSONObject(i);
							listCrimes.add(Crime.deserialize(crime)); 
						}
					} catch (Exception e) {
						listCrimes = null;
					}
				}
				
				callback.onRequest(response.successful, listCrimes, response.exception.getMessage());
			}
		};
		
		JavelinComms.httpGet(url, null, null, request.getParams(), internalCallback); 
	}
	
	public interface SpotCrimeCallback {
		void onRequest(boolean ok, List<Crime> results, String errorIfNotOk);
	}
}
