package com.tapshield.android.utils;


public class StringUtils {

	public static final String REGEX_WHITESPACES = "\\s+";
	
	private static final String REGEX_NAME = ".+";
	private static final String REGEX_FOUR_DIGITS_NO_SPACES = "\\d{4}";
	private static final String REGEX_EMAIL = "[a-zA-Z0-9_.-]+@[a-zA-Z0-9_.-]+\\.[a-z]{2,3}";
	private static final String REGEX_NO_DIGITS = "\\D";
	
	public static boolean isNameValid(final String name) {
		return name.matches(REGEX_NAME);
	}
	
	public static boolean isEmailValid(final String email) {
		return email.matches(REGEX_EMAIL);
	}
	
	public static boolean isFourDigitsNoSpaceValid(final String fourDigitsNoSpaces) {
		return fourDigitsNoSpaces.matches(REGEX_FOUR_DIGITS_NO_SPACES);
	}
	
	public static boolean isPhoneNumberValid(final String tenDigitNumber) {
		return tenDigitNumber.replaceAll(REGEX_NO_DIGITS, new String()).length() == 10;
	}
	
	public static final int getIndexOf(String lookingFor, String[] strings) {
		lookingFor = lookingFor.trim().toLowerCase();
		int index = -1;
		
		for (int i = 0; i < strings.length; i++) {
			String current = strings[i].toLowerCase().trim();
			if (current.equals(lookingFor)) {
				index = i;
				break;
			}
		}
		return index;
	}
}
