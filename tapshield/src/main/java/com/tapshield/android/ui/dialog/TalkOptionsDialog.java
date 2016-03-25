package com.tapshield.android.ui.dialog;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.DialogFragment;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;

import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinUserManager;
import com.tapshield.android.app.TapShieldApplication;

public class TalkOptionsDialog extends DialogFragment implements OnClickListener {

	public static final int OPTION_ORG = 0;
	public static final int OPTION_911 = 1;
	public static final int OPTION_CHAT = 2;
	
	private Button mOrg;
	private Button m911;
	private Button mChat;
	private List<TalkOptionsListener> mListeners = new ArrayList<TalkOptionsListener>();
	
	private View[] mAnimatingViews;
	
	public TalkOptionsDialog() {
		setStyle(STYLE_NO_FRAME, android.R.style.Theme_DeviceDefault_Light_Dialog);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		setupDialog();
		return inflater.inflate(R.layout.dialog_talk_options, container);
	}
	
	private void setupDialog() {
		getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.argb(0, 0, 0, 0)));
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		mOrg = (Button) view.findViewById(R.id.dialog_talk_options_org);
		m911 = (Button) view.findViewById(R.id.dialog_talk_options_911);
		mChat = (Button) view.findViewById(R.id.dialog_talk_options_chat);
		
		mOrg.setVisibility(View.INVISIBLE);
		m911.setVisibility(View.INVISIBLE);
		mChat.setVisibility(View.INVISIBLE);
		
		mOrg.setOnClickListener(this);
		m911.setOnClickListener(this);
		mChat.setOnClickListener(this);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		JavelinUserManager userManager = JavelinClient
				.getInstance(getActivity(), TapShieldApplication.JAVELIN_CONFIG)
				.getUserManager();
		
		if (userManager.isPresent() && userManager.getUser().belongsToAgency()) {
			
			String alertMode = userManager.getUser().agency.alertModeName;
			
			if (alertMode != null && !alertMode.isEmpty()) {
				mOrg.setText(alertMode);
			}
		}
		
		mAnimatingViews = new View[3];
		mAnimatingViews[0] = mOrg;
		mAnimatingViews[1] = m911;
		mAnimatingViews[2] = mChat;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		animateViews();
	}
	
	@Override
	public void onClick(View v) {
		
		switch (v.getId()) {
		case R.id.dialog_talk_options_org:
			notifyListeners(OPTION_ORG);
			break;
		case R.id.dialog_talk_options_911:
			notifyListeners(OPTION_911);
			break;
		case R.id.dialog_talk_options_chat:
			notifyListeners(OPTION_CHAT);
			break;
		}
		
		dismiss();
	}

	public void show(Activity activity) {
		show(activity.getFragmentManager(), TalkOptionsDialog.class.getSimpleName());
	}
	
	private void animateViews() {
		
		Animation anim = null;
		
		for (int i = 0; i < mAnimatingViews.length; i++) {
			
			anim = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_in_up);
			anim.setFillAfter(true);
			anim.setStartOffset((i * anim.getDuration())/4l);
			
			if (mAnimatingViews[i] != null) {
				mAnimatingViews[i].startAnimation(anim);
			}
		}
	}
	
	public void setOptionEnable(int option, boolean enable) {
		
		Button b = null;
		
		switch (option) {
		case OPTION_911:
			b = m911;
			break;
		case OPTION_ORG:
			b = mOrg;
			break;
		case OPTION_CHAT:
			b = mChat;
			break;
		}
		
		if (b != null) {
			b.setEnabled(enable);
		}
	}
	
	public boolean addListener(TalkOptionsListener l) {
		return mListeners.add(l);
	}
	
	public boolean removeListener(TalkOptionsListener l) {
		return mListeners.remove(l);
	}

	public void notifyListeners(int option) {
		for (TalkOptionsListener l : mListeners) {
			l.onOptionSelect(option);
		}
	}
	
	public interface TalkOptionsListener {
		void onOptionSelect(int option);
	}
}
