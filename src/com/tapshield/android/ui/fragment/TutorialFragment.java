package com.tapshield.android.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.tapshield.android.R;

public class TutorialFragment extends BaseFragment {
	
	public final static String EXTRA_IMAGE = "image";
	public final static String EXTRA_TEXT = "text";
	
	private ImageView mImage;
	private TextView mText;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_tutorial, container, false);
		mImage = (ImageView) root.findViewById(R.id.fragment_tutorial_image);
		mText = (TextView) root.findViewById(R.id.fragment_tutorial_text);
		return root;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		Bundle arguments = getArguments();
		
		int imageResource = arguments.getInt(EXTRA_IMAGE, -1);
		int textResource = arguments.getInt(EXTRA_TEXT, -1);
		
		mImage.setImageResource(imageResource);
		mText.setText(textResource);
	}
}
