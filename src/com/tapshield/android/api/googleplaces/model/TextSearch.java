package com.tapshield.android.api.googleplaces.model;

public class TextSearch extends Search {

	private static final String PARAM_QUERY = "query";
	
	public TextSearch(String query) {
		super("textsearch");
		addParam(PARAM_QUERY, replaceWhitespaceWithPlus(query));
	}
	
	public TextSearch location(double latitude, double longitude, int radiusMeters) {
		return location(Double.toString(latitude), Double.toString(longitude), radiusMeters);
	}
	
	public TextSearch location(String latitude, String longitude, int radiusMeters) {
		addParam(PARAM_LOCATION, latitude + "," + longitude);
		addParam(PARAM_RADIUS, Integer.toString(radiusMeters));
		return this;
	}
}
