package com.tapshield.android.api.googledirections.model;

import com.google.gson.annotations.SerializedName;

public class Bounds {

	@SerializedName("southwest")
	private BoundsCoordinates mSw;
	
	@SerializedName("northeast")
	private BoundsCoordinates mNe;
	
	public double swLat() {
		return mSw.mLat;
	}
	
	public double swLon() {
		return mSw.mLon;
	}
	
	public double neLat() {
		return mNe.mLat;
	}
	
	public double neLon() {
		return mNe.mLon;
	}
	
	public static class BoundsCoordinates {
		
		@SerializedName("lat")
		public double mLat;
		
		@SerializedName("lng")
		public double mLon;
	}
}
