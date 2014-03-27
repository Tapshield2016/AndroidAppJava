package com.tapshield.android.ui.fragment;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.location.Location;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnActionExpandListener;
import android.view.View;
import android.view.ViewGroup;
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

public class OrganizationSelectionFragment extends BaseFragment
		implements OnItemClickListener, LocationListener {

	public static final String EXTRA_AGENCY = "com.tapshield.android.extra.organizationselection.result";
	
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
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_organizationselection, container, false);
		mList = (ListView) root.findViewById(R.id.fragment_organizationselection_list);
		return root;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		mJavelin = JavelinClient.getInstance(getActivity(), TapShieldApplication.JAVELIN_CONFIG);
		mTracker = LocationTracker.getInstance(getActivity());
		
		mNearbyAgencies = new ArrayList<Agency>();
		mAllAgencies = new ArrayList<Agency>();
		
		mAdapter = new AgencyListAdapter(getActivity(), R.layout.item_organizationselection,
				mNearbyAgencies);
		
		TextView emptyListView = new TextView(getActivity());
		LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		emptyListView.setLayoutParams(lp);
		emptyListView.setGravity(Gravity.CENTER);
		
		mList.setOnItemClickListener(this);
		mList.setEmptyView(emptyListView);
		mList.setAdapter(mAdapter);
		
		mLoader = new AlertDialog.Builder(getActivity())
				.setTitle("loading agencies")
				.setMessage("please wait...")
				.create();
		
		mLoader.show();
		mLoader.setOnCancelListener(new OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
				getActivity().finish();
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
					UiUtils.toastShort(getActivity(), "error retrieving list of organizations");
					getActivity().finish();
				}
			}
		});
		mTracker.addLocationListener(this);
		mTracker.start();
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		
		inflater.inflate(R.menu.organizationselection, menu);
		
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
	public void onItemClick(AdapterView<?> adapter, View v, int position, long id) {
		Agency agencySelected = (Agency) adapter.getItemAtPosition(position);
		Bundle extras = new Bundle();
		extras.putString(EXTRA_AGENCY, Agency.serializeToString(agencySelected));
		userRequestProceed(extras);
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
				} else {
					UiUtils.toastShort(getActivity(), "error retrieving list of organizations");
					getActivity().finish();
				}
			}
		});
	}
}
