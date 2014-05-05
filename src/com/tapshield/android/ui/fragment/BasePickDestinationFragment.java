package com.tapshield.android.ui.fragment;

import android.app.ListFragment;
import android.os.Bundle;

public class BasePickDestinationFragment extends ListFragment {

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getListView().setDivider(null);
	}
	
	protected final void destinationPicked(final String destination, String optionalDestinationName) {
		((DestinationPickListener) getActivity()).onDestinationPick(destination, optionalDestinationName);
	}
	
	public interface DestinationPickListener {
		void onDestinationPick(String destination, String optionalDestinationName);
	}
}

