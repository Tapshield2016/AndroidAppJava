package com.tapshield.android.ui.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tapshield.android.R;

public class RouteFragment extends Fragment {

	public static final String EXTRA_ETA = "com.tapshield.android.intent.extra.entourage_route_eta";
	public static final String EXTRA_SUMMARY = "com.tapshield.android.intent.extra.entourage_route_summary";
	
	private TextView mETA;
	private TextView mSummary;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_pickroute_route, container, false);
		mETA = (TextView) root.findViewById(R.id.fragment_pickroute_route_text_eta);
		mSummary = (TextView) root.findViewById(R.id.fragment_pickroute_route_text_summary);
		return root;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		Bundle extras = getArguments();
		
		if (extras == null || !extras.containsKey(EXTRA_ETA) || !extras.containsKey(EXTRA_SUMMARY)) {
			return;
		}
		
		mETA.setText(extras.getString(EXTRA_ETA));
		mSummary.setText(extras.getString(EXTRA_SUMMARY));
	}
}
