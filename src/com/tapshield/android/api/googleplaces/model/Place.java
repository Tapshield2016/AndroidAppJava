package com.tapshield.android.api.googleplaces.model;

import org.json.JSONArray;
import org.json.JSONObject;

public class Place {

	private static final String PARAM_ADDRESS = "formatted_address";
	private static final String PARAM_GEOMETRY = "geometry";
	private static final String PARAM_LOCATION = "location";
	private static final String PARAM_LATITUDE = "lat";
	private static final String PARAM_LONGITUDE = "lng";
	private static final String PARAM_REFERENCE = "reference";
	private static final String PARAM_NAME = "name";
	private static final String PARAM_TYPES = "types";
	private static final String PARAM_RATING = "rating";
	
	private String mAddress;
	private double mLatitude;
	private double mLongitude;
	private String mReference;
	private String mName;
	private String[] mTypes;
	private float mRating;
	
	private Place() {
		mAddress = new String();
		mTypes = new String[0];
		mRating = 0;
	}
	
	public String address() {
		return mAddress;
	}
	
	public double latitude() {
		return mLatitude;
	}
	
	public double longitude() {
		return mLongitude;
	}
	
	public String detailReference() {
		return mReference;
	}
	
	public String name() {
		return mName;
	}
	
	public String[] types() {
		return mTypes;
	}
	
	public float rating() {
		return mRating;
	}
	
	public static final Place fromJson(JSONObject o) {
		Place p = new Place();
		
		try {
			
			p.mName = o.getString(PARAM_NAME);
			p.mReference = o.getString(PARAM_REFERENCE);
			
			if (o.has(PARAM_ADDRESS)) {
				p.mAddress = o.getString(PARAM_ADDRESS);
			}
			
			if (o.has(PARAM_RATING)) {
				p.mRating = (float) o.getDouble(PARAM_RATING);
			}
			
			JSONObject geom = o.getJSONObject(PARAM_GEOMETRY);
			JSONObject loc = geom.getJSONObject(PARAM_LOCATION);
			p.mLatitude = loc.getDouble(PARAM_LATITUDE);
			p.mLongitude = loc.getDouble(PARAM_LONGITUDE);
			
			if (o.has(PARAM_TYPES)) {
				JSONArray types = o.getJSONArray(PARAM_TYPES);
				int len = types.length();

				p.mTypes = new String[len];
				for (int t = 0; t < len; t++) {
					p.mTypes[t] = types.getString(t);
				}
			}
		} catch (Exception e) {}
		
		return p;
	}
}
