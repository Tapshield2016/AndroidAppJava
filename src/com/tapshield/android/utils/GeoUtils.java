package com.tapshield.android.utils;

import java.util.List;

import android.content.Context;
import android.location.Location;
import android.util.Log;

public class GeoUtils {

	public static boolean isThereOverhang(Context c, Location l, List<Location> boundaries) {
		float minDistance = minDistanceBetweenLocationAndEdges(l, boundaries);
		return l.getAccuracy() > (2 * minDistance);
	}
	
	public static boolean isLocationInsideBoundaries(Context c, final Location location,
			List<Location> boundaries) {
		
		if (boundaries == null || boundaries.isEmpty()) {
			return true;
		}
		
		if (location == null) {
			return false;
		}
	    
	    boolean inside = false;
        int len = boundaries.size();
	    Location a, b;
	    
	    for (int ia = 0; ia < len; ia++) {
	    	//A last vertex? B = first. Else, B = vertex(A+1)
	    	int ib = ia == len-1 ? 0 : ia + 1;
	    	
	        a = boundaries.get(ia);
	        b = boundaries.get(ib);
	    
	        //check for same vertex being used for first and last
	        if ((ia == len - 1) && (a.getLatitude() == b.getLatitude()) && (a.getLongitude() == b.getLongitude())) {
	        	break;
	        }
	        
	        boolean isCrossing =
	        		(a.getLatitude() > location.getLatitude() && b.getLatitude() <= location.getLatitude())
	        		|| (a.getLatitude() < location.getLatitude() && b.getLatitude() >= location.getLatitude());
	        
	        boolean isAhead = (a.getLongitude() > location.getLongitude() || b.getLongitude() > location.getLongitude());
	        
	        if ((isCrossing && isAhead)) {
	            inside = !inside;
	        }
	    }
	    return inside;
	}
	
	public static float minDistanceBetweenLocationAndEdges(final Location location, List<Location> list) {
		
		float min = Float.MAX_VALUE;
		
		if (location == null || list == null || list.isEmpty()) {
			Log.e("tapshield-location", "Error arg(s) null");
			return min;
		}
		
		//get a and b, if a is last vertex then b should be first (index 0)
		for (int i = 0; i < list.size(); i++) {
			Location a = list.get(i);
			Location b = i == list.size() - 1 ? list.get(0) : list.get(i + 1);
			
			float d = minDistanceBetweenLocationAndEdge(location, a, b);
			Log.d("tapshield-location", "Min distance between point and edge " + i + "/" + (list.size()-1));
			Log.i("tapshield-location", " d=" + d + " [p=" + location.toString()
					+ " a=" + a.toString() + " b=" + b.toString() + "]");
			
			if (d < min) {
				min = d;
			}
		}
		
		Log.d("tapshield-location", "Min to be returned=" + min);

		return min;
	}
	
	public static float minDistanceBetweenLocationAndEdge(Location point, Location a, Location b) {
		float d = Float.MAX_VALUE;
		if (point == null || a == null || b == null) {
			Log.e("tapshield-location", "Error arg(s) null");
			return d;
		}
		
		Log.d("tapshield-location", "analyzing edge");
		
		double px = point.getLatitude();
		double py = point.getLongitude();
		double x1 = a.getLatitude();
		double y1 = a.getLongitude();
		double x2 = b.getLatitude();
		double y2 = b.getLongitude();
		
		double mag = getLineMagnitude(x1, y1, x2, y2);
		
		if (mag < 0.00000001) {
			d = 9999;
		}
		
		double k = (((px - x1) * (x2 - x1)) + ((py - y1) * (y2 - y1)));
		double u = k / (mag * mag);
		
		if (u < 0.00001 || u > 1) {
			double ix = getLineMagnitude(px, py, x1, y1);
			double iy = getLineMagnitude(px, py, x2, y2);
			if (ix > iy) {
				d = b.distanceTo(point);
			} else {
				d = a.distanceTo(point);
			}
		} else {
			double xu = x1 + u * (x2 - x1);
			double yu = y1 + u * (y2 - y1);
			
			Location nearest = new Location("");
			
			nearest.setLatitude(xu);
			nearest.setLongitude(yu);
			
			d = point.distanceTo(nearest);
			
			
			Log.d("tapshield-location", " location=" + nearest.toString());
		}
		Log.d("tapshield-location", " edgeA=" + a.toString());
		Log.d("tapshield-location", " edgeB=" + b.toString());
		Log.d("tapshield-location", " d=" + d);
		
		return d;
	}
	
	private static double getLineMagnitude(double x1, double y1, double x2, double y2) {
		return Math.sqrt(Math.pow((x2 - x1), 2) + Math.pow((y2 - y1), 2));
	}
}
