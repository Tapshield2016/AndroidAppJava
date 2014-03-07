package com.tapshield.android.api.spotcrime;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.joda.time.DateTime;

public class SpotCrimeRequest {

	public static final String SORT_BY_DATE = "date";
	public static final String SORT_BY_DISTANCE = "distance";
	
	public static final String SORT_ORDER_ASCENDING = "ASC";
	public static final String SORT_ORDER_DESCENDING = "DESC";
	
	//required
	private static final String PARAM_LAT = "lat";
	private static final String PARAM_LON = "lon";
	private static final String PARAM_RADIUS = "radius";
	private static final String PARAM_KEY = "key";
	
	//optional
	private static final String PARAM_SINCE = "since";
	private static final String PARAM_MAX_RECORDS = "max_records";
	private static final String PARAM_SORT_BY = "sort_by";
	private static final String PARAM_SORT_ORDER = "sort_order";
	
	private static final String FORMAT_SINCE = "yyyy-MM-dd";
	
	private String mKey;
	private double mLatitude;
	private double mLongitude;
	private float mRadius;
	private long mSince;
	private int mMaxRecords;
	private String mSortBy;
	private String mSortOrder;
	
	public SpotCrimeRequest(SpotCrimeConfig config, double latitude, double longitude, float radius) {
		mKey = config.getKey();
		mLatitude = latitude;
		mLongitude = longitude;
		mRadius = radius;
		
		mSince = -1;
		mMaxRecords = -1;
		mSortBy = null;
		mSortOrder = null;
	}
	
	public SpotCrimeRequest setSince(long sinceMilli) {
		mSince = sinceMilli;
		return this;
	}
	
	public SpotCrimeRequest setMaxRecords(int maxRecords) {
		mMaxRecords = maxRecords;
		return this;
	}
	
	public SpotCrimeRequest setSortBy(String sortBy) {
		if (sortBy.equals(SORT_BY_DATE) || sortBy.equals(SORT_BY_DISTANCE)) {
			mSortBy = sortBy;
		}
		return this;
	}
	
	public SpotCrimeRequest setSortOrder(String sortOrder) {
		if (sortOrder.equals(SORT_ORDER_ASCENDING) || sortOrder.equals(SORT_ORDER_DESCENDING)) {
			mSortOrder = sortOrder;
		}
		return this;
	}

	public List<NameValuePair> getParams() {
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		
		params.add(new BasicNameValuePair(PARAM_KEY, mKey));
		params.add(new BasicNameValuePair(PARAM_LAT, Double.toString(mLatitude)));
		params.add(new BasicNameValuePair(PARAM_LON, Double.toString(mLongitude)));
		params.add(new BasicNameValuePair(PARAM_RADIUS, Float.toString(mRadius)));
		
		if (mSince != -1) {
			DateTime since = new DateTime(mSince);
			params.add(new BasicNameValuePair(PARAM_SINCE, since.toString(FORMAT_SINCE)));
		}
		
		if (mMaxRecords != -1) {
			params.add(new BasicNameValuePair(PARAM_MAX_RECORDS, Integer.toString(mMaxRecords)));
		}
		
		if (mSortBy != null) {
			params.add(new BasicNameValuePair(PARAM_SORT_BY, mSortBy));
		}
		
		if (mSortOrder != null) {
			params.add(new BasicNameValuePair(PARAM_SORT_ORDER, mSortOrder));
		}
		
		return params;
	}
}
