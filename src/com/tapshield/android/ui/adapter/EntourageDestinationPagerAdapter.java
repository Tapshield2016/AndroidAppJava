package com.tapshield.android.ui.adapter;

import java.util.List;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.tapshield.android.api.googleplaces.model.Place;
import com.tapshield.android.ui.fragment.EntouragePagerPlaceFragment;

public class EntourageDestinationPagerAdapter extends FragmentStatePagerAdapter {

	private List<Place> mPlaces;

	public EntourageDestinationPagerAdapter(FragmentManager fm, List<Place> places) {
		super(fm);
		mPlaces = places;
	}
	
	@Override
	public Fragment getItem(int position) {
		
		Place p = mPlaces.get(position);
		
		Bundle args = new Bundle();
		args.putString(EntouragePagerPlaceFragment.EXTRA_NAME, p.name());
		args.putString(EntouragePagerPlaceFragment.EXTRA_DESCRIPTION,
				p.hasAddress() ? p.address() : "");
		if (p.hasPhotos()) {
			args.putString(EntouragePagerPlaceFragment.EXTRA_PHOTO, p.photos().get(0).reference());
		}
		
		EntouragePagerPlaceFragment f = new EntouragePagerPlaceFragment();
		f.setArguments(args);
		
		return f;
	}
	
	//this method forces a instantiation of items
	@Override
	public int getItemPosition(Object object) {
		return POSITION_NONE;
	}

	@Override
	public int getCount() {
		return mPlaces.size();
	}
}
