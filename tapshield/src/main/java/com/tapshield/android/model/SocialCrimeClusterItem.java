package com.tapshield.android.model;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;
import com.tapshield.android.api.model.SocialCrime;

public class SocialCrimeClusterItem implements ClusterItem {

	private SocialCrime mSocialCrime;
	
	public SocialCrimeClusterItem(SocialCrime socialCrime) {
		mSocialCrime = socialCrime;
	}
	
	public SocialCrime getSocialCrime() {
		return mSocialCrime;
	}
	
	@Override
	public LatLng getPosition() {
		return new LatLng(mSocialCrime.getLatitude(), mSocialCrime.getLongitude());
	}
}
