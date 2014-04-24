package com.tapshield.android.ui.fragment;

import android.app.ListFragment;
import android.os.Bundle;

public class BaseDestinationPickFragment extends ListFragment {

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getListView().setDivider(null);
	}
	
	protected final void destinationPicked(final String destination) {
		((DestinationPickListener) getActivity()).onDestinationPick(destination);
	}
	
	public interface DestinationPickListener {
		void onDestinationPick(String destination);
	}
}

