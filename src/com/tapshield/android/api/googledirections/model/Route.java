package com.tapshield.android.api.googledirections.model;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.location.Location;

public class Route {

	private static final String JSON_SUMMARY = "summary";
	private static final String JSON_OVERVIEWPOLYLINE = "overview_polyline";
	private static final String JSON_OVERVIEWPOLYLINE_POINTS = "points";
	private static final String JSON_COPYRIGHTS = "copyrights";
	private static final String JSON_WARNINGS = "warnings";
	private static final String JSON_LEGS = "legs";
	private static final String JSON_DURATION = "duration";
	private static final String JSON_DURATION_VALUE = "value";
	private static final String JSON_DURATION_TEXT = "text";
	
	private long mDurationSeconds;
	private String mDurationText;
	private String mSummary;
	private String mEncodedOverviewPolyline;
	private String[] mWarnings;
	private String mCopyrights;
	
	private Route() {
		mDurationSeconds = -1;
	}
	
	public Route(String encodedOverviewPolyline) {
		this();
		mEncodedOverviewPolyline = encodedOverviewPolyline;
	}
	
	public Route setSummary(String summary) {
		mSummary = summary;
		return this;
	}
	
	public Route setDuration(long durationSeconds, String durationText) {
		mDurationSeconds = durationSeconds;
		mDurationText = durationText;
		return this;
	}
	
	public Route setWarnings(String[] warnings) {
		mWarnings = warnings;
		return this;
	}
	
	public Route setCopyrights(String copyrights) {
		mCopyrights = copyrights;
		return this;
	}
	
	public boolean hasSummary() {
		return mSummary != null;
	}
	
	public boolean hasDuration() {
		return mDurationSeconds >= 0 && mDurationText != null;
	}
	
	public boolean hasWarnings() {
		return mWarnings != null && mWarnings.length > 0;
	}
	
	public boolean hasCopyrights() {
		return mCopyrights != null;
	}
	
	public long durationSeconds() {
		return mDurationSeconds;
	}
	
	public String durationText() {
		return mDurationText;
	}
	
	public String summary() {
		return mSummary;
	}
	
	public String encodedOverviewPolyline() {
		return mEncodedOverviewPolyline;
	}
	
	//ALGORITHM BY 'Ismail Habib'
	//REFERENCES AT http://www.geekyblogger.com/2010/12/decoding-polylines-from-google-maps.html
	//BASED ON 'Jeffrey Sambells's'
	//AT http://jeffreysambells.com/2010/05/27/decoding-polylines-from-google-maps-direction-api-with-java
	//AS OF APR 18 2014
	
	public List<Location> decodedOverviewPolyline() {
		List<Location> poly = new ArrayList<Location>();
		
		int index = 0, len = mEncodedOverviewPolyline.length();
		int lat = 0, lng = 0;
		
		while (index < len) {
			
			int b, shift = 0, result = 0;
			
			do {
				b = mEncodedOverviewPolyline.charAt(index++) - 63;
				result |= (b & 0x1f) << shift;
				shift += 5;
			} while (b >= 0x20);
			
			int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
			lat += dlat;
			shift = 0;
			result = 0;
			
			do {
				b = mEncodedOverviewPolyline.charAt(index++) - 63;
				result |= (b & 0x1f) << shift;
				shift += 5;
			} while (b >= 0x20);
			
			int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
			lng += dlng;
			Location p = new Location(getClass().toString());
			p.setLatitude((double) lat / 1E5);
			p.setLongitude((double) lng / 1E5);
			poly.add(p);
		}
		return poly;
	}
	
	public String[] warnings() {
		return mWarnings;
	}
	
	public String copyrights() {
		return mCopyrights;
	}
	
	public static final Route fromJson(JSONObject o) {
		Route r = null;
		
		try {
			String encodedOverviewPolyline = o.getJSONObject(JSON_OVERVIEWPOLYLINE)
					.getString(JSON_OVERVIEWPOLYLINE_POINTS);
			r = new Route(encodedOverviewPolyline);
			
			if (o.has(JSON_SUMMARY)) {
				r.setSummary(o.getString(JSON_SUMMARY));
			}
			
			if (o.has(JSON_COPYRIGHTS)) {
				r.setCopyrights(o.getString(JSON_COPYRIGHTS));
			}
			
			if (o.has(JSON_WARNINGS)) {
				JSONArray warningsJson = o.getJSONArray(JSON_WARNINGS);
				int len = warningsJson.length();
				String[] warnings = new String[len];
				for (int w = 0; w < len; w++) {
					warnings[w] = warningsJson.getString(w);
				}
			}
			
			//assuming only one leg (index 0), waypoints are not supported at this time
			JSONObject leg = o.getJSONArray(JSON_LEGS).getJSONObject(0);
			
			if (leg.has(JSON_DURATION)) {
				JSONObject duration = leg.getJSONObject(JSON_DURATION);
				if (duration.has(JSON_DURATION_TEXT) && duration.has(JSON_DURATION_VALUE)) {
					r.setDuration(
							duration.getLong(JSON_DURATION_VALUE),
							duration.getString(JSON_DURATION_TEXT));
				}
			}
		} catch (Exception e) {
			r = null;
		}
		
		return r;
	}
}
