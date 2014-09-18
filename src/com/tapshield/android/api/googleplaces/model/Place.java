package com.tapshield.android.api.googleplaces.model;

import com.google.gson.annotations.SerializedName;

public class Place {

	@SerializedName("place_id")
	private String mId;
	
	@SerializedName("formatted_address")
	private String mAddress;
	
	@SerializedName("geometry")
	private Geometry mGeometry;

	@SerializedName("name")
	private String mName;
	
	@SerializedName("types")
	private String[] mTypes;
	
	@SerializedName("rating")
	private float mRating = 0f;
	
	public boolean hasAddress() {
		return mAddress != null;
	}
	
	public String address() {
		return mAddress;
	}
	
	public double latitude() {
		return mGeometry.mLocation.mLatitude;
	}
	
	public double longitude() {
		return mGeometry.mLocation.mLongitude;
	}
	
	public String placeId() {
		return mId;
	}
	
	public String name() {
		return mName;
	}
	
	public boolean hasTypes() {
		return mTypes != null && mTypes.length > 0;
	}
	
	public String[] types() {
		return mTypes;
	}
	
	public boolean hasRating() {
		return mRating >= 1f;
	}
	
	public float rating() {
		return mRating;
	}
	
	private class Geometry {
		@SerializedName("location")
		private Location mLocation;
	}
	
	private class Location {
		@SerializedName("lat")
		private double mLatitude;
		
		@SerializedName("lng")
		private double mLongitude;
	}
}
