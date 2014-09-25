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
	
	private static final String TYPE_DETAILS = "details";
	
	public static final String OUTPUT_JSON = "json";
	public static final String OUTPUT_XML = "xml";
	
	private static final String PARAM_KEY = "key";
	private static final String PARAM_PLACEID = "placeid";
	
	private GooglePlacesConfig mConfig;
	private String mOutput = OUTPUT_JSON;

	public GooglePlaces config(GooglePlacesConfig config) {
		mConfig = config;
		return this;
	}
	
	public void detailsOf(AutocompletePlace autocompletePlace, final GooglePlacesListener l) {
		detailsOf(autocompletePlace.placeId(), l);
	}
	
	public void detailsOf(Place place, final GooglePlacesListener l) {
		detailsOf(place.placeId(), l);
	}
	
	public void detailsOf(String placeId, final GooglePlacesListener l) {
		JavelinCommsCallback internalCallback = new JavelinCommsCallback() {
			
			@Override
			public void onEnd(JavelinCommsRequestResponse response) {
				boolean ok = response.successful;
				Place place = null;
				String error = null;
				
				if (ok) {
					Gson gson = new Gson();
					
					try {
						place = gson.fromJson(response.response, Place.class);
					} catch (Exception e) {
						ok = false;
						place = null;
						error = e.getMessage();
					}
				} else {
					error = response.response;
				}
				
				l.onPlacesDetailsEnd(ok, place, error);
			}
		};
		
		JavelinComms.httpGet(getDetailsUrl(placeId), null, null, null, internalCallback);
	}
	
	private String getDetailsUrl(String placeId) {
		return mConfig.url() + TYPE_DETAILS + "/" + mOutput + "?"
				+ PARAM_KEY + "=" + mConfig.key()
				+ PARAM_PLACEID + "=" + placeId;
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
				boolean ok = response.successful;
				List<Place> places = null;
				String error = null;

				if (ok) {
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
						ok = false;
						places = null;
						error = e.getMessage();
					}
				} else {
					error = response.response;
				}

				l.onPlacesSearchEnd(ok, places, error);
			}
		};

		JavelinComms.httpGet(getSearchUrl(search), null, null, null, internalCallback);
	}
	
	public void autocomplete(final AutocompleteSearch autocompleteSearch, final GooglePlacesListener l) {
		if (l == null) {
			throw new RuntimeException(GooglePlacesListener.class.getSimpleName() + " object is null.");
		}

		JavelinCommsCallback internalCallback = new JavelinCommsCallback() {

			@Override
			public void onEnd(JavelinCommsRequestResponse response) {
				boolean ok = response.successful;
				List<AutocompletePlace> places = null;
				String error = null;

				if (ok) {
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
						ok = false;
						places = null;
						error = e.getMessage();
					}
				} else {
					error = response.response;
				}

				l.onPlacesAutocompleteEnd(ok, places, error);
			}
		};

		String url = getSearchUrl(autocompleteSearch);
		JavelinComms.httpGet(url, null, null, null, internalCallback);
		Log.i(TAG, "url=" + url);
	}
	
	private String getSearchUrl(Search search) {
		return mConfig.url() + search.getType() + "/" + mOutput + "?"
				+ PARAM_KEY + "=" + mConfig.key()
				+ search.getParams();
	}
	
	public interface GooglePlacesListener {
		void onPlacesSearchEnd(boolean ok, List<Place> places, String error);
		void onPlacesAutocompleteEnd(boolean ok, List<AutocompletePlace> places, String error);
		void onPlacesDetailsEnd(boolean ok, Place place, String error);
	}
}
