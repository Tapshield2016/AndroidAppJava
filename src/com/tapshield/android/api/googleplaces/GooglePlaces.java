package com.tapshield.android.api.googleplaces;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.util.Log;

import com.google.gson.Gson;
import com.tapshield.android.api.JavelinComms;
import com.tapshield.android.api.JavelinComms.JavelinCommsCallback;
import com.tapshield.android.api.JavelinComms.JavelinCommsRequestResponse;
import com.tapshield.android.api.googleplaces.model.AutocompletePlace;
import com.tapshield.android.api.googleplaces.model.AutocompleteSearch;
import com.tapshield.android.api.googleplaces.model.NearbySearch;
import com.tapshield.android.api.googleplaces.model.Place;
import com.tapshield.android.api.googleplaces.model.Search;
import com.tapshield.android.api.googleplaces.model.TextSearch;

public class GooglePlaces {

	private static final String TAG = "googleplaces";
	
	public static final String OUTPUT_JSON = "json";
	public static final String OUTPUT_XML = "xml";
	
	private static final String PARAM_KEY = "key";
	
	private GooglePlacesConfig mConfig;
	private String mOutput = OUTPUT_JSON;

	public GooglePlaces config(GooglePlacesConfig config) {
		mConfig = config;
		return this;
	}
	
	public void searchNearby(NearbySearch nearbySearch, final GooglePlacesListener l) {
		search(nearbySearch, l);
	}
	
	public void searchText(TextSearch textSearch, final GooglePlacesListener l) {
		search(textSearch, l);
	}
	
	private void search(final Search search, final GooglePlacesListener l) {
		if (l == null) {
			throw new RuntimeException(GooglePlacesListener.class.getSimpleName() + " object is null.");
		}

		JavelinCommsCallback internalCallback = new JavelinCommsCallback() {

			@Override
			public void onEnd(JavelinCommsRequestResponse response) {
				List<Place> places = null;
				String error = null;

				if (response.successful) {
					try {
						places = new ArrayList<Place>();

						JSONObject jsonResponse = response.jsonResponse;
						JSONArray results = jsonResponse.getJSONArray(search.getResultsKey());

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

				l.onPlacesSearchEnd(response.successful, places, error);
			}
		};

		String url = getUrl(search);
		JavelinComms.httpGet(url, null, null, null, internalCallback);
		Log.i(TAG, "url=" + url);
	}
	
	public void autocomplete(final AutocompleteSearch autocompleteSearch, final GooglePlacesListener l) {
		if (l == null) {
			throw new RuntimeException(GooglePlacesListener.class.getSimpleName() + " object is null.");
		}

		JavelinCommsCallback internalCallback = new JavelinCommsCallback() {

			@Override
			public void onEnd(JavelinCommsRequestResponse response) {
				List<AutocompletePlace> places = null;
				String error = null;

				if (response.successful) {
					try {
						places = new ArrayList<AutocompletePlace>();

						JSONObject jsonResponse = response.jsonResponse;
						JSONArray results = jsonResponse.getJSONArray(autocompleteSearch.getResultsKey());

						Gson gson = new Gson();

						for (int i = 0; i < results.length(); i++) {
							JSONObject jsonPlace = results.getJSONObject(i);
							AutocompletePlace place = gson.fromJson(jsonPlace.toString(),
									AutocompletePlace.class);
							places.add(place);
						}
					} catch (Exception e) {
						places = null;
						error = e.toString();
					}
				} else {
					error = response.response;
				}

				l.onPlacesAutocompleteEnd(response.successful, places, error);
			}
		};

		String url = getUrl(autocompleteSearch);
		JavelinComms.httpGet(url, null, null, null, internalCallback);
		Log.i(TAG, "url=" + url);
	}
	
	private String getUrl(Search search) {
		return mConfig.url() + search.getType() + "/" + mOutput + "?"
				+ PARAM_KEY + "=" + mConfig.key()
				+ search.getParams();
	}

	public interface GooglePlacesListener {
		void onPlacesSearchEnd(boolean ok, List<Place> places, String error);
		void onPlacesAutocompleteEnd(boolean ok, List<AutocompletePlace> places, String error);
	}
}
