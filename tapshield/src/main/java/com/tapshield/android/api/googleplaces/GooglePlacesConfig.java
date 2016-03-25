package com.tapshield.android.api.googleplaces;

public class GooglePlacesConfig {

	private String mUrl = "https://maps.googleapis.com/maps/api/place/";
	private String mKey;

	public String url() {
		return mUrl;
	}
	
	public String key() {
		return mKey;
	}
	
	public static class Builder {
		
		private String key;
		
		public Builder key(String key) {
			this.key = key;
			return this;
		}
		
		public GooglePlacesConfig build() {
			GooglePlacesConfig config = new GooglePlacesConfig();
			config.mKey = key;
			return config;
		}
	}
}
