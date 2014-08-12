package com.tapshield.android.ui.fragment;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;

import com.tapshield.android.R;
import com.tapshield.android.api.googleplaces.GooglePlacesClient;
import com.tapshield.android.api.googleplaces.GooglePlacesClient.GooglePlacesListener;
import com.tapshield.android.api.googleplaces.GooglePlacesRequest;
import com.tapshield.android.api.googleplaces.model.Place;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.utils.UiUtils;

public class PickDestinationPlaceFragment extends BasePickDestinationFragment
		implements GooglePlacesListener {

	private List<Place> mPlaces = new ArrayList<Place>();
	private PlaceAdapter mAdapter;
	private MenuItem mSearch;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mAdapter = new PlaceAdapter(getActivity(), mPlaces);
		setListAdapter(mAdapter);
		
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.pickdestination, menu);
		
		mSearch = menu.findItem(R.id.action_pickdestination_search);
		SearchView searchView = (SearchView) mSearch.getActionView();
		searchView.setQueryHint(
				getResources().getString(R.string.ts_fragment_pickdestination_place_search_hint));
		
		searchView.setOnQueryTextListener(new OnQueryTextListener() {
			
			@Override
			public boolean onQueryTextSubmit(String query) {
				UiUtils.hideKeyboard(getActivity());
				searchPlacesFor(query);
				return true;
			}
			
			@Override
			public boolean onQueryTextChange(String newText) {
				return false;
			}
		});
		expandSearch();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		expandSearch();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		mSearch.collapseActionView();
	}
	
	private void expandSearch() {
		if (mSearch != null) {
			mSearch.expandActionView();
		}
	}
	
	private void searchPlacesFor(String query) {
		setListShown(false);
		/*
		GooglePlacesRequest r =
				new GooglePlacesRequest(TapShieldApplication.GOOGLEPLACES_CONFIG, query);
		GooglePlacesClient
				.get(TapShieldApplication.GOOGLEPLACES_CONFIG)
				.request(r, this);
		*/
	}
	
	@Override
	public void onFinish(boolean ok, List<Place> places, String errorIfNotOk) {
		setListShown(true);
		if (ok) {
			mPlaces.clear();
			mPlaces.addAll(places);
			mAdapter.notifyDataSetChanged();
		} else {
			UiUtils.toastShort(getActivity(), "Error loading results");
			Log.e("aaa", "error loading places results=" + errorIfNotOk);
		}
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Place p = mAdapter.getItem(position);
		destinationPicked(p.latitude() + "," + p.longitude(), p.name());
	}
	
	private class PlaceAdapter extends BaseAdapter {

		private Context mContext;
		private List<Place> mItems;
		
		public PlaceAdapter(Context context, List<Place> places) {
			mContext = context;
			mItems = places;
		}
		
		@Override
		public int getCount() {
			return mItems.size();
		}

		@Override
		public Place getItem(int position) {
			return mItems.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View view, ViewGroup parent) {
			if (view == null) {
				view = LayoutInflater.from(mContext).inflate(R.layout.item_destination, parent, false);
			}

			TextView name = (TextView) view.findViewById(R.id.item_destination_text_name);
			TextView address = (TextView) view.findViewById(R.id.item_destination_text_address);
			
			Place p = getItem(position);
			name.setText(p.name());
			address.setText(p.address());
			
			return view;
		}
	}
}

