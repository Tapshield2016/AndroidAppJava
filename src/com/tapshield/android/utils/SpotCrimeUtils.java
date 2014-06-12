package com.tapshield.android.utils;

import java.util.Locale;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import com.tapshield.android.R;
import com.tapshield.android.api.spotcrime.SpotCrimeClient;
import com.tapshield.android.api.spotcrime.model.Crime;

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
}
