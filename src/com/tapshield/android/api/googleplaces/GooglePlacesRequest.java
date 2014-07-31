package com.tapshield.android.api.googleplaces;

import com.tapshield.android.utils.StringUtils;

public class GooglePlacesRequest {

	private static final String TYPE_SEARCH = "textsearch";
	private static final String TYPE_NEARBY = "nearbysearch";
	
	private static final String OUTPUT_JSON = "json";
	private static final String OUTPUT_XML = "xml";
	
	private static final String PARAM_KEY = "key";
	private static final String PARAM_LOCATION = "location";
	private static final String PARAM_RADIUS = "radius";
	private static final String PARAM_NAME = "name";
	private static final String PARAM_QUERY = "query";
	private static final String PARAM_SENSOR = "sensor";
	
	private String mType = TYPE_SEARCH;
	private String mOutput = OUTPUT_JSON;
	private String mKey;
	private String mToSearchFor;
	private String mLocation;
	private int mRadius = -1;
	private boolean mSensor = true;
	
	public GooglePlacesRequest(GooglePlacesConfig config) {
		mKey = config.key();
	}
	
	public GooglePlacesRequest setTypeNearby() {
		mType = TYPE_NEARBY;
		return this;
	}
	
	public GooglePlacesRequest setTypeSearch() {
		mType = TYPE_SEARCH;
		return this;
	}
	
	public GooglePlacesRequest setSearch(String search) {
		mToSearchFor = search.trim().replaceAll(StringUtils.REGEX_WHITESPACES, "+");
		return this;
	}
	
	public GooglePlacesRequest setLocation(double latitude, double longitude, int radiusMeters) {
		return setLocation(Double.toString(latitude), Double.toString(longitude), radiusMeters);
	}
	
	public GooglePlacesRequest setLocation(String latitude, String longitude, int radiusMeters) {
		mLocation = latitude + "," + longitude;
		mRadius = radiusMeters;
		return this;
	}
	
	public final String getUrlSuffix() {
		String suffix = mType + "/" + mOutput + "?";
		
		if (mKey != null && !mKey.isEmpty()) {
			suffix = addGetParam(suffix, PARAM_KEY, mKey);
		}
		
		if (mToSearchFor != null && !mToSearchFor.isEmpty()) {
			suffix = addGetParam(suffix,
					mType == TYPE_NEARBY ? PARAM_NAME : PARAM_QUERY, mToSearchFor);
		}
		
		if (mLocation != null && !mLocation.isEmpty() && mRadius > 0) {
			suffix = addGetParam(suffix, PARAM_LOCATION, mLocation);
			suffix = addGetParam(suffix, PARAM_RADIUS, Integer.toString(mRadius));
		}
		
		suffix = addGetParam(suffix, PARAM_SENSOR, Boolean.toString(mSensor));
		
		return suffix;
	}
	
	private static final String addGetParam(String currentUrl, String param, String value) {
		if (!currentUrl.endsWith("?")) {
			currentUrl += "&";
		}
		
		return currentUrl + param + "=" + value;
	}
}
