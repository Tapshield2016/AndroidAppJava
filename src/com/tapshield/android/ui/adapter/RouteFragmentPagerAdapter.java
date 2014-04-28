package com.tapshield.android.ui.adapter;

import java.util.Collections;
import java.util.List;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.tapshield.android.api.googledirections.model.Route;
import com.tapshield.android.ui.fragment.RouteFragment;

public class RouteFragmentPagerAdapter extends FragmentPagerAdapter {

	private List<Route> mRoutes;
	private RouteFragment[] mFragments;
	
	public RouteFragmentPagerAdapter(FragmentManager fm) {
		super(fm);
		mRoutes = Collections.emptyList();
	}
	
	public void setRoutes(List<Route> routes) {
		mRoutes = routes;
		mFragments = new RouteFragment[mRoutes.size()];
		notifyDataSetChanged();
	}
	
	@Override
	public Fragment getItem(int position) {
		
		if (mFragments[position] == null) {
			Bundle extras = new Bundle();
			extras.putString(RouteFragment.EXTRA_ETA, mRoutes.get(position).durationText());
			extras.putString(RouteFragment.EXTRA_SUMMARY, mRoutes.get(position).summary());
			
			RouteFragment f = new RouteFragment();
			f.setArguments(extras);
			mFragments[position] = f;
		}
		
		return mFragments[position];
	}

	@Override
	public int getCount() {
		return mRoutes.size();
	}
}
