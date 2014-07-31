package com.tapshield.android.ui.activity;

import java.util.ArrayList;
import java.util.List;

import android.app.ActionBar;
import android.app.ProgressDialog;
import android.location.Location;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;
import com.tapshield.android.R;
import com.tapshield.android.api.googledirections.GoogleDirections;
import com.tapshield.android.api.googledirections.GoogleDirections.GoogleDirectionsListener;
import com.tapshield.android.api.googledirections.GoogleDirectionsRequest;
import com.tapshield.android.api.googledirections.model.GoogleDirectionsResponse;
import com.tapshield.android.api.googledirections.model.Route;
import com.tapshield.android.api.googleplaces.GooglePlacesClient;
import com.tapshield.android.api.googleplaces.GooglePlacesClient.GooglePlacesListener;
import com.tapshield.android.api.googleplaces.GooglePlacesRequest;
import com.tapshield.android.api.googleplaces.model.Place;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.location.LocationTracker;
import com.tapshield.android.utils.UiUtils;

public class EntourageDestinationActivity extends BaseFragmentActivity
		implements GooglePlacesListener, LocationListener, GoogleDirectionsListener {

	private SearchView mSearchView;
	
	private GoogleMap mMap;
	private ProgressDialog mSearchingDialog;
	private ProgressDialog mRoutingDialog;
	
	private GooglePlacesClient mPlacesClient;
	private List<Place> mPlaces;
	private List<Marker> mPlacesMarkers;
	private boolean mPlaceCentered = false;
	private String mPlaceCenteredName;

	private List<Route> mRoutes;
	private List<Polyline> mRoutesPolylines;
	private int mRouteSelected = 0;
	
	private LocationTracker mTracker;
	private Location mUserLocation;
	
	@Override
	protected void onCreate(Bundle savedInstance) {
		super.onCreate(savedInstance);
		setContentView(R.layout.activity_entouragedestination);
		
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		
		mSearchingDialog = getSearchingDialog();
		mRoutingDialog = getRoutingDialog();
		
		mPlaces = new ArrayList<Place>();
		mPlacesMarkers = new ArrayList<Marker>();
		
		mRoutes = new ArrayList<Route>();
		mRoutesPolylines = new ArrayList<Polyline>();
		
		mTracker = LocationTracker.getInstance(this);
		mPlacesClient = GooglePlacesClient.get(TapShieldApplication.GOOGLEPLACES_CONFIG);
	}
	
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		
		mMap = ((SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.entouragedestination_fragment_map)).getMap();
		
		if (mMap != null) {
			mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
				
				@Override
				public void onCameraChange(CameraPosition cameraPosition) {
					mPlaceCentered = false;
					mPlaceCenteredName = null;
				}
			});
			
			mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
				
				@Override
				public boolean onMarkerClick(Marker marker) {
					mPlaceCentered = true;
					mPlaceCenteredName = marker.getTitle();
					return false;
				}
			});
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.entouragedestination, menu);
		getMenuInflater().inflate(R.menu.done, menu);
		
		MenuItem searchItem = menu.findItem(R.id.action_entouragedestination_search);
		mSearchView = (SearchView) searchItem.getActionView();
		mSearchView.setQueryHint(searchItem.getTitle());
		
		mSearchView.setOnQueryTextListener(new OnQueryTextListener() {
			
			@Override
			public boolean onQueryTextSubmit(String query) {
				
				if (mUserLocation == null) {
					UiUtils.toastShort(EntourageDestinationActivity.this, "Still obtaining location");
					return false;
				}
				
				mSearchView.setQuery(new String(), false);
				mSearchView.onActionViewCollapsed();
				
				mSearchingDialog.show();
				
				GooglePlacesRequest request =
						new GooglePlacesRequest(TapShieldApplication.GOOGLEPLACES_CONFIG)
						.setTypeSearch()
						.setLocation(mUserLocation.getLatitude(), mUserLocation.getLongitude(), 4000)
						.setSearch(query);
				mPlacesClient.request(request, EntourageDestinationActivity.this);

				return true;
			}
			
			@Override
			public boolean onQueryTextChange(String newText) {
				return false;
			}
		});
		
		return true;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mTracker.addLocationListener(this);
		mTracker.start();
	}
	
	@Override
	protected void onPause() {
		mTracker.removeLocationListener(this);
		mTracker.stop();
		super.onPause();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.action_done:
			GoogleDirectionsRequest request =
					new GoogleDirectionsRequest(
							TapShieldApplication.GOOGLEDIRECTIONS_CONFIG,
							mUserLocation.getLatitude(),
							mUserLocation.getLongitude(),
							mMap.getCameraPosition().target.latitude,
							mMap.getCameraPosition().target.longitude)
					.setRequestAlternatives()
					.setMode(GoogleDirectionsRequest.MODE_DRIVING);
			GoogleDirections.request(request, this);
			break;
		case R.id.action_entouragedestination_contacts:
			
			break;
		}
		return false;
	}

	private ProgressDialog getSearchingDialog() {
		ProgressDialog d = new ProgressDialog(this);
		d.setCancelable(false);
		d.setIndeterminate(true);
		d.setMessage("Searching. Please wait...");
		return d;
	}
	
	private ProgressDialog getRoutingDialog() {
		ProgressDialog d = new ProgressDialog(this);
		d.setCancelable(false);
		d.setIndeterminate(true);
		d.setMessage("Routing. Please wait...");
		return d;
	}
	
	//google places callback
	@Override
	public void onFinish(boolean ok, List<Place> places, String errorIfNotOk) {

		mSearchingDialog.dismiss();
		
		if (ok) {
			mPlaces.clear();
			mPlaces.addAll(places);
			refreshPlacesMarkers();
			
			if (places.isEmpty()) {
				UiUtils.toastShort(this, "No results");
			}
		} else {
			UiUtils.toastLong(this, String.format("Error searching places: %s", errorIfNotOk));
		}
	}
	
	//google directions callback
	@Override
	public void onDirectionsRetrieval(boolean ok, GoogleDirectionsResponse response) {
		if (ok) {
			mRoutes = response.routes();
			refreshRoutes();
		} else {
			UiUtils.toastLong(this, "Error retriving routes:" + response.status());
		}
	}
	
	private void refreshPlacesMarkers() {
		for (Marker m : mPlacesMarkers) {
			if (m != null) {
				m.remove();
			}
		}
		
		mPlacesMarkers.clear();
		
		if (mPlaces.isEmpty()) {
			return;
		}
		
		LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
		
		for (Place p : mPlaces) {
			if (p != null) {
				mPlacesMarkers.add(addPlaceMarkerToMap(p));
				boundsBuilder.include(new LatLng(p.latitude(), p.longitude()));
			}
		}
		
		CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(
				adjustBoundsForMaxZoomLevel(boundsBuilder.build()), 100);
		mMap.animateCamera(cameraUpdate);
	}
	
	private Marker addPlaceMarkerToMap(final Place p) {
		MarkerOptions options = new MarkerOptions()
				.position(new LatLng(p.latitude(), p.longitude()))
				.title(p.name())
				.snippet(p.address())
				.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
				.anchor(0.5f, 1.0f);
		return mMap.addMarker(options);
	}
	
	private void refreshRoutes() {
		int colorSelected = getResources().getColor(R.color.ts_brand_light);
		int colorUnselected = getResources().getColor(R.color.ts_gray_dark);
		
		for (Polyline p : mRoutesPolylines) {
			p.remove();
		}
		
		mRoutesPolylines.clear();

		boolean isSelected;
		for (int i = 0; i < mRoutes.size(); i++) {
			isSelected = i == mRouteSelected;
			Route r = mRoutes.get(i);
			PolylineOptions polylineOptions = new PolylineOptions()
					.addAll(PolyUtil.decode(r.overviewPolyline()))
					.color(isSelected ? colorSelected : colorUnselected)
					.zIndex(isSelected ? 100 : 1);
			mRoutesPolylines.add(mMap.addPolyline(polylineOptions));
		}
	}
	
	//http://stackoverflow.com/questions/15700808/setting-max-zoom-level-in-google-maps-android-api-v2
	// as of july 31 2014
	private LatLngBounds adjustBoundsForMaxZoomLevel(LatLngBounds bounds) {
		  LatLng sw = bounds.southwest;
		  LatLng ne = bounds.northeast;
		  double deltaLat = Math.abs(sw.latitude - ne.latitude);
		  double deltaLon = Math.abs(sw.longitude - ne.longitude);

		  final double zoomN = 0.01; // minimum zoom coefficient
		  if (deltaLat < zoomN) {
		     sw = new LatLng(sw.latitude - (zoomN - deltaLat / 2), sw.longitude);
		     ne = new LatLng(ne.latitude + (zoomN - deltaLat / 2), ne.longitude);
		     bounds = new LatLngBounds(sw, ne);
		  }
		  else if (deltaLon < zoomN) {
		     sw = new LatLng(sw.latitude, sw.longitude - (zoomN - deltaLon / 2));
		     ne = new LatLng(ne.latitude, ne.longitude + (zoomN - deltaLon / 2));
		     bounds = new LatLngBounds(sw, ne);
		  }

		  return bounds;
		}

	@Override
	public void onLocationChanged(Location location) {
		
		//update camera if mUserLocation is null (one-time thing)
		if (mUserLocation == null) {
			CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(
					new LatLng(location.getLatitude(), location.getLongitude()), 13);
			mMap.moveCamera(cameraUpdate);
		}
		
		mUserLocation = location;
	}
}
