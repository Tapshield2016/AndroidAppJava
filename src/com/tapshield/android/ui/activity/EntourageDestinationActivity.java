package com.tapshield.android.ui.activity;

import java.util.ArrayList;
import java.util.List;

import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.tapshield.android.R;
import com.tapshield.android.api.googleplaces.GooglePlaces;
import com.tapshield.android.api.googleplaces.model.AutocompletePlace;
import com.tapshield.android.api.googleplaces.model.AutocompleteSearch;
import com.tapshield.android.api.googleplaces.model.NearbySearch;
import com.tapshield.android.api.googleplaces.model.Place;
import com.tapshield.android.api.googleplaces.model.TextSearch;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.location.LocationTracker;

public class EntourageDestinationActivity extends BaseFragmentActivity
		implements LocationListener, GooglePlaces.GooglePlacesListener {

	private GoogleMap mMap;
	private AutoCompleteTextView mSearch;
	private ArrayAdapter<String> mAdapter;
	private List<String> mData;
	
	private Location mLocation;
	private LocationTracker mTracker;
	
	private int FLAG_SEARCH_COUNT = 1;
	
	@Override
	protected void onCreate(Bundle savedInstance) {
		super.onCreate(savedInstance);
		setContentView(R.layout.activity_entourage_destination);
		getActionBar().hide();
		
		mSearch = (AutoCompleteTextView) findViewById(R.id.entourage_destination_autocomplete_search);
		mSearch.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (s.length() >= mSearch.getThreshold()) {
					
					if (mLocation != null) {
						
						AutocompleteSearch autocomplete = new AutocompleteSearch(s.toString())
								.location(mLocation.getLatitude(), mLocation.getLongitude(), 500);
						
						TextSearch text = new TextSearch(s.toString())
								.location(mLocation.getLatitude(), mLocation.getLongitude(), 500);
						
						NearbySearch nearby = new NearbySearch(
								mLocation.getLatitude(), mLocation.getLongitude(), 500)
								.rankby(NearbySearch.RANKBY_DISTANCE)
								.keyword(s.toString());
						
						GooglePlaces places = new GooglePlaces()
								.config(TapShieldApplication.GOOGLEPLACES_CONFIG);
						
						places.autocomplete(autocomplete, EntourageDestinationActivity.this);
						places.searchText(text, EntourageDestinationActivity.this);
						places.searchNearby(nearby, EntourageDestinationActivity.this);
					}
				}
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			
			@Override
			public void afterTextChanged(Editable s) {}
		});

		mData = new ArrayList<String>();
		mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mData);
		
		mSearch.setAdapter(mAdapter);
		mSearch.setThreshold(4);
		
		mTracker = LocationTracker.getInstance(this);
	}
	
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		
		mMap = ((SupportMapFragment)
				getSupportFragmentManager().findFragmentById(R.id.entourage_destination_fragment_map)).getMap();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mTracker.start();
		mTracker.addLocationListener(this);
	}
	
	@Override
	protected void onPause() {
		mTracker.removeLocationListener(this);
		mTracker.stop();
		super.onPause();
	}

	@Override
	public void onLocationChanged(Location l) {
		mLocation = l;
		
		CameraUpdate update = CameraUpdateFactory
				.newLatLngZoom(new LatLng(l.getLatitude(), l.getLongitude()), 15);
		mMap.moveCamera(update);
		//drawUserLocation();
	}

	@Override
	public void onPlacesSearchEnd(boolean ok, List<Place> places, String error) {
		Log.i("googleplaces", "search #" + FLAG_SEARCH_COUNT++);
		Log.i("googleplaces", "ok=" + ok + " places=" + places + " err=" + error);
		if (ok) {
			mData.clear();
			
			Log.i("googleplaces", "====================================");
			for (Place place : places) {
				mData.add(place.name());
				Log.i("googleplaces", "    " + place.name());
			}
			
			mAdapter.notifyDataSetChanged();
		}
	}

	@Override
	public void onPlacesAutocompleteEnd(boolean ok, List<AutocompletePlace> places, String error) {
		Log.i("googleplaces", "search #" + FLAG_SEARCH_COUNT++);
		Log.i("googleplaces", "ok=" + ok + " places=" + places + " err=" + error);
		if (ok) {
			mData.clear();
			
			Log.i("googleplaces", "====================================");
			for (AutocompletePlace place : places) {
				mData.add(place.description());
				Log.i("googleplaces", "    " + place.description());
			}
			
			mAdapter.notifyDataSetChanged();
		}
	}
}
