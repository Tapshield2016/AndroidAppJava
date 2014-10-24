package com.tapshield.android.utils;

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
}
