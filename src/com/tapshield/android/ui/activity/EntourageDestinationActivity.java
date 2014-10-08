package com.tapshield.android.ui.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.location.Address;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.tapshield.android.R;
import com.tapshield.android.api.googleplaces.GooglePlaces;
import com.tapshield.android.api.googleplaces.model.AutocompletePlace;
import com.tapshield.android.api.googleplaces.model.AutocompleteSearch;
import com.tapshield.android.api.googleplaces.model.NearbySearch;
import com.tapshield.android.api.googleplaces.model.Place;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.location.LocationTracker;
import com.tapshield.android.ui.adapter.ContactPlaceAutoCompleteAdapter;
import com.tapshield.android.ui.adapter.EntourageDestinationPagerAdapter;
import com.tapshield.android.ui.view.CustomAutoCompleteTextView;
import com.tapshield.android.ui.view.CustomAutoCompleteTextView.SelectionConverter;
import com.tapshield.android.utils.ContactsRetriever;
import com.tapshield.android.utils.ContactsRetriever.Contact;
import com.tapshield.android.utils.GeoUtils.GeocoderAsync;
import com.tapshield.android.utils.GeoUtils.GeocoderAsyncListener;
import com.tapshield.android.utils.UiUtils;

public class EntourageDestinationActivity extends BaseFragmentActivity
		implements LocationListener, GooglePlaces.GooglePlacesListener {

	private static final String TAG = "ts-entourage";
	
	private GoogleMap mMap;
	private CustomAutoCompleteTextView mSearch;
	private ViewPager mPlacesPager;
	private EntourageDestinationPagerAdapter mPagerAdapter;
	private ContactPlaceAutoCompleteAdapter mAutoCompleteAdapter;
	private AlertDialog mConfirmationDialog;
	private ProgressDialog mLocatingDialog;
	
	private GooglePlaces mPlacesApi;
	private Marker mDetailsMarker;
	private List<Marker> mNearbyMarkers = new ArrayList<Marker>();
	private List<AutocompletePlace> mAutocompletePlaces = new ArrayList<AutocompletePlace>();
	private List<Contact> mContacts = new ArrayList<Contact>();
	private List<Contact> mContactsFiltered = new ArrayList<Contact>();

	private Location mLocation;
	private LocationTracker mTracker;
	private boolean mCameraUpdated = false;
	private NavMode mNavMode = NavMode.FREE;
	
	private static enum NavMode {
		FREE,
		PAGED_RESULTS
	}
	
	@Override
	protected void onCreate(Bundle savedInstance) {
		super.onCreate(savedInstance);
		setContentView(R.layout.activity_entourage_destination);
		getActionBar().hide();
		
		mPlacesApi = new GooglePlaces()
				.config(TapShieldApplication.GOOGLEPLACES_CONFIG);
		
		mSearch = (CustomAutoCompleteTextView) findViewById(R.id.entourage_destination_autocomplete_search);
		mSearch.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				onUserInput(s.toString().trim());
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			
			@Override
			public void afterTextChanged(Editable s) {}
		});

		mAutoCompleteAdapter = new ContactPlaceAutoCompleteAdapter(this, mContactsFiltered,
				mAutocompletePlaces);
		
		mSearch.setAdapter(mAutoCompleteAdapter);
		mSearch.setThreshold(3);
		mSearch.setSelectionConverter(new SelectionConverter() {
			
			@Override
			public String toString(Object selectedItem) {
				return mSearch.getText().toString();
			}
		});
		mSearch.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

				Log.i(TAG, "index=" + position + " contact=" + mAutoCompleteAdapter.isContact(position)
						+ " contacts=" + mContactsFiltered.size() + " places=" + mAutocompletePlaces.size());
				
				if (mAutoCompleteAdapter.isFirst(position)) {
					//SHOW SEARCHING DIALOG
					
					NearbySearch nearby = new NearbySearch(
							mLocation.getLatitude(), mLocation.getLongitude(), 20000)
							.rankby(NearbySearch.RANKBY_DISTANCE)
							.keyword(mSearch.getText().toString().trim());
					
					mPlacesApi.searchNearby(nearby, EntourageDestinationActivity.this);
					
				} else if (mAutoCompleteAdapter.isContact(position)) {
					
					final Contact contact = (Contact) mAutoCompleteAdapter.getItem(position);
					final String address = contact.address().get(0);
					
					//get location from the address via Geocoder util
					GeocoderAsyncListener listener = new GeocoderAsyncListener() {

						@Override
						public void onGeocoderAsyncFinish(boolean hasResults,
								List<Address> results) {
							mLocatingDialog.dismiss();
							if (hasResults) {
								LatLng position = new LatLng(results.get(0).getLatitude(),
										results.get(0).getLongitude());
								moveDetailsMarker(contact.name(), address, position);
								moveCamera(position);
							} else {
								UiUtils.toastShort(EntourageDestinationActivity.this, "Invalid Address");
							}
						}
					};

					GeocoderAsync.fromLocationName(EntourageDestinationActivity.this, address, 1,
							listener);
				} else {
					//autocomplete places do not have location data, retrieve details on click to show marker
					AutocompletePlace place = mAutocompletePlaces.get(position);
					mPlacesApi.detailsOf(place, EntourageDestinationActivity.this);
				}
				
				clearSearch();
				clearNearbyMarkers();
				UiUtils.hideKeyboard(EntourageDestinationActivity.this);
				mLocatingDialog = getLocatingDialog();
				mLocatingDialog.show();
			}
		});

		mPagerAdapter = new EntourageDestinationPagerAdapter(getSupportFragmentManager(),
				mNearbyPlaces);
		
		mPlacesPager = (ViewPager) findViewById(R.id.entourage_destination_pager);
		mPlacesPager.setAdapter(mPagerAdapter);
		mPlacesPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			
			@Override
			public void onPageSelected(int position) {
				moveCamera(mNearbyMarkers.get(position).getPosition());
			}
			
			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {}
			
			@Override
			public void onPageScrollStateChanged(int arg0) {}
		});
		
		ImageButton clearSearch = (ImageButton)
				findViewById(R.id.entourage_destination_imagebutton_discard);
		
		clearSearch.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				clearSearch();
			}
		});
		
		ImageButton done = (ImageButton)
				findViewById(R.id.entourage_destination_imagebutton_done);
		done.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mConfirmationDialog.show();
			}
		});
		
		mTracker = LocationTracker.getInstance(this);
		
		mConfirmationDialog = getConfirmationDialog();
		mLocatingDialog = getLocatingDialog();
		
		
		ContactsRetriever.ContactsRetrieverListener contactsRetrieverListener =
				new ContactsRetriever.ContactsRetrieverListener() {
			
			@Override
			public void onContactsRetrieval(List<Contact> contacts) {
				mContacts = contacts;
				onUserInput(mSearch.getText().toString());
			}
		};
		
		new ContactsRetriever(this, contactsRetrieverListener).execute(ContactsRetriever.TYPE_POSTAL);
	}
	
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		
		mMap = ((SupportMapFragment)
				getSupportFragmentManager()
				.findFragmentById(R.id.entourage_destination_fragment_map))
				.getMap();
		
		if (mMap != null) {
			UiSettings controls = mMap.getUiSettings();
			controls.setRotateGesturesEnabled(false);
			controls.setTiltGesturesEnabled(false);
			controls.setIndoorLevelPickerEnabled(false);
			
			mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
				
				@Override
				public boolean onMarkerClick(Marker marker) {
					return false;
				}
			});
		}
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
	public void onBackPressed() {
		
		//assuming there are only two modes, if not default, set to default
		if (isNavModeDefault()) {
			super.onBackPressed();
		} else {
			setNavModeDefault();
		}
	}
	
	private AlertDialog getConfirmationDialog() {
		return new AlertDialog.Builder(this)
				.setTitle(R.string.ts_entourage_destination_dialog_confirmation_title)
				.setMessage(R.string.ts_entourage_destination_dialog_confirmation_message)
				.setPositiveButton(R.string.ts_common_yes, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						UiUtils.toastShort(EntourageDestinationActivity.this, "Destination set!");
						finish();
					}
				})
				.setNeutralButton(R.string.ts_common_cancel, null)
				.create();
	}
	
	private ProgressDialog getLocatingDialog() {
		ProgressDialog d = new ProgressDialog(this);
		d.setIndeterminate(true);
		d.setCancelable(false);
		d.setMessage(getString(R.string.ts_entourage_destination_dialog_locating_message));
		return d;
	}
	
	private void clearSearch() {
		mSearch.setText("");
	}
	
	private void clearNearbyMarkers() {
		for (Marker m : mNearbyMarkers) {
			m.remove();
		}
	}
	
	private Marker createMarker(String title, String snippet, LatLng position) {
		MarkerOptions options = new MarkerOptions()
				.anchor(0.5f, 1.0f)
				.title(title)
				.snippet(snippet)
				.icon(BitmapDescriptorFactory.defaultMarker())
				.position(position);
		return mMap.addMarker(options);
	}
	
	private void clearDetailsMarker() {
		if (mDetailsMarker != null) {
			mDetailsMarker.remove();
			mDetailsMarker = null;
		}
	}
	
	private void moveDetailsMarker(String title, String snippet, LatLng position) {
		if (mDetailsMarker == null) {
			mDetailsMarker = createMarker(title, snippet, position);
		} else {
			mDetailsMarker.setTitle(title);
			mDetailsMarker.setSnippet(snippet);
			mDetailsMarker.setPosition(position);
		}
	}
	
	private void moveCamera(LatLng position) {
		CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(position);
		mMap.animateCamera(cameraUpdate);
	}
	
	private NavMode getNavModeDefault() {
		return NavMode.FREE;
	}
	
	private boolean isNavModeDefault() {
		return mNavMode == getNavModeDefault();
	}
	
	private void setNavModeDefault() {
		setNavMode(getNavModeDefault());
	}
	
	private void setNavMode(NavMode navMode) {
		if (mNavMode == navMode) {
			return;
		}
		
		mNavMode = navMode;

		int visPager = 0;
		int visBar = 0;
		int animResPager = 0;
		int animResBar = 0;
		
		switch (mNavMode) {
		case FREE:
			//hide result pager
			visPager = View.GONE;
			animResPager = R.anim.slide_out_dn;
			
			//show clean search bar
			visBar = View.VISIBLE;
			animResBar = R.anim.slide_in_dn;
			mSearch.setText("");
			clearNearbyMarkers();
			mCameraUpdated = false;
			onLocationChanged(mLocation);
			clearDetailsMarker();
			break;
		case PAGED_RESULTS:
			//show result pager
			visPager = View.VISIBLE;
			animResPager = R.anim.slide_in_up;
			
			//hide search bar
			visBar = View.GONE;
			animResBar = R.anim.slide_out_up;
			break;
		}
		
		if (mPlacesPager.getVisibility() != visPager) {
			mPlacesPager.setVisibility(visPager);
			mPlacesPager.startAnimation(AnimationUtils.loadAnimation(this, animResPager));
		}
		
		FrameLayout searchContainer = (FrameLayout) mSearch.getParent();
		
		if (searchContainer.getVisibility() != visBar) {
			searchContainer.setVisibility(visBar);
			searchContainer.startAnimation(AnimationUtils.loadAnimation(this, animResBar));
		}
	}
	
	private void filterContacts(String query) {
		query = query.trim().toLowerCase(Locale.getDefault());
		
		mContactsFiltered.clear();
		
		String name;
		for (Contact contact : mContacts) {
			name = contact.name().trim().toLowerCase(Locale.getDefault());
			if (name.contains(query)) {
				mContactsFiltered.add(contact);
			}
		}
		
		mAutoCompleteAdapter.notifyDataSetChanged();
	}
	
	private void onUserInput(String input) {
		if (input.length() >= mSearch.getThreshold()) {
			
			mAutoCompleteAdapter.setSearchTerm(input);
			
			if (mLocation != null) {

				filterContacts(input);
				
				AutocompleteSearch autocomplete = new AutocompleteSearch(input)
						.location(mLocation.getLatitude(), mLocation.getLongitude(), 500);
				
				mPlacesApi.autocomplete(autocomplete, EntourageDestinationActivity.this);
			}
		}
	}
	
	@Override
	public void onLocationChanged(Location l) {
		mLocation = l;
		CameraUpdate update = CameraUpdateFactory
				.newLatLngZoom(new LatLng(l.getLatitude(), l.getLongitude()), 15);
		if (!mCameraUpdated) {
			mCameraUpdated = true;
			mMap.moveCamera(update);
		}
		//drawUserLocation(); ?
	}

	private List<Place> mNearbyPlaces = new ArrayList<Place>();
	
	@Override
	public void onPlacesSearchEnd(boolean ok, List<Place> places, String error) {
		
		mLocatingDialog.dismiss();
		
		if (ok) {
			
			if (places == null || places.isEmpty()) {
				UiUtils.toastShort(this, "No results!");
				Log.i(TAG, "Error=" + error);
				return;
			}

			setNavMode(NavMode.PAGED_RESULTS);
			
			LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

			//clear and store newly created markers
			mNearbyMarkers.clear();
			LatLng position;
			for (Place place : places) {
				position = new LatLng(place.latitude(), place.longitude());
				mNearbyMarkers.add(createMarker(place.name(), place.address(), position));
				boundsBuilder.include(position);
			}
			
			CameraUpdate cameraUpdate = CameraUpdateFactory
					.newLatLngBounds(boundsBuilder.build(), 200);
			mMap.animateCamera(cameraUpdate);
			
			//update list and notify page adapter
			mNearbyPlaces.clear();
			mNearbyPlaces.addAll(places);
			mPagerAdapter.notifyDataSetChanged();
		}
	}

	@Override
	public void onPlacesAutocompleteEnd(boolean ok, List<AutocompletePlace> places, String error) {
		if (ok) {
			mAutocompletePlaces.clear();
			mAutocompletePlaces.addAll(places);
			mAutoCompleteAdapter.notifyDataSetChanged();
		}
	}

	@Override
	public void onPlacesDetailsEnd(boolean ok, Place place, String error) {
		
		mLocatingDialog.dismiss();
		
		if (ok) {
			LatLng position = new LatLng(place.latitude(), place.longitude());
			
			moveDetailsMarker(place.name(), place.address(), position);
			moveCamera(position);
			
			mDetailsMarker.showInfoWindow();
		}
	}
}
