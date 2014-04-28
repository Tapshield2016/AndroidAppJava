package com.tapshield.android.ui.activity;

import java.util.List;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
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
import com.tapshield.android.api.googledirections.model.Route;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.location.LocationTracker;
import com.tapshield.android.ui.adapter.RouteFragmentPagerAdapter;
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
			Route r = new Route("m{dmDfkuoN@MBu@\\SNGRE\\Er@A|AAj@bDXzBZ`Af@~AH`@RvALpBi@ASAuAEmCS{CUk@CODEDOAc@Cy@?wBDy@FsAF}G\\i@BuITwTVeTZmDAaCMiC[_Es@sXoFyBk@sBw@s@]aBaA{NyIgJyFuC}AcBo@gBi@oB_@mAO}@G{@E{BAeRB_N?oFIuCWeJiAgCQgBCsGBiJ@eED}KHmBDsALeBZwA`@s@XiB|@}AdA_BzAmElFiIhK_@DeC`CuBfBW@I?CAQEAW?CACIQAs@EsEMwN@uZ?gD@sDFyD@{I@yKDM@cBDoFEgCCM?_C?sKI_XFI@QLs@Ge@O{@CwC?WK}WCsDGc@I_@MUsB}BQa@I_@As@AqEAg@Ic@Oc@[w@o@iAm@i@cBaBQUKYOu@EkMCsAOgAI[m@{AaAiBwAeCY{@]o@U_@WUWQ[Kc@Ig@AOFmIHk@AUI");
			r.setDuration(2, "2 seconds");
			r.setSummary("TARDIS");
			r.setWarnings(new String[]{"This route requires 1+ sonic screwdrivers"});
			r.setCopyrights("Copyrights of The Shadow Proclamation");
			mRoutes.add(r);
			mPagerAdapter.setRoutes(mRoutes);
			updateRoutesUi();
		} else {
			Log.e("aaa", "Error requesting routes=" + errorIfNotOk);
		}
	}
	
	private void updateRoutesUi() {
		
		mMap.clear();
		
		for (int r = 0; r < mRoutes.size(); r++) {
			
			int color = getResources().getColor(
					r == mSelectedRoute ? R.color.ts_brand_light : R.color.ts_gray_dark);
			
			PolylineOptions routePoly = new PolylineOptions()
				.color(color)
				.width(20);
			
			Route route = mRoutes.get(r);
			
			List<Location> list = route.decodedOverviewPolyline();
			for (Location l : list) {
				routePoly.add(new LatLng(l.getLatitude(), l.getLongitude()));
			}
			
			mMap.addPolyline(routePoly);
			
			if (mSelectedRoute == r) {
				animateCameraBounding(list);
			}
		}
		
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
