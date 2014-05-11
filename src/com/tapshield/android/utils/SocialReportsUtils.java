package com.tapshield.android.utils;

import com.tapshield.android.R;
import com.tapshield.android.api.JavelinSocialReportingManager;


public class SocialReportsUtils {

	//check javelinsocialreportingmanager for excluded types
	public static final int[] SOCIAL_REPORTS_DRAWABLES_ID = {
		R.drawable.ic_actionbar_done,
		R.drawable.ic_actionbar_done,
		R.drawable.ic_actionbar_done,
		R.drawable.ic_actionbar_done,
		R.drawable.ic_actionbar_done,
		R.drawable.ic_actionbar_done,
		R.drawable.ic_actionbar_done,
		R.drawable.ic_actionbar_done,
		R.drawable.ic_actionbar_done,
		R.drawable.ic_actionbar_done,
		R.drawable.ic_actionbar_done
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
