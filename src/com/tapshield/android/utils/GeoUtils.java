package com.tapshield.android.utils;

import java.util.List;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import com.tapshield.android.api.model.Agency;
import com.tapshield.android.api.model.DispatchCenter;
import com.tapshield.android.api.model.Region;

public class GeoUtils {

	public static final String PROVIDER_GPS = LocationManager.GPS_PROVIDER;
	public static final String PROVIDER_NETWORK = LocationManager.NETWORK_PROVIDER;
	
	public static boolean isProviderEnabled(final String provider, final Context context) {
		LocationManager locationManager = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);
		return locationManager.isProviderEnabled(provider);
	}

	public static boolean isThereOverhang(Context c, Location l, List<Location> boundaries) {
		float minDistance = minDistanceBetweenLocationAndEdges(l, boundaries);
		return l.getAccuracy() > (2 * minDistance);
	}
	
	public static boolean isLocationInsideAgency(Context c, final Location location,
			Agency agency) {
		
		//assuming agency is not null
		if (!agency.hasRegions()) {
			return true;
		}
		
		if (location == null) {
			return false;
		}
		
		for (Region r : agency.regions) {
			if (r.hasBoundaries()) {
				List<Location> boundaries = Agency.getBoundariesOfRegion(r);
				Log.i("javelin", "checking for location inside region " + r.mUrl);
				if (isLocationInsideBoundaries(c, location, boundaries)) {
					
					Log.i("javelin", "inside region " + r.mUrl);
					
					//check for open dispatch centers in current region before returning true
					//check being done here for the time being
					if (agency.dispatchCenters != null) {
						
						//primary dispatch centers
						for (DispatchCenter dc : agency.dispatchCenters) {
							if (dc.mUrl.trim().equals(r.mPrimaryDispatchCenterUrl) && dc.isOpen()) {
								Log.i("javelin", "primary dispatcher " + dc.mUrl  + " is open");
								return true;
							}
						}
						
						//secondary dispatch centers
						for (DispatchCenter dc : agency.dispatchCenters) {
							if (dc.mUrl.trim().equals(r.mSecondaryDispatchCenterUrl) && dc.isOpen()) {
								Log.i("javelin", "secondary dispatcher " + dc.mUrl  + " is open");
								return true;
							}
						}
						
						//fallback dispatch centers
						for (DispatchCenter dc : agency.dispatchCenters) {
							if (dc.mUrl.trim().equals(r.mFallbackDispatchCenterUrl) && dc.isOpen()) {
								Log.i("javelin", "fallback dispatcher " + dc.mUrl  + " is open");
								return true;
							}
						}
					}
				}
			}
		}
		
		return false;
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
	    
	    //for (Geofence g : geofences) {
	        
	    	int len = boundaries.size();
	    	//len = g.size(); //list of location objects within current geofence
		    Location a, b;
		    
		    //check for current geofence
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
		    
		    //if inside one geofence (current one) break it and have the function return inside
		    // which is true at this point
		    
		    /*
		    if (inside) {
		    	break;
		    }
		    */
		//}
	    return inside;
	}
	
	public static float minDistanceBetweenLocationAndRegions(final Location location,
			List<Region> regions) {

		float min = Float.MAX_VALUE;
		
		if (location == null || regions == null) {
			return min;
		}
		
		for (Region r : regions) {
			min = minDistanceBetweenLocationAndEdges(location, Agency.getBoundariesOfRegion(r));
		}
		
		return min;
	}
	
	public static float minDistanceBetweenLocationAndEdges(final Location location, List<Location> list) {
		
		float min = Float.MAX_VALUE;
		
		if (location == null || list == null || list.isEmpty()) {
			Log.e("tapshield-location", "Error arg(s) null");
			return min;
		}
		//for (Geofence g : geofences) {
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
		//}
		
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
