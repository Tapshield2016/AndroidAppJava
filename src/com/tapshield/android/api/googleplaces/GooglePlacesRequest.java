package com.tapshield.android.api.googleplaces;

public class GooglePlacesRequest {

	public static final String TYPE_SEARCH = "textsearch";
	
	public static final String OUTPUT_JSON = "json";
	public static final String OUTPUT_XML = "xml";
	
	private static final String PARAM_KEY = "key";
	private static final String PARAM_LOCATION = "location";
	private static final String PARAM_RADIUS = "radius";
	private static final String PARAM_QUERY = "query";
	private static final String PARAM_SENSOR = "sensor";
	
	private static final String REGEX_WHITESPACES = "\\s+";
	
	private String mType = TYPE_SEARCH;
	private String mOutput = OUTPUT_JSON;
	private String mKey;
	private String mQuery;
	private String mLocation;
	private int mRadius = -1;
	private boolean mSensor = true;
	
	public GooglePlacesRequest(GooglePlacesConfig config, String query) {
		mKey = config.key();
		mQuery = query;
	}
	
	public GooglePlacesRequest setType(String type) {
		mType = type;
		return this;
	}
	
	public GooglePlacesRequest setQuery(String query) {
		mQuery = query.trim().replaceAll(REGEX_WHITESPACES, "+");
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
		
		if (mQuery != null && !mQuery.isEmpty()) {
			suffix = addGetParam(suffix, PARAM_QUERY, mQuery);
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
