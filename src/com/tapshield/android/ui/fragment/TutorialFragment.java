package com.tapshield.android.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tapshield.android.R;

public class TutorialFragment extends BaseFragment {
	
	public final static String EXTRA_TITLE = "title";
	
	private TextView mText;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_tutorial, container, false);
		mText = (TextView) root.findViewById(R.id.fragment_tutorial_text);
		return root;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		String title = getArguments().getString(EXTRA_TITLE, new String());
		setTitle(title);
		mText.setText(getTitle());
	}
}
