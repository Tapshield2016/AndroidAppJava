package com.tapshield.android.api.spotcrime;

public class SpotCrimeConfig {

	private String mBaseUrl = "https://api.spotcrime.com/crimes";
	private String mUrl = mBaseUrl + ".json";
	private String mUrlDetailsPrefix = mBaseUrl + "/";
	private String mUrlDetailsSuffix = ".json";
	private String mKey;
	
	public String getUrl() {
		return mUrl;
	}
	
	public String getDetailsUrl(int crimeId) {
		return mUrlDetailsPrefix + Integer.toString(crimeId) + mUrlDetailsSuffix;
	}
	
	public String getKey() {
		return mKey;
	}
	
	public static class Builder {
		
		private String key;
		
		public Builder key(String key) {
			this.key = key;
			return this;
		}
		
		public SpotCrimeConfig build() {
			SpotCrimeConfig config = new SpotCrimeConfig();
			config.mKey = key;
			return config;
		}
	}
}
