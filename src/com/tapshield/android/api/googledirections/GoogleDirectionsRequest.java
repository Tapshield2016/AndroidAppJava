package com.tapshield.android.api.googledirections;

import com.tapshield.android.utils.StringUtils;

public class GoogleDirectionsRequest {

	public static final String OUTPUT_JSON = "json";
	public static final String OUTPUT_XML = "xml";
	
	public static final String MODE_DRIVING = "driving";
	public static final String MODE_WALKING = "walking";
	
	public static final String UNITS_IMPERIAL = "imperial";
	public static final String UNITS_METRIC = "metric";

	private static final String PARAM_KEY = "key";
	private static final String PARAM_ORIGIN = "origin";
	private static final String PARAM_DESTINATION = "destination";
	private static final String PARAM_SENSOR = "sensor";
	private static final String PARAM_MODE = "mode";
	private static final String PARAM_UNITS = "units";
	private static final String PARAM_ALTERNATIVES = "alternatives";
	
	private String mUrl = "https://maps.googleapis.com/maps/api/directions/";
	private String mKey;
	private String mOutput = OUTPUT_JSON;
	private String mOrigin;
	private String mDestination;
	private String mMode = MODE_DRIVING;
	private String mUnits = UNITS_IMPERIAL;
	private boolean mAlternatives = false;
	private boolean mSensor = true;
	
	public GoogleDirectionsRequest(GoogleDirectionsConfig config, String origin, String destination) {
		mKey = config.key();
		mOrigin = origin.trim().replaceAll(StringUtils.REGEX_WHITESPACES, "+");
		mDestination = destination.trim().replaceAll(StringUtils.REGEX_WHITESPACES, "+");
	}
	
	public GoogleDirectionsRequest(GoogleDirectionsConfig config, double originLatitude, double originLongitude,
			double destinationLatitude, double destinationLongitude) {
		this(config,
				Double.toString(originLatitude) + "," + Double.toString(originLongitude),
				Double.toString(destinationLatitude) + "," + Double.toString(destinationLongitude));
	}
	
	public GoogleDirectionsRequest setMode(String mode) {
		if (mode == MODE_DRIVING || mode == MODE_WALKING) {
			mMode = mode;
		}
		return this;
	}
	
	public GoogleDirectionsRequest setRequestAlternatives() {
		mAlternatives = true;
		return this;
	}
	
	public String url() {
		String url = mUrl + mOutput + "?";
		
		url = addGetParam(url, PARAM_KEY, mKey);
		url = addGetParam(url, PARAM_ORIGIN, mOrigin);
		url = addGetParam(url, PARAM_DESTINATION, mDestination);
		url = addGetParam(url, PARAM_SENSOR, Boolean.toString(mSensor));
		url = addGetParam(url, PARAM_MODE, mMode);
		url = addGetParam(url, PARAM_UNITS, mUnits);
		url = addGetParam(url, PARAM_ALTERNATIVES, Boolean.toString(mAlternatives));
		
		return url;
	}
	
	private static final String addGetParam(String currentUrl, String param, String value) {
		if (!currentUrl.endsWith("?")) {
			currentUrl += "&";
		}
		
		return currentUrl + param + "=" + value;
	}
}
