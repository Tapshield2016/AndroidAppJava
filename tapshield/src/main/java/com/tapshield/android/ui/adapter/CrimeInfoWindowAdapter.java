package com.tapshield.android.ui.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
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
			String[] data = snippet.split(SEPARATOR);//0:viewed-flag 1:date, 2:source [, 3:address]
			
			infoWindow = LayoutInflater.from(mContext).inflate(R.layout.infowindow_crime, null);
			ImageView viewed = (ImageView) infoWindow.findViewById(R.id.infowindow_crime_image_viewed);
			TextView title = (TextView) infoWindow.findViewById(R.id.infowindow_crime_text_title);
			TextView date = (TextView) infoWindow.findViewById(R.id.infowindow_crime_text_date);
			TextView address = (TextView) infoWindow.findViewById(R.id.infowindow_crime_text_address);
			TextView source = (TextView) infoWindow.findViewById(R.id.infowindow_crime_text_source);
			
			title.setText(marker.getTitle());
			
			if (Boolean.parseBoolean(data[0])) {
				viewed.setVisibility(View.VISIBLE);
			}
			
			date.setText(data[1]);
			
			boolean sourcePresent = data != null && data.length >= 3 && data[2] != null && !data[2].isEmpty();
			boolean addressPresent = data != null && data.length >= 4 && data[3] != null && !data[3].isEmpty();
			
			//if source is spotcrime, ignore it, since it's suposedly unnecessary
			if (sourcePresent
					&& !data[2].equals(mContext.getString(R.string.ts_misc_credits_spotcrime))) {
				source.setText(data[2]);
				source.setVisibility(View.VISIBLE);
			}
			
			if (addressPresent) {
				address.setText(data[3]);
				address.setVisibility(View.VISIBLE);
			}
		}
		return infoWindow;
	}
}
