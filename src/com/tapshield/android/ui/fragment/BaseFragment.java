package com.tapshield.android.ui.fragment;

import android.app.Fragment;
import android.os.Bundle;

public class BaseFragment extends Fragment {

	private String mTitle;
	
	protected void setTitle(String title) {
		mTitle = title;
	}
	
	public String getTitle() {
		return mTitle;
	}
	
	protected final void userRequestProceed() {
		userRequestProceed(null);
	}
	
	protected final void userRequestProceed(Bundle extras) {
		getListeningActivity().onProceed(extras);
	}
	
	protected final void userRequestReturn() {
		getListeningActivity().onReturn();
	}
	
	private OnUserActionRequestedListener getListeningActivity() {
		return (OnUserActionRequestedListener) getActivity();
	}
	
	public interface OnUserActionRequestedListener {
		void onProceed(Bundle extras);
		void onReturn();
	}
}
