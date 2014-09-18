package com.tapshield.android.api.googleplaces;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.tapshield.android.api.JavelinComms;
import com.tapshield.android.api.JavelinComms.JavelinCommsCallback;
import com.tapshield.android.api.JavelinComms.JavelinCommsRequestResponse;
import com.tapshield.android.api.googleplaces.model.Place;

public class GooglePlacesClient {

	private static final String JSON_RESULTS = "results";
	
	private static GooglePlacesClient mInstance;
	private GooglePlacesConfig mConfig;
	
	public static GooglePlacesClient get(GooglePlacesConfig config) {
		if (mInstance == null) {
			mInstance = new GooglePlacesClient(config);
		}
		
		return mInstance;
	}
	
	private GooglePlacesClient(GooglePlacesConfig config) {
		mConfig = config;
	}
	
	public final void request(final GooglePlacesRequest request, final GooglePlacesListener l) {
		JavelinCommsCallback internalCallback = new JavelinCommsCallback() {
			
			@Override
			public void onEnd(JavelinCommsRequestResponse response) {
				List<Place> places = null;
				String error = null;
				
				if (response.successful) {
					try {
						places = new ArrayList<Place>();
						
						JSONObject jsonResponse = response.jsonResponse;
						JSONArray results = jsonResponse.getJSONArray(JSON_RESULTS);
						
						Gson gson = new Gson();
						
						for (int i = 0; i < results.length(); i++) {
							JSONObject jsonPlace = results.getJSONObject(i);
							Place place = gson.fromJson(jsonPlace.toString(), Place.class);
							places.add(place);
						}
					} catch (Exception e) {
						places = null;
						error = e.toString();
					}
				} else {
					error = response.response;
				}
				
				l.onFinish(response.successful, places, error);
			}
		};
		
		String url = mConfig.url() + request.getUrlSuffix();
		JavelinComms.httpGet(url, null, null, null, internalCallback);
	}
	
	public interface GooglePlacesListener {
		void onFinish(boolean ok, List<Place> places, String errorIfNotOk);
	}
}
