package com.tapshield.android.api.googledirections.model;

import com.google.gson.annotations.SerializedName;

public class Leg {

	@SerializedName("distance")
	private Distance mDistance;
	
	@SerializedName("duration")
	private Duration mDuration;
	
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
