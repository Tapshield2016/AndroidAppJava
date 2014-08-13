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
		
		View infoWindow = null;
		String snippet = null;
		
		if (marker != null && (snippet = marker.getSnippet()) != null) {
			String[] data = snippet.split(SEPARATOR);//0:date, 1:source [, 2:address ]
			
			infoWindow = LayoutInflater.from(mContext).inflate(R.layout.infowindow_crime, null);
			TextView title = (TextView) infoWindow.findViewById(R.id.infowindow_crime_text_title);
			TextView date = (TextView) infoWindow.findViewById(R.id.infowindow_crime_text_date);
			TextView address = (TextView) infoWindow.findViewById(R.id.infowindow_crime_text_address);
			TextView source = (TextView) infoWindow.findViewById(R.id.infowindow_crime_text_source);
			
			title.setText(marker.getTitle());
			date.setText(data[0]);
			
			boolean sourcePresent = data != null && data.length >= 2 && data[1] != null && !data[1].isEmpty();
			boolean addressPresent = data != null && data.length >= 3 && data[2] != null && !data[2].isEmpty();
	
			if (sourcePresent) {
				source.setText(data[1]);
				source.setVisibility(View.VISIBLE);
			}
			
			if (addressPresent) {
				address.setText(data[2]);
				address.setVisibility(View.VISIBLE);
			}
		}
		return infoWindow;
	}
}
