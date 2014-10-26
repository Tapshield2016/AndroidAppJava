package com.tapshield.android.api.googleplaces.model;


public class AutocompleteSearch extends Search {

	public static final String RESULTS = "predictions";
	
	public static final String TYPES_GEOCODE = "geocode";
	public static final String TYPES_ADDRESS = "address";
	public static final String TYPES_ESTABLISHMENT = "establishment";
	public static final String TYPES_REGIONS = "(regions)";
	public static final String TYPES_CITIES = "(cities)";
	
	private static final String PARAM_INPUT = "input";
	private static final String PARAM_TYPES = "types";

	private static final String DEFAULT_NO_LOC_BIASING_COORDINATES_LAT = "0";
	private static final String DEFAULT_NO_LOC_BIASING_COORDINATES_LON = "0";
	private static final int DEFAULT_NO_LOC_BIASING_RADIUS = 20000000;
	
	public AutocompleteSearch(String input) {
		super("autocomplete", "predictions");
		addParam(PARAM_INPUT, replaceWhitespaceWithPlus(input));
	}
	
	public AutocompleteSearch location(double latitude, double longitude, int radiusMeters) {
		return location(Double.toString(latitude), Double.toString(longitude), radiusMeters);
	}
	
	public AutocompleteSearch location(String latitude, String longitude, int radiusMeters) {
		addParam(PARAM_LOCATION, latitude + "," + longitude);
		addParam(PARAM_RADIUS, Integer.toString(radiusMeters));
		return this;
	}
	
	public AutocompleteSearch types(String types) {
		if (types.equals(TYPES_GEOCODE)
				|| types.equals(TYPES_ADDRESS)
				|| types.equals(TYPES_ESTABLISHMENT)
				|| types.equals(TYPES_REGIONS)
				|| types.equals(TYPES_CITIES)) {
			addParam(PARAM_TYPES, types);
		}
		return this;
	}

	public AutocompleteSearch disableLocationBiasing() {
		location(DEFAULT_NO_LOC_BIASING_COORDINATES_LAT, DEFAULT_NO_LOC_BIASING_COORDINATES_LON,
				DEFAULT_NO_LOC_BIASING_RADIUS);
		return this;
	}
}
