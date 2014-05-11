package com.tapshield.android.utils;

import com.tapshield.android.R;
import com.tapshield.android.api.JavelinSocialReportingManager;


public class SocialReportsUtils {

	//check javelinsocialreportingmanager for excluded types
	public static final int[] SOCIAL_REPORTS_DRAWABLES_ID = {
		//R.drawable.ic_launcher,
		R.drawable.ts_report_assault,
		R.drawable.ts_report_brokenbone,
		//R.drawable.ic_launcher,
		R.drawable.ts_report_highfever,
		R.drawable.ts_report_choking,
		R.drawable.ts_report_other,
		//R.drawable.ic_launcher,
		//R.drawable.ic_launcher,
		R.drawable.ts_report_burglary,
		R.drawable.ts_report_theft,
		R.drawable.ts_report_vandalism
	};
	
	public final static int getImageResourceByType(String type) {
		int resource = -1;
		
		final String[] list = JavelinSocialReportingManager.TYPE_LIST;
		final int len = list.length;
		
		for (int i = 0; i < len; i++) {
			if (list[i].toLowerCase().trim().equals(type.toLowerCase().trim())) {
				resource = SOCIAL_REPORTS_DRAWABLES_ID[i];
				break;
			}
		}
		
		return resource;
	}
}
