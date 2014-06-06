package com.tapshield.android.utils;

import java.util.Locale;

import com.tapshield.android.R;
import com.tapshield.android.api.JavelinSocialReportingManager;


public class SocialReportsUtils {

	//check javelinsocialreportingmanager for excluded types
	public static final int[] SOCIAL_REPORTS_DRAWABLES_ID_ICONS = {
		R.drawable.ts_report_abuse_blue,
		R.drawable.ts_report_assault_blue,
		R.drawable.ts_report_caraccident_blue,
		R.drawable.ts_report_disturbance_blue,
		R.drawable.ts_report_drugs1_blue,
		R.drawable.ts_report_harassment_blue,
		R.drawable.ts_report_mentalhealth_blue,
		R.drawable.ts_report_other_blue,
		R.drawable.ts_report_suggestion_blue,
		R.drawable.ts_report_suspicious_blue,
		R.drawable.ts_report_theft_blue,
		R.drawable.ts_report_vandalism_blue
	};
	
	public static final int[] SOCIAL_REPORTS_DRAWABLES_ID_PINS = {
		R.drawable.ts_pin_abuse_blue,
		R.drawable.ts_pin_assault_blue,
		R.drawable.ts_pin_caraccident_blue,
		R.drawable.ts_pin_disturbance_blue,
		R.drawable.ts_pin_drugs1_blue,
		R.drawable.ts_pin_harassment_blue,
		R.drawable.ts_pin_mentalhealth_blue,
		R.drawable.ts_pin_other_blue,
		R.drawable.ts_pin_suggestion_blue,
		R.drawable.ts_pin_suspicious_blue,
		R.drawable.ts_pin_theft_blue,
		R.drawable.ts_pin_vandalism_blue
	};
	
	public final static int getDrawableOfType(String type, final boolean isMarker) {
		int resource = isMarker ? R.drawable.ts_pin_other_blue : R.drawable.ts_report_other_blue;
		
		final String[] list = JavelinSocialReportingManager.TYPE_LIST;
		final int len = list.length;
		
		for (int i = 0; i < len; i++) {
			if (list[i].toLowerCase(Locale.getDefault()).trim().equals(type.toLowerCase(Locale.getDefault()).trim())) {
				resource = isMarker ? SOCIAL_REPORTS_DRAWABLES_ID_PINS[i]
						: SOCIAL_REPORTS_DRAWABLES_ID_ICONS[i];
				break;
			}
		}
		
		return resource;
	}
}
