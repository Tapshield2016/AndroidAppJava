package com.tapshield.android.utils;

import java.util.Locale;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import android.content.Context;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.tapshield.android.R;
import com.tapshield.android.api.spotcrime.SpotCrimeClient;
import com.tapshield.android.api.spotcrime.model.Crime;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.ui.adapter.CrimeInfoWindowAdapter;

public class SpotCrimeUtils {

	public static final String FORMAT_CRIME_DATE = "MM/dd/yy hh:mm aa";

	public static int getDrawableOfType(String type, final boolean isMarker) {
		int resource = isMarker ? R.drawable.ts_pin_other_red : R.drawable.ts_report_other_red;
		
		type = type.toLowerCase(Locale.getDefault()).trim();
		
		
		//compare to rest of types ('other' is the default)
		if (type.equals(SpotCrimeClient.TYPE_ARREST)) {
			resource = isMarker ? R.drawable.ts_pin_arrest_red : R.drawable.ts_report_arrest_red;
		} else if (type.equals(SpotCrimeClient.TYPE_ARSON)) {
			resource = isMarker ? R.drawable.ts_pin_arson_red : R.drawable.ts_report_arson_red;
		} else if (type.equals(SpotCrimeClient.TYPE_ASSAULT)) {
			resource = isMarker ? R.drawable.ts_pin_assault_red : R.drawable.ts_report_assault_red;
		} else if (type.equals(SpotCrimeClient.TYPE_BURGLARY)) {
			resource = isMarker ? R.drawable.ts_pin_burglary_red : R.drawable.ts_report_burglary_red;
		} else if (type.equals(SpotCrimeClient.TYPE_ROBBERY)) {
			resource = isMarker ? R.drawable.ts_pin_robbery_red : R.drawable.ts_report_robbery_red;
		} else if (type.equals(SpotCrimeClient.TYPE_SHOOTING)) {
			resource = isMarker ? R.drawable.ts_pin_shooting_red : R.drawable.ts_report_shooting_red;
		} else if (type.equals(SpotCrimeClient.TYPE_THEFT)) {
			resource = isMarker ? R.drawable.ts_pin_theft_red : R.drawable.ts_report_theft_red;
		} else if (type.equals(SpotCrimeClient.TYPE_VANDALISM)) {
			resource = isMarker ? R.drawable.ts_pin_vandalism_red : R.drawable.ts_report_vandalism_red;
		}
		
		return resource;
	}
	
	public static DateTime getDateTimeFromCrime(Crime crime) {
		return DateTimeFormat.forPattern(FORMAT_CRIME_DATE).parseDateTime(crime.getDate());
	}
	
	public final static MarkerOptions getMarkerOptionsOf(Context context, Crime crime,
			boolean attachPosition) {
		
		final DateTime crimeDateTime = SpotCrimeUtils.getDateTimeFromCrime(crime);
		final String type = crime.getType();
		final int markerDrawableResource = SpotCrimeUtils.getDrawableOfType(type, true);
		final String timeLabel = DateTimeUtils.getTimeLabelFor(crimeDateTime);

		//set snippet with mandatory time label and source (optional address if not null)
		final String source = context.getString(R.string.ts_misc_credits_spotcrime);
		final String address = crime.getAddress() != null ? crime.getAddress() : new String();
		final String snippet = Boolean.toString(false)
				+ CrimeInfoWindowAdapter.SEPARATOR + timeLabel
				+ CrimeInfoWindowAdapter.SEPARATOR + source
				+ CrimeInfoWindowAdapter.SEPARATOR + address;

		final float alpha = MapUtils.getOpacityOffTimeframeAt(
				crimeDateTime.getMillis(),
				new DateTime()
						.minusHours(TapShieldApplication.CRIMES_PERIOD_HOURS)
						.getMillis(),
				TapShieldApplication.CRIMES_MARKER_OPACITY_MINIMUM);
		
		MarkerOptions options = new MarkerOptions()
				.draggable(false)
				.icon(BitmapDescriptorFactory.fromResource(markerDrawableResource))
				.anchor(0.5f, 1.0f)
				.alpha(alpha)
				.title(type)
				.snippet(snippet);
		
		if (attachPosition) {
			options.position(new LatLng(crime.getLatitude(), crime.getLongitude()));
		}
		
		return options;
	}
}
