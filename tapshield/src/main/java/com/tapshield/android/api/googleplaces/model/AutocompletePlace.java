package com.tapshield.android.api.googleplaces.model;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class AutocompletePlace {

	@SerializedName("place_id")
	private String mId;
	
	@SerializedName("description")
	private String mDescription;
	
	@SerializedName("matched_substrings")
	private List<MatchingSubstring> mMatchingSubstrings;
	
	public String placeId() {
		return mId;
	}
	
	public String description() {
		return mDescription;
	}
	
	public boolean hasMatchingSubstrings() {
		return mMatchingSubstrings != null && !mMatchingSubstrings.isEmpty();
	}
	
	public List<MatchingSubstring> getMatchingSubstrings() {
		return mMatchingSubstrings;
	}
	
	public class MatchingSubstring {
		@SerializedName("length")
		private int mLength;
		
		@SerializedName("offset")
		private int mOffset;
		
		public int length() {
			return mLength;
		}
		
		public int offset() {
			return mOffset;
		}
	}
	
	
	
	
	
	/*
    "description" : "Paris, France",
    "id" : "691b237b0322f28988f3ce03e321ff72a12167fd",
    "matched_substrings" : [
       {
          "length" : 5,
          "offset" : 0
       }
    ],
    "place_id" : "ChIJD7fiBh9u5kcRYJSMaMOCCwQ",
    "reference" : "CjQlAAAA_KB6EEceSTfkteSSF6U0pvumHCoLUboRcDlAH05N1pZJLmOQbYmboEi0SwXBSoI2EhAhj249tFDCVh4R-PXZkPK8GhTBmp_6_lWljaf1joVs1SH2ttB_tw",
    "terms" : [
       {
          "offset" : 0,
          "value" : "Paris"
       },
       {
          "offset" : 7,
          "value" : "France"
       }
	*/
}
