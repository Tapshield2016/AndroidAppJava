package com.tapshield.android.ui.dialog;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;

import com.tapshield.android.R;

public class TalkOptionsDialog extends DialogFragment implements OnClickListener {

	public static final int OPTION_ORG = 0;
	public static final int OPTION_911 = 1;
	public static final int OPTION_CHAT = 2;
	
	private Button mOrg;
	private Button m911;
	private Button mChat;
	private List<TalkOptionsListener> mListeners = new ArrayList<TalkOptionsListener>();
	
	public TalkOptionsDialog() {
		setStyle(STYLE_NORMAL, android.R.style.Theme_DeviceDefault_Dialog);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		getDialog().setTitle("Talk Options");
		getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		getDialog().getWindow().setBackgroundDrawable(null);
		return inflater.inflate(R.layout.dialog_talk_options, container);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		mOrg = (Button) view.findViewById(R.id.dialog_talk_options_org);
		m911 = (Button) view.findViewById(R.id.dialog_talk_options_911);
		mChat = (Button) view.findViewById(R.id.dialog_talk_options_chat);
		
		mOrg.setOnClickListener(this);
		m911.setOnClickListener(this);
		mChat.setOnClickListener(this);
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
	}
	
	public void show(Activity activity) {
		show(activity.getFragmentManager(), TalkOptionsDialog.class.getSimpleName());
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
