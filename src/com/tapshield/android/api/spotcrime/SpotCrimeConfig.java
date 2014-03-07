package com.tapshield.android.api.spotcrime;

public class SpotCrimeConfig {

	private String mUrl = "https://api.spotcrime.com/crimes.json";
	private String mKey;
	
	public String getUrl() {
		return mUrl;
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
