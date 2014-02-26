package com.tapshield.android.ui.activity;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnActionExpandListener;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;

import com.google.android.gms.location.LocationListener;
import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinClient.OnAgenciesFetchListener;
import com.tapshield.android.api.model.Agency;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.location.LocationTracker;
import com.tapshield.android.ui.adapter.AgencyListAdapter;
import com.tapshield.android.utils.UiUtils;

public class OrganizationSelectionActivity extends Activity
		implements OnItemClickListener, LocationListener {

	public static final String EXTRA = "com.tapshield.android.extra.organizationselection.result";
	
	private JavelinClient mJavelin;
	private LocationTracker mTracker;
	
	private ListView mList;
	private List<Agency> mNearbyAgencies;
	private List<Agency> mAllAgencies;
	private List<Agency> mQueryResultAllAgencies;
	private AgencyListAdapter mAdapter;
	
	private boolean mNearbyLoaded = false;
	private boolean mAllLoaded = false;
	private AlertDialog mLoader;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_organizationselection);
		
		mJavelin = JavelinClient.getInstance(this, TapShieldApplication.JAVELIN_CONFIG);
		mTracker = LocationTracker.getInstance(this);
		
		mNearbyAgencies = new ArrayList<Agency>();
		mAllAgencies = new ArrayList<Agency>();
		
		mAdapter = new AgencyListAdapter(this, R.layout.item_organizationselection, mNearbyAgencies);
		
		TextView emptyListView = new TextView(this);
		LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		emptyListView.setLayoutParams(lp);
		emptyListView.setGravity(Gravity.CENTER);
		
		mList = (ListView) findViewById(R.id.organizationselection_list);
		mList.setOnItemClickListener(this);
		mList.setEmptyView(emptyListView);
		mList.setAdapter(mAdapter);
		
		mLoader = new AlertDialog.Builder(this)
				.setTitle("loading agencies")
				.setMessage("please wait...")
				.create();
		
		mLoader.show();
		mLoader.setOnCancelListener(new OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
				finish();
			}
		});
		
		mJavelin.fetchAgencies(new OnAgenciesFetchListener() {
			
			@Override
			public void onAgenciesFetch(boolean successful, List<Agency> agencies,
					Throwable exception) {
				if (successful) {
					mAllAgencies.clear();
					mAllAgencies.addAll(agencies);
					mAllLoaded = true;
					dismissLoaderWhenFinished();
				} else {
					finish();
				}
			}
		});
		mTracker.addLocationListener(this);
		mTracker.start();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.organizationselection, menu);
		
		//get menu item to set listener on expansion of search widget to
		//    toggle agency list: all and nearby
		//and get specific action view of search item to set listener of changes in search query
		
		MenuItem searchItem = menu.findItem(R.id.action_organizationselection_search);
		SearchView searchView = (SearchView) searchItem.getActionView();
		searchView.setQueryHint("search all organizations");
		searchItem.setOnActionExpandListener(new OnActionExpandListener() {
			
			@Override
			public boolean onMenuItemActionExpand(MenuItem item) {
				setDataSet(mAllAgencies);
				return true;
			}
			
			@Override
			public boolean onMenuItemActionCollapse(MenuItem item) {
				setDataSet(mNearbyAgencies);
				return true;
			}
		});
		
		searchView.setOnQueryTextListener(new OnQueryTextListener() {
			
			@Override
			public boolean onQueryTextSubmit(String query) {
				return false;
			}
			
			@Override
			public boolean onQueryTextChange(String newText) {
				filterAll(newText);
				return true;
			}
		});
		return true;
	}
	
	private void setDataSet(List<Agency> dataSet) {
		mAdapter.setItems(dataSet);
	}
	
	private void filterAll(String query) {
		if (query == null || query.isEmpty()) {
			setDataSet(mAllAgencies);
		} else {
			
			if (mQueryResultAllAgencies == null) {
				mQueryResultAllAgencies = new ArrayList<Agency>();
			} else {
				mQueryResultAllAgencies.clear();
			}
			
			//regular expression for all that contains the query non-case-sensitive
			String queryRegularExpression = ".*".concat(query.toLowerCase()).concat(".*");
			
			for (Agency a : mAllAgencies) {
				if (a.name.toLowerCase().matches(queryRegularExpression)) {
					mQueryResultAllAgencies.add(a);
				}
			}
			
			setDataSet(mQueryResultAllAgencies);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
		Agency agencySelected = (Agency) adapter.getItemAtPosition(position);
		Intent data = new Intent()
				.putExtra(EXTRA, Agency.serializeToString(agencySelected));
		setResult(Activity.RESULT_OK, data);
		finish();
	}
	
	private void dismissLoaderWhenFinished() {
		if (mNearbyLoaded && mAllLoaded) {
			mLoader.dismiss();
		}
	}

	@Override
	public void onLocationChanged(Location l) {
		mTracker.stop();
		mJavelin.fetchAgenciesNearby(l.getLatitude(), l.getLongitude(), 10, new OnAgenciesFetchListener() {
			
			@Override
			public void onAgenciesFetch(boolean successful, List<Agency> agencies, Throwable exception) {
				if (successful) {
					mNearbyAgencies.clear();
					mNearbyAgencies.addAll(agencies);
					mAdapter.notifyDataSetChanged();
					mNearbyLoaded = true;
					dismissLoaderWhenFinished();
				} else {
					finish();
				}
			}
		});
	}
}
