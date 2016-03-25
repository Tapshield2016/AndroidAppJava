package com.tapshield.android.api.googledirections;

public class GoogleDirectionsConfig {

	private String mKey;

	public String key() {
		return mKey;
	}
	
	public static class Builder {
		
		private String key;
		
		public Builder key(String key) {
			this.key = key;
			return this;
		}
		
		public GoogleDirectionsConfig build() {
			GoogleDirectionsConfig config = new GoogleDirectionsConfig();
			config.mKey = key;
			return config;
		}
	}
}
