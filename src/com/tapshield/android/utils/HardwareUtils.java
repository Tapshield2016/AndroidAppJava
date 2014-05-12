package com.tapshield.android.utils;

import android.content.Context;
import android.media.AudioManager;
import android.os.Vibrator;
import android.util.Log;

public class HardwareUtils {

	private static final long[] VIBRATOR_PATTERN_DEFAULT = new long[]{300, 700};
	
	private static Vibrator mVibrator;
	
	public static void vibrate(Context context, long milliseconds) {
		getVibrator(context);
		mVibrator.vibrate(milliseconds);
	}
	
	public static void vibrate(Context context, long[] pattern, int repeatIndex) {
		getVibrator(context);

		//set default pattern if no pattern is provided
		if (pattern == null) {
			pattern = VIBRATOR_PATTERN_DEFAULT;
		}
		
		//set to vibrate just once if repeat index is invalid
		int lastIndex = pattern.length - 1;
		if (repeatIndex < -1 || repeatIndex > lastIndex) {
			repeatIndex = -1;
		}
		
		mVibrator.vibrate(pattern, repeatIndex);
	}
	
	public static void vibrateStop(Context context) {
		getVibrator(context);
		
		try {
			mVibrator.cancel();
		} catch (Exception e) {
			Log.e("tapshield", "vibrateStop()", e);
		}
	}
	
	private static void getVibrator(Context context) {
		if (mVibrator == null) {
			mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
		}
	}
	
	public static boolean toggleSpeakerphone(Context context) {
		AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		boolean on = !manager.isSpeakerphoneOn();
		manager.setSpeakerphoneOn(on);
		return on;
	}
	
	public static boolean toggleSpeakerphone(Context context, boolean on) {
		AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		
		if (on != manager.isSpeakerphoneOn()) {
			manager.setSpeakerphoneOn(on);
		}
		
		return on;
	}
	
	public static final boolean isSpeakerphoneOn(Context context) {
		AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		return manager.isSpeakerphoneOn();
	}
}
