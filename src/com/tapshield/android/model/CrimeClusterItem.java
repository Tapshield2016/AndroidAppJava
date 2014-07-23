package com.tapshield.android.model;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;
import com.tapshield.android.api.spotcrime.model.Crime;

public class CrimeClusterItem implements ClusterItem {

	private Crime mCrime;
	
	public CrimeClusterItem(Crime crime) {
		mCrime = crime;
	}
	
	public Crime getCrime() {
		return mCrime;
	}

	@Override
	public LatLng getPosition() {
		return new LatLng(mCrime.getLatitude(), mCrime.getLongitude());
	}
}
