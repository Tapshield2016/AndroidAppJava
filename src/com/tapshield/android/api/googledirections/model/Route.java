package com.tapshield.android.api.googledirections.model;

import java.util.ArrayList;
import java.util.List;

import android.location.Location;

import com.google.gson.annotations.SerializedName;

public class Route {

	@SerializedName("summary")
	private String mSummary;
	
	@SerializedName("warnings")
	private List<String> mWarnings;

	@SerializedName("overview_polyline")
	private OverviewPolyline mOverview;
	
	@SerializedName("legs")
	private List<Leg> mLegs;
	
	@SerializedName("copyrights")
	private String mCopyrights;
	
	@SerializedName("bounds")
	private Bounds mBounds;
	
	public String[] warnings() {
		int len = mWarnings.size();
		String[] warnings = new String[len];
		for (int i = 0; i < len; i++) {
			warnings[i] = mWarnings.get(i);
		}
		return warnings;
	}
	
	public String summary() {
		return mSummary;
	}
	
	public String copyrights() {
		return mCopyrights;
	}
	
	//assuming only one leg is given--app not supporting waypoints
	public long distanceValue() {
		return mLegs.get(0).distanceValue();
	}
	
	public String distanceText() {
		return mLegs.get(0).distanceText();
	}
	
	public long durationSeconds() {
		return mLegs.get(0).durationSeconds();
	}
	
	public String durationText() {
		return mLegs.get(0).durationText();
	}
	
	public double boundsSwLat() {
		return mBounds.swLat();
	}
	
	public double boundsSwLon() {
		return mBounds.swLon();
	}
	
	public double boundsNeLat() {
		return mBounds.neLat();
	}
	
	public double boundsNeLon() {
		return mBounds.neLon();
	}
	
	private String overviewPolyline() {
		return mOverview.mPoints;
	}

	public final List<Location> decodedOverviewPolyline() {
		List<Location> poly = new ArrayList<Location>();
		
		String encoded = overviewPolyline();
		
		int index = 0, len = encoded.length();
		int lat = 0, lng = 0;
		
		while (index < len) {
			
			int b, shift = 0, result = 0;
			
			do {
				b = encoded.charAt(index++) - 63;
				result |= (b & 0x1f) << shift;
				shift += 5;
			} while (b >= 0x20);
			
			int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
			lat += dlat;
			shift = 0;
			result = 0;
			
			do {
				b = encoded.charAt(index++) - 63;
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
	
	public static class OverviewPolyline {
		@SerializedName("points")
		private String mPoints;
	}
	
	
	/*
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
	*/
}
