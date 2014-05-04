package com.tapshield.android.api.googledirections.model;

import com.google.gson.annotations.SerializedName;
import com.tapshield.android.R.menu;
import com.tapshield.android.api.googledirections.model.Bounds.BoundsCoordinates;

public class Leg {

	@SerializedName("distance")
	private Distance mDistance;
	
	@SerializedName("duration")
	private Duration mDuration;

	@SerializedName("start_location")
	BoundsCoordinates mStartLocation;
	
	@SerializedName("end_location")
	BoundsCoordinates mEndLocation;
	
	public long distanceValue() {
		return mDistance.mValue;
	}
	
	public String distanceText() {
		return mDistance.mText;
	}
	
	public long durationSeconds() {
		return mDuration.mValue;
	}
	
	public String durationText() {
		return mDuration.mText;
	}
	
	public double startLat() {
		return mStartLocation.mLat;
	}
	
	public double startLon() {
		return mStartLocation.mLon;
	}
	
	public double endLat() {
		return mEndLocation.mLat;
	}
	
	public double endLon() {
		return mEndLocation.mLon;
	}
	
	public static class Distance {
		@SerializedName("value")
		private long mValue;
		
		@SerializedName("text")
		private String mText;
	}
	
	public static class Duration {
		@SerializedName("value")
		private long mValue;
		
		@SerializedName("text")
		private String mText;
	}
}
