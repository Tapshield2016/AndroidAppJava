package com.tapshield.android.utils;

import android.content.Context;

import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinUserManager;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.manager.EmergencyManager;

public class EmergencyManagerUtils {

	/**
	 * Returns true if any real emergency is running, all types are considered real except for chat
	 * that's taking a more passive approach.
	 */
	public static boolean isRealEmergencyActive(EmergencyManager emergencyManager) {
		return emergencyManager.isRunning()
				&& emergencyManager.getType() != EmergencyManager.TYPE_CHAT;
	}
	
	/**
	 * Returns true if an emergency of type chat is running since that would be considered passive.
	 *  All other types are considered real emergencies.
	 */
	public static boolean isPassiveEmergencyActive(EmergencyManager emergencyManager) {
		return emergencyManager.isRunning()
				&& emergencyManager.getType() == EmergencyManager.TYPE_CHAT;
	}
	
	public static String getEmergencyNumber(Context context) {
		//i.e. 911
		String emergNumber = null;
		
		try {
			emergNumber = JavelinClient
					.getInstance(context, TapShieldApplication.JAVELIN_CONFIG)
					.getUserManager()
					.getUser().agency.secondaryNumber;
		} catch (Exception e) {
			emergNumber = null;
		}
		
		//if not present, pull default
		if (emergNumber == null || emergNumber.trim().isEmpty()) {
			emergNumber = context.getString(R.string.ts_no_org_emergency_number);
		}
		
		return emergNumber;
	}
}
