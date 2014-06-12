package com.tapshield.android.api.spotcrime;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import com.tapshield.android.api.JavelinComms;
import com.tapshield.android.api.JavelinComms.JavelinCommsCallback;
import com.tapshield.android.api.JavelinComms.JavelinCommsRequestResponse;
import com.tapshield.android.api.spotcrime.model.Crime;

public class SpotCrimeClient {

	public static final String TYPE_ARREST = "arrest";
	public static final String TYPE_ARSON = "arson";
	public static final String TYPE_ASSAULT = "assault";
	public static final String TYPE_BURGLARY = "burglary";
	public static final String TYPE_OTHER = "other";
	public static final String TYPE_ROBBERY = "robbery";
	public static final String TYPE_SHOOTING = "shooting";
	public static final String TYPE_THEFT = "theft";
	public static final String TYPE_VANDALISM = "vandalism";
	
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
				
				String error = new String();
				
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
				} else {
					if (response.exception.getMessage().contains("errors")) {
						try {
							JSONObject responseObject = new JSONObject(response.exception.getMessage());
							
							if (!responseObject.has("errors")) {
								throw new Exception();
							}
							
							JSONArray errors = responseObject.getJSONArray("errors");
							
							if (errors.length() == 0) {
								throw new Exception();
							}
							
							for (int i=0; i < errors.length(); i++) {
								if (!error.isEmpty()) {
									error = error.concat(",");
								}
								
								error = error.concat(errors.getString(i));
							}
						} catch (Exception e) {
							error = response.exception.getMessage();
						}
					}
				}
				
				callback.onRequest(response.successful, listCrimes, error);
			}
		};
		
		JavelinComms.httpGet(url, null, null, request.getParams(), internalCallback); 
	}
	
	public void details(final int crimeId, final SpotCrimeCallback callback) {
		JavelinCommsCallback internalCallback = new JavelinComms.JavelinCommsCallback() {
			
			@Override
			public void onEnd(JavelinCommsRequestResponse response) {
				
				String error = new String();
				Crime crime = null;
				
				if (response.successful) {
					try {
						crime = Crime.deserialize(response.jsonResponse);
					} catch (Exception e) {
						error = e.getMessage();
					}
				} else {
					error = response.exception.getMessage();
				}
				
				callback.onDetail(response.successful, crime, error);
			}
		};
		
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair(SpotCrimeRequest.PARAM_KEY, mConfig.getKey()));
		
		JavelinComms.httpGet(mConfig.getDetailsUrl(crimeId), null, null, params, internalCallback);
	}
	
	public interface SpotCrimeCallback {
		void onRequest(boolean ok, List<Crime> results, String errorIfNotOk);
		void onDetail(boolean ok, Crime crime, String errorIfNotOk);
	}
}
