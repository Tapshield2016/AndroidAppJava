package com.tapshield.android.ui.activity;

import java.util.List;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

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
import com.tapshield.android.api.googledirections.model.GoogleDirectionsResponse;
import com.tapshield.android.api.googledirections.model.Route;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.location.LocationTracker;
import com.tapshield.android.manager.EntourageManager;
import com.tapshield.android.ui.adapter.RouteFragmentPagerAdapter;
import com.tapshield.android.utils.UiUtils;

public class PickRouteActivity extends FragmentActivity implements LocationListener, GoogleDirectionsListener {
	
	public static final String EXTRA_MODE = "com.tapshield.android.intent.extra.route_mode";
	public static final String EXTRA_DESTINATION = "com.tapshield.android.intent.extra.route_destination";
	public static final String EXTRA_DESTIONATION_NAME = "com.tapshield.android.intent.extra.route_destination_name";
	public static final int MODE_DRIVING = 0;
	public static final int MODE_WALKING = 1;
	
	private LocationTracker mTracker;
	private String mOrigin;
	private String mDestination;
	private String mMode;
	private String mOptionalDestinationName;
	private ProgressDialog mGettingLocationDialog;
	private ProgressDialog mGettingRoutesDialog;
	private List<Route> mRoutes;
	private int mSelectedRoute = -1;
	private GoogleMap mMap;
	private TextView mWarnings;
	private ViewPager mPager;
	private RouteFragmentPagerAdapter mPagerAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_pickroute);
		
		mMap = ((SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.pickroute_fragment_map)).getMap();
		
		mWarnings = (TextView) findViewById(R.id.pickroute_text_warning);
		mPager = (ViewPager) findViewById(R.id.pickroute_pager);
		mPagerAdapter = new RouteFragmentPagerAdapter(getSupportFragmentManager());
		mPager.setAdapter(mPagerAdapter);

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
			
			
			mOptionalDestinationName = extras.containsKey(EXTRA_DESTIONATION_NAME) ?
					extras.getString(EXTRA_DESTIONATION_NAME) : null;

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
		
		mPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			
			@Override
			public void onPageSelected(int position) {
				mSelectedRoute = position;
				updateRoutesUi();
			}
			
			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {}
			
			@Override
			public void onPageScrollStateChanged(int arg0) {}
		});
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
			//use the setter/getter for temporary route to hold it for following activities
			Route r = mRoutes.get(mSelectedRoute);
			r.destinationName(mOptionalDestinationName);
			
			EntourageManager
					.get(this)
					.setTemporaryRoute(
							r);
			Intent arrivalAndContacts = new Intent(this, PickArrivalContacts.class);
			startActivity(arrivalAndContacts);
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
				.setMode(mMode)
				.setRequestAlternatives();
				
		GoogleDirections.request(request, this);
	}

	@Override
	public void onDirectionsRetrieval(boolean ok, GoogleDirectionsResponse response) {
		mGettingRoutesDialog.dismiss();
		
		if (ok) {
			mSelectedRoute = 0;
			mRoutes = response.routes();
			mPagerAdapter.setRoutes(mRoutes);
			updateRoutesUi();
		}
	}
	
	private void updateRoutesUi() {

		mMap.clear();
		
		int colorSelected = getResources().getColor(R.color.ts_brand_light);
		int colorUnselected = getResources().getColor(R.color.ts_gray_dark);
		
		for (int r = 0; r < mRoutes.size(); r++) {
			
			Route route = mRoutes.get(r);
			
			//draw unselected routes...
			if (r != mSelectedRoute) {
				drawRoute(route, colorUnselected);
			}
			
			//...bounds on selected route...
			if (mSelectedRoute == r) {
				animateCameraBounding(route.boundsNeLat(), route.boundsNeLon(),
						route.boundsSwLat(), route.boundsSwLon());
			}
		}
		
		//...and draw selected so it will always overlay alternatives
		drawRoute(mRoutes.get(mSelectedRoute), colorSelected);
		
		String[] warningList =  mRoutes.get(mSelectedRoute).warnings();
		
		if (warningList != null && warningList.length > 0) {

			String warnings = new String();
			for (String w : warningList) {
				if (!warnings.isEmpty()) {
					warnings = warnings.concat(", ");
				}
				warnings = warnings.concat(w);
			}

			mWarnings.setText(warnings);
			mWarnings.setVisibility(View.VISIBLE);
		} else {
			mWarnings.setVisibility(View.GONE);
		}
	}
	
	private void drawRoute(Route route, int color) {

		int routeWidth = getResources().getInteger(R.integer.ts_entourage_route_width);

		PolylineOptions routePoly = new PolylineOptions()
				.color(color)
				.width(routeWidth);

		List<Location> list = route.decodedOverviewPolyline();
		for (Location l : list) {
			routePoly.add(new LatLng(l.getLatitude(), l.getLongitude()));
		}

		mMap.addPolyline(routePoly);
	}
	
	private void animateCameraBounding(double northeastLatitude, double northeastLongitude,
			double southwestLatitude, double southwestLongitude) {
		LatLngBounds mapBounds = new LatLngBounds(
				new LatLng(southwestLatitude, southwestLongitude),
				new LatLng(northeastLatitude, northeastLongitude));
		CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(mapBounds, 100);
		mMap.animateCamera(cameraUpdate);
	}
}
