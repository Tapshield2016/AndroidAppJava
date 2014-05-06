package com.tapshield.android.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.model.Marker;
import com.tapshield.android.R;

public class CrimeInfoWindowAdapter implements InfoWindowAdapter {

	public static final String SEPARATOR = "%%";
	
	private Context mContext;
	
	public CrimeInfoWindowAdapter(Context context) {
		mContext = context;
	}
	
	@Override
	public View getInfoContents(Marker marker) {
		return null;
	}

	@Override
	public View getInfoWindow(Marker marker) {
		String snippet = marker.getSnippet();
		String[] data = snippet.split(SEPARATOR);//0:date, 1: address (optional)
		
		View infoWindow = LayoutInflater.from(mContext).inflate(R.layout.infowindow_crime, null);
		
		TextView title = (TextView) infoWindow.findViewById(R.id.infowindow_crime_text_title);
		title.setText(marker.getTitle());
		
		boolean hasAtLeastOne = data != null && data.length >= 1;
		boolean hasAtLeastTwo = data != null && data.length >= 2;
		
		if (hasAtLeastOne) {
			TextView date = (TextView) infoWindow.findViewById(R.id.infowindow_crime_text_date);
			date.setText(data[0]);
			date.setVisibility(View.VISIBLE);
		}
		
		if (hasAtLeastTwo) {
			TextView address = (TextView) infoWindow.findViewById(R.id.infowindow_crime_text_address);
			address.setText(data[1]);
			address.setVisibility(View.VISIBLE);
		}
		
		return infoWindow;
	}
}
