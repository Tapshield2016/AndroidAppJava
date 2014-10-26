package com.tapshield.android.api.googleplaces.model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.tapshield.android.utils.StringUtils;

//Base class that will provide methods to different search types in order to return 
public abstract class Search {

	protected static final String PARAM_RANKBY = "rankby";
	protected static final String PARAM_LOCATION = "location";
	protected static final String PARAM_RADIUS = "radius";
	
	private static final String GET_PARAM_SEPARATOR = "&";
	private static final String GET_PARAM_EQUAL = "=";

	private String mType = Search.class.getSimpleName();
	private String mResultsKey = "results";
	private Map<String, String> mParams;
	
	protected Search(String type) {
		this(type, null);
	}
	
	protected Search(String type, String resultsKey) {
		if (type == null || type.isEmpty()) {
			throw new RuntimeException("Constructor argument type must not be null/empty.");
		}
		
		if (resultsKey != null) {
			setResultsKey(resultsKey);
		}
		
		setType(type);
		mParams = new HashMap<String, String>();
	}
	
	private final void setType(String type) {
		mType = type;
	}
	
	public String getType() {
		return mType;
	}
	
	private final void setResultsKey(String resultsKey) {
		mResultsKey = resultsKey;
	}
	
	public String getResultsKey() {
		return mResultsKey;
	}
	
	public boolean hasParams() {
		return getParams() != null && !getParams().isEmpty();
	}
	
	protected final void addParam(String name, String value) {
		mParams.put(name, value);
	}
	
	protected final void removeParam(String name) {
		mParams.remove(name);
	}
	
	protected void onPreGetParams(Map<String, String> params) {}
	
	public final String getParams() {
		onPreGetParams(mParams);
		
		StringBuilder builder = new StringBuilder();

		Iterator<String> iterator = mParams.keySet().iterator();
		String value;
		while (iterator.hasNext()) {
			String key = iterator.next();
			value = mParams.get(key).toString();
			builder.append(GET_PARAM_SEPARATOR + key + GET_PARAM_EQUAL + value);
		}
		
		return builder.toString();
	}

	protected static final String replaceWhitespaceWithPlus(String string) {
		return string.trim().replaceAll(StringUtils.REGEX_WHITESPACES, "+");
	}
}
