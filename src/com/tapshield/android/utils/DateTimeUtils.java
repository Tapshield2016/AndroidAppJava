package com.tapshield.android.utils;

import org.joda.time.DateTime;

public class DateTimeUtils {

	public static String getTimeLabelFor(DateTime forDateTime) {
		String label = "Just now";
		
		DateTime now = new DateTime();
		
		long diff = now.getMillis() - forDateTime.getMillis();
		
		int seconds = (int) Math.ceil(diff / 1000);
		int minutes = (int) Math.ceil(seconds / 60);
		int hours = (int) Math.ceil(minutes / 60);
		
		if (seconds >= 2) {
			label = seconds + " seconds ago";
		}
		
		if (minutes == 1) {
			label = minutes + " minute ago";
		}
		
		if (minutes >= 2) {
			label = minutes + " minutes ago";
		}
		
		if (hours == 1) {
			label = hours + " hour ago";
		}
		
		if (hours >= 2) {
			label = hours + " hours ago";
		}
		
		if (hours > 6) {
			int forDay = forDateTime.getDayOfMonth();
			int nowDay = now.getDayOfMonth();
			
			if (nowDay - forDay < 2) {
				String dayLabel = nowDay == forDay ? "Today" : "Yesterday";
				label = dayLabel + " " + forDateTime.toString("hh:mm aa"); 
			} else {
				label = forDateTime.toString(SpotCrimeUtils.FORMAT_CRIME_DATE);
			}
		}
		
		return label;
	}
}
