package com.tapshield.android.utils;

import java.util.List;

import org.json.JSONObject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.tapshield.android.R;
import com.tapshield.android.api.model.Agency;
import com.tapshield.android.api.model.Region;
import com.tapshield.android.api.model.UserProfile;

public class MapUtils {

	private static GoogleMap mMap;
	private static Marker mUserFlatMarker;
	private static Circle mUserAccuracy;
	
	public static void displayAgencyBoundaries(final Context context, final GoogleMap map,
			final Agency agency) {
		
		if (agency == null || (!agency.hasRegions() && !agency.hasBoundaries())) {
			return;
		}
		
		//by continuing, we expect to have at least one of the two: regions or simple boundaries
		
		final int solidColor = getSolidColorOffAgencyTheme(agency, "secondary_color");
		
		final int fillColor = Color.argb(
				51,
				Color.red(solidColor),
				Color.green(solidColor),
				Color.blue(solidColor));
		
		boolean complexBoundaries = agency.hasRegions();
		
		if (complexBoundaries) {
			for (Region r : agency.regions) {
				addPolygonWith(Agency.getBoundariesOfRegion(r), map, solidColor, fillColor);
			}
		} else {
			addPolygonWith(agency.getBoundaries(), map, solidColor, fillColor);
		}
	}
	
	private static void addPolygonWith(final List<Location> points, final GoogleMap map,
			final int colorStroke, final int colorFill) {
		
		PolygonOptions polygonOptions = new PolygonOptions()
				.strokeWidth(3)
				.strokeColor(colorStroke)
				.fillColor(colorFill);
		
		LatLng point;
		for (Location l : points) {
			point = new LatLng(l.getLatitude(), l.getLongitude());
			polygonOptions.add(point);
		}
		
		map.addPolygon(polygonOptions);
	}
	
	public static int getSolidColorOffAgencyTheme(final Agency agency, final String colorPropertyName) {
		int color;
		
		try {
			JSONObject theme = new JSONObject(agency.themeJsonString);
			String secondary = theme
					.getString(colorPropertyName)
					.replace("0x", "#");
			color = Color.parseColor(secondary);
		} catch (Exception e) {
			color = Color.parseColor("#00529b");
		}
		
		return color;
	}
	
	public static void displayUserPositionWithAccuracy(Context context, GoogleMap map,
			final double positionLatitude, final double positionLongitude, final float accuracy) {
		
		final LatLng position = new LatLng(positionLatitude, positionLongitude);

		//check if current reference of map is different
		if (!map.equals(mMap)) {
			mMap = null;
		}
		
		//accuracy
		if (mUserAccuracy == null || mMap == null) {
			
			int accuracyBaseColor = context.getResources().getColor(R.color.ts_brand_light);
			int accuracyColorFill = Color.argb(50,
					Color.red(accuracyBaseColor), 
					Color.green(accuracyBaseColor), 
					Color.blue(accuracyBaseColor));
			
			CircleOptions accuracyOptions = new CircleOptions()
					.center(position)
					.radius(accuracy)
					.strokeWidth(4)
					.strokeColor(accuracyBaseColor)
					.fillColor(accuracyColorFill);
			
			mUserAccuracy = map.addCircle(accuracyOptions);
		} else {
			mUserAccuracy.setCenter(position);
			mUserAccuracy.setRadius(accuracy);
		}
		
		//user icon at user position
		if (mUserFlatMarker == null || mMap == null) {
			Bitmap bitmap = UserProfile.hasPicture(context)
					? UserProfile.getPicture(context) : BitmapFactory.decodeResource(
							context.getResources(), R.drawable.ts_avatar_default);
			Bitmap resizedBitmap = BitmapUtils.resizeBitmap(bitmap, 100, true);
			Bitmap clippedBitmap = BitmapUtils.clipCircle(resizedBitmap, BitmapUtils.CLIP_RADIUS_DEFAULT);
			MarkerOptions markerOptions = new MarkerOptions()
					.anchor(0.5f, 0.5f)
					.position(position)
					.flat(true)
					.draggable(false)
					.icon(BitmapDescriptorFactory.fromBitmap(clippedBitmap));
			
			mUserFlatMarker = map.addMarker(markerOptions);
		} else {
			mUserFlatMarker.setPosition(position);
		}
		
		if (mMap == null) {
			mMap = map;
		}
	}
}
