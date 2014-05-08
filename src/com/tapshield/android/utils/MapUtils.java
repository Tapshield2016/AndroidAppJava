package com.tapshield.android.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.tapshield.android.R;
import com.tapshield.android.api.model.UserProfile;

public class MapUtils {

	private static GoogleMap mMap;
	private static Marker mUserFlatMarker;
	private static Circle mUserAccuracy;
	
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
