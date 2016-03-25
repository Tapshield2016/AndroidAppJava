package com.tapshield.android.api.googleplaces.model;

import java.util.Map;

public class NearbySearch extends Search {

	public static final String RANKBY_PROMINENCE = "prominence";
	public static final String RANKBY_DISTANCE = "distance";
	
	private static final String PARAM_KEYWORD = "keyword";
	private static final String PARAM_NAME = "name";
	
	public NearbySearch(double latitude, double longitude, int radiusMeters) {
		this(Double.toString(latitude), Double.toString(longitude), radiusMeters);
	}
	
	public NearbySearch(String latitude, String longitude, int radiusMeters) {
		super("nearbysearch");
		addParam(PARAM_RANKBY, RANKBY_PROMINENCE);
		addParam(PARAM_LOCATION, latitude + "," + longitude);
		addParam(PARAM_RADIUS, Integer.toString(radiusMeters));
	}
	
	public NearbySearch rankby(String rankby) {
		if (rankby == RANKBY_PROMINENCE || rankby == RANKBY_DISTANCE) {
			addParam(PARAM_RANKBY, rankby);
		}
		return this;
	}
	
	public NearbySearch keyword(String keyword) {
		addParam(PARAM_KEYWORD, replaceWhitespaceWithPlus(keyword));
		return this;
	}
	
	public NearbySearch name(String name) {
		addParam(PARAM_NAME, replaceWhitespaceWithPlus(name));
		return this;
	}

	@Override
	protected void onPreGetParams(Map<String, String> params) {
		//just before getParams() gets called, apply rule of no radius if rankby=distance
		if (params.containsKey(PARAM_RANKBY) && params.get(PARAM_RANKBY).toString().equals(RANKBY_DISTANCE)) {
			removeParam(PARAM_RADIUS);
		}
	}
}
