package com.tapshield.android.api.googledirections.model;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class GoogleDirectionsResponse {

	private static final String STATUS_OK = "OK";
	
	@SerializedName("status")
	private String mStatus;
	
	@SerializedName("routes")
	private List<Route> mRoutes;
	
	public List<Route> routes() {
		return mRoutes;
	}
	
	public boolean ok() {
		return mStatus.equals(STATUS_OK);
	}
}
