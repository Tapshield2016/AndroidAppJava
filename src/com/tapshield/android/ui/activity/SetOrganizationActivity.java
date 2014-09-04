package com.tapshield.android.ui.activity;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnActionExpandListener;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;

import com.google.android.gms.location.LocationListener;
import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinClient.OnAgenciesFetchListener;
import com.tapshield.android.api.model.Agency;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.location.LocationTracker;
import com.tapshield.android.manager.SessionManager;
import com.tapshield.android.ui.adapter.AgencyListAdapter;
import com.tapshield.android.utils.UiUtils;

public class SetOrganizationActivity extends BaseFragmentActivity
		implements OnItemClickListener, LocationListener {

	public static final String EXTRA_SET_ORGANIZATION = "com.tapshield.android.extra.organization";
	
	private JavelinClient mJavelin;
	private LocationTracker mTracker;
	
	private ListView mList;
	private View mEmpty;
	private List<Agency> mNearbyAgencies;
	private List<Agency> mAllAgencies;
	private List<Agency> mQueryResultAllAgencies;
	private AgencyListAdapter mAdapter;
	
	private boolean mNearbyLoaded = false;
	private boolean mAllLoaded = false;
	private AlertDialog mLoader;
	
	@Override
	protected void onCreate(Bundle savedInstance) {
		super.onCreate(savedInstance);
		setContentView(R.layout.activity_setorganization);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		UiUtils.setStepIndicatorInActionBar(this, 0, 3,
				R.string.ts_registration_actionbar_title_pickorg);
		
		mList = (ListView) findViewById(R.id.setorganization_list);
		mEmpty = findViewById(R.id.setorganization_empty);
		
		mJavelin = JavelinClient.getInstance(this, TapShieldApplication.JAVELIN_CONFIG);
		mTracker = LocationTracker.getInstance(this);
		
		mNearbyAgencies = new ArrayList<Agency>();
		mAllAgencies = new ArrayList<Agency>();
		
		mAdapter = new AgencyListAdapter(this, R.layout.item_organizationselection,
				mNearbyAgencies);
		
		mList.setOnItemClickListener(this);
		mList.setAdapter(mAdapter);
		
		//check for preset organization
		Intent i;
		if ((i = getIntent()) != null && i.hasExtra(EXTRA_SET_ORGANIZATION)) {
			Agency presetAgency = 
					Agency.deserializeFromString(i.getStringExtra(EXTRA_SET_ORGANIZATION));
			saveSelectedAgency(presetAgency);
			done();
			return;
		}
		
		mLoader = new AlertDialog.Builder(this)
				.setTitle(R.string.ts_organizationselection_dialog_loading_title)
				.setMessage(R.string.ts_organizationselection_dialog_loading_message)
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
					UiUtils.toastShort(SetOrganizationActivity.this, getString(R.string.ts_organizationselection_toast_loading_error));
					finish();
				}
			}
		});
		mTracker.addLocationListener(this);
		mTracker.start();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		getMenuInflater().inflate(R.menu.setorganization, menu);
		
		//get menu item to set listener on expansion of search widget to
		//    toggle agency list: all and nearby
		//and get specific action view of search item to set listener of changes in search query
		
		MenuItem searchItem = menu.findItem(R.id.action_setorganization_showall);
		SearchView searchView = (SearchView) searchItem.getActionView();
		searchView.setQueryHint(getString(R.string.ts_setorganization_filter));
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
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			return true;
		}
		return false;
	}
	
	@Override
	public void onBackPressed() {
		//stop SessionManager sporadic checks and finish activity
		new AlertDialog.Builder(this)
				.setTitle(R.string.ts_setorganization_dialog_notnow_title)
				.setMessage(R.string.ts_setorganization_dialog_notnow_message)
				.setPositiveButton(R.string.ts_common_ok, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						SessionManager
								.getInstance(SetOrganizationActivity.this)
								.setSporadicChecks(false);
						finish();
					}
				})
				.show();
	}
	
	private void setDataSet(List<Agency> dataSet) {
		mAdapter.setItems(dataSet);
		mEmpty.setVisibility(dataSet.isEmpty() ? View.VISIBLE : View.GONE);
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
	public void onItemClick(AdapterView<?> adapter, View v, int position, long id) {
		Agency agencySelected = (Agency) adapter.getItemAtPosition(position);
		saveSelectedAgency(agencySelected);
		done();
	}
	
	private void dismissLoaderWhenFinished() {
		if (mNearbyLoaded && mAllLoaded) {
			mLoader.dismiss();
		}
	}
	
	@Override
	public void onLocationChanged(Location l) {
		mTracker.stop();
		
		if (!mNearbyAgencies.isEmpty()) {
			return;
		}
		
		mJavelin.fetchAgenciesNearby(l.getLatitude(), l.getLongitude(), 10, new OnAgenciesFetchListener() {
			
			@Override
			public void onAgenciesFetch(boolean successful, List<Agency> agencies, Throwable exception) {
				
				if (successful) {
					mNearbyAgencies.clear();
					mNearbyAgencies.addAll(agencies);
					mAdapter.notifyDataSetChanged();
					mNearbyLoaded = true;
					dismissLoaderWhenFinished();
					mEmpty.setVisibility(mNearbyAgencies.isEmpty() ? View.VISIBLE : View.GONE);
				} else {
					UiUtils.toastShort(SetOrganizationActivity.this,
							getString(R.string.ts_organizationselection_toast_loading_error));
					finish();
				}
			}
		});
	}
	
	private void saveSelectedAgency(final Agency agency) {
		//do NOT call UserManager.updateRequiredInformation() since it's the SessionManager's
		// responsibility once all pieces of information have been verified.
		//only set temporary organization
		mJavelin.getUserManager().setTemporaryAgency(agency);
	}
	
	private void done() {
		Intent intent = new Intent(this, AddEmailActivity.class);
		startActivity(intent);
	}
}
