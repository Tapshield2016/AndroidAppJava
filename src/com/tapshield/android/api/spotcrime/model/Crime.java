package com.tapshield.android.api.spotcrime.model;

import org.json.JSONException;
import org.json.JSONObject;

public class Crime {

	public static final String PARAM_CDID = "cdid";
	public static final String PARAM_LINK = "link";
	public static final String PARAM_TYPE = "type";
	public static final String PARAM_LAT = "lat";
	public static final String PARAM_LON = "lon";
	public static final String PARAM_DATE = "date";
	public static final String PARAM_DESCRIPTION = "description";
	public static final String PARAM_ADDRESS = "address";
	
	private int mId;
	private String mUrl;
	private String mType;
	private String mAddress;
	private String mDate;
	private String mDescription;
	private double mLatitude;
	private double mLongitude;
	
	public Crime(int id, String url, String type) {
		mId = id;
		mUrl = url;
		mType = type;
		
		mAddress = null;
		mDate = null;
		mDescription = null;
		mLatitude = -1;
		mLongitude = -1;
	}
	
	public int getId() {
		return mId;
	}
	
	public String getUrl() {
		return mUrl;
	}
	
	public String getType() {
		return mType;
	}
	
	public void setAddress(String address) {
		mAddress = address;
	}
	
	public String getAddress() {
		return mAddress;
	}
	
	public void setDate(String date) {
		mDate = date;
	}
	
	public String getDate() {
		return mDate;
	}
	
	public void setDescription(String description) {
		mDescription = description;
	}
	
	public String getDescription() {
		return mDescription;
	}
	
	public void setLocation(double latitude, double longitude) {
		mLatitude = latitude;
		mLongitude = longitude;
	}
	
	public double getLatitude() {
		return mLatitude;
	}
	
	public double getLongitude() {
		return mLongitude;
	}
	
	public static JSONObject serialize(Crime c) {
		JSONObject o = new JSONObject();
		
		try {
			o.put(PARAM_CDID, c.mId);
			o.put(PARAM_LINK, c.mUrl);
			o.put(PARAM_TYPE, c.mType);
			
			if (c.mLatitude != -1) {
				o.put(PARAM_LAT, c.mLatitude);
			}
			
			if (c.mLongitude != -1) {
				o.put(PARAM_LON, c.mLongitude);
			}
			
			if (c.mAddress != null) {
				o.put(PARAM_ADDRESS, c.mAddress);
			}
			
			if (c.mDate != null) {
				o.put(PARAM_DATE, c.mDate);
			}
			
			if (c.mDescription != null) {
				o.put(PARAM_DESCRIPTION, c.mDescription);
			}
		} catch (JSONException e) {
			o = null;
		}
		
		return o;
	}
	
	public static Crime deserialize(JSONObject j) {
		try {
			int cdid = j.getInt(PARAM_CDID);
			String url = j.getString(PARAM_LINK);
			String type = j.getString(PARAM_TYPE);
			
			Crime c = new Crime(cdid, url, type);
			
			if (j.has(PARAM_ADDRESS)) {
				c.setAddress(j.getString(PARAM_ADDRESS));
			}
			
			if (j.has(PARAM_DATE)) {
				c.setDate(j.getString(PARAM_DATE));
			}
			
			if (j.has(PARAM_DESCRIPTION)) {
				c.setDescription(j.getString(PARAM_DESCRIPTION));
			}
			
			if (j.has(PARAM_LAT) && j.has(PARAM_LON)) {
				c.setLocation(j.getDouble(PARAM_LAT), j.getDouble(PARAM_LON));
			}
			
			return c;
		} catch (Exception e) {
			return null;
		}
	}
}
