package com.tapshield.android.ui.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;

public class BaseFragment extends Fragment {

	private String mTitle;
	
	public void setTitle(String title) {
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
