package com.tapshield.android.ui.activity;

import java.util.List;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;
import com.tapshield.android.R;
import com.tapshield.android.api.googledirections.GoogleDirections;
import com.tapshield.android.api.googledirections.GoogleDirections.GoogleDirectionsListener;
import com.tapshield.android.api.googledirections.GoogleDirectionsRequest;
import com.tapshield.android.api.googledirections.model.Route;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.location.LocationTracker;
import com.tapshield.android.utils.UiUtils;

public class PickRouteActivity extends FragmentActivity implements LocationListener, GoogleDirectionsListener {
	
	public static final String EXTRA_MODE = "com.tapshield.android.intent.extra.route_mode";
	public static final String EXTRA_DESTINATION = "com.tapshield.android.intent.extra.route_destination";
	public static final int MODE_DRIVING = 0;
	public static final int MODE_WALKING = 1;
	
	private LocationTracker mTracker;
	private String mOrigin;
	private String mDestination;
	private String mMode;
	private ProgressDialog mGettingLocationDialog;
	private ProgressDialog mGettingRoutesDialog;
	private List<Route> mRoutes;
	private int mSelectedRoute = -1;
	private GoogleMap mMap;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_pickroute);
		
		mMap = ((SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.main_fragment_map)).getMap();
		
		mGettingLocationDialog = getGettingLocationDialog();
		mGettingRoutesDialog = getGettingRoutesDialog();
		
		mTracker = LocationTracker.getInstance(this);
		
		if (getIntent() != null && getIntent().getExtras() != null) {
			Bundle extras = getIntent().getExtras();
			
			//cancel if no required extras were given
			if (!extras.containsKey(EXTRA_DESTINATION) || !extras.containsKey(EXTRA_MODE)) {
				cancel();
				return;
			}

			mDestination = extras.getString(EXTRA_DESTINATION);
			int mode = extras.getInt(EXTRA_MODE);
			if (mode == MODE_DRIVING) {
				mMode = GoogleDirectionsRequest.MODE_DRIVING;
			} else if (mode == MODE_WALKING) {
				mMode = GoogleDirectionsRequest.MODE_WALKING;
			} else {
				//unknown/bad extra
				cancel();
			}
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.next, menu);
		return true;
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		mGettingLocationDialog.show();
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
		switch (item.getItemId()) {
		case R.id.action_next:
			next();
			return true;
		}
		return false;
	}
	
	private ProgressDialog getGettingLocationDialog() {
		ProgressDialog p = new ProgressDialog(this);
		p.setMessage(getString(R.string.ts_pickroute_progressdialog_location_message));
		p.setCancelable(true);
		p.setOnCancelListener(new DialogInterface.OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
				cancel();
			}
		});
		p.setIndeterminate(true);
		return p;
	}
	
	private ProgressDialog getGettingRoutesDialog() {
		ProgressDialog p = new ProgressDialog(this);
		p.setMessage(getString(R.string.ts_pickroute_progressdialog_routes_message));
		p.setCancelable(true);
		p.setOnCancelListener(new DialogInterface.OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
				cancel();
			}
		});
		p.setIndeterminate(true);
		return p;
	}
	
	private void cancel() {
		UiUtils.startActivityNoStack(this, MainActivity.class);
	}
	
	private void next() {
		//proceed only if a route has been selected
		if (mSelectedRoute >= 0) {
			UiUtils.toastShort(this, "Route #" + mSelectedRoute + " selected");
		} else {
			UiUtils.toastShort(this, getString(R.string.ts_pickroute_toast_noroute));
		}
	}

	@Override
	public void onLocationChanged(final Location l) {
		if (mOrigin != null) {
			return;
		}
		
		mGettingLocationDialog.dismiss();
		mOrigin = l.getLatitude() + "," + l.getLongitude();
		requestRoutes();
	}
	
	private void requestRoutes() {
		mSelectedRoute = -1;
		mGettingRoutesDialog.show();
		GoogleDirectionsRequest request = new GoogleDirectionsRequest(
				TapShieldApplication.GOOGLEDIRECTIONS_CONFIG, mOrigin, mDestination)
				.setMode(mMode);
				
		GoogleDirections.request(request, this);
	}

	@Override
	public void onDirectionsRetrieval(boolean ok, List<Route> routes, String errorIfNotOk) {
		mGettingRoutesDialog.dismiss();
		if (ok) {
			mSelectedRoute = 0;
			mRoutes = routes;
			updateRoutesUi();
		} else {
			Log.e("aaa", "Error requesting routes=" + errorIfNotOk);
		}
	}
	
	private void updateRoutesUi() {
		
		for (int r = 0; r < mRoutes.size(); r++) {
			
			int color = getResources().getColor(
					r == mSelectedRoute ? R.color.ts_brand_light : R.color.ts_gray_light);
			
			PolylineOptions routePoly = new PolylineOptions()
				.color(color)
				.width(20);
			
			Route route = mRoutes.get(r);
			
			List<Location> list = route.decodedOverviewPolyline();
			for (Location l : list) {
				routePoly.add(new LatLng(l.getLatitude(), l.getLongitude()));
			}
			
			mMap.addPolyline(routePoly);
			animateCameraBounding(list);
		}
	}
	
	private void animateCameraBounding(List<Location> bounds) {
		Location first = bounds.get(0);
		Location last = bounds.get(bounds.size() - 1);

		double west;
		double north;
		double east;
		double south;
		
		if (first.getLongitude() > last.getLongitude()) {
			east = first.getLongitude();
			west = last.getLongitude();
		} else {
			west = first.getLongitude();
			east = last.getLongitude();
		}
		
		if (first.getLatitude() > last.getLatitude()) {
			north = first.getLatitude();
			south = last.getLatitude();
		} else {
			south = first.getLatitude();
			north = last.getLatitude();
		}
		
		LatLng northEast = new LatLng(north, east);
		LatLng southWest = new LatLng(south, west);
		
		LatLngBounds mapBounds = new LatLngBounds(southWest, northEast);
		CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(mapBounds, 100);
		
		mMap.animateCamera(cameraUpdate);
	}
}
