package com.tapshield.android.ui.fragment;

import android.app.Fragment;

public class BaseFragment extends Fragment {

	private String mTitle;
	
	protected void setTitle(String title) {
		mTitle = title;
	}
	
	public String getTitle() {
		return mTitle;
	}
}
