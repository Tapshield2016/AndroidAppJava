package com.tapshield.android.utils;

import com.tapshield.android.R;
import com.tapshield.android.api.spotcrime.SpotCrimeClient;

public class SpotCrimeUtils {

	public static int getMarkerResourceOfType(String type) {
		int resource = R.drawable.ts_pin_other;
		
		type = type.toLowerCase().trim();
		
		if (type.equals(SpotCrimeClient.TYPE_ARREST)) {
			resource = R.drawable.ts_pin_arrest;
		} else if (type.equals(SpotCrimeClient.TYPE_ARSON)) {
			resource = R.drawable.ts_pin_arson;
		} else if (type.equals(SpotCrimeClient.TYPE_ASSAULT)) {
			resource = R.drawable.ts_pin_assault;
		} else if (type.equals(SpotCrimeClient.TYPE_BURGLARY)) {
			resource = R.drawable.ts_pin_burglary;
		} else if (type.equals(SpotCrimeClient.TYPE_ROBBERY)) {
			resource = R.drawable.ts_pin_robbery;
		} else if (type.equals(SpotCrimeClient.TYPE_SHOOTING)) {
			resource = R.drawable.ts_pin_shooting;
		} else if (type.equals(SpotCrimeClient.TYPE_THEFT)) {
			resource = R.drawable.ts_pin_theft;
		} else if (type.equals(SpotCrimeClient.TYPE_VANDALISM)) {
			resource = R.drawable.ts_pin_vandalism;
		}
		
		return resource;
	}
}
