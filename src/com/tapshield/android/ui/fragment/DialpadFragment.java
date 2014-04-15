package com.tapshield.android.ui.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.manager.EmergencyManager;
import com.tapshield.android.manager.TwilioManager;
import com.tapshield.android.ui.activity.MainActivity;
import com.tapshield.android.ui.view.AnimatedVerticalColorProgress;
import com.tapshield.android.ui.view.Dialpad;
import com.tapshield.android.ui.view.Dialpad.DialpadListener;
import com.tapshield.android.utils.UiUtils;

public class DialpadFragment extends Fragment implements DialpadListener {

	private JavelinClient mJavelin;
	private EmergencyManager mEmergencyManager;
	
	private AnimatedVerticalColorProgress mProgress;
	private Dialpad mDialpad;
	
	private int[] mProgressColors;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_dialpad, container, false);
		mProgress = (AnimatedVerticalColorProgress) root.findViewById(R.id.fragment_dialpad_progress);
		mDialpad = (Dialpad) root.findViewById(R.id.fragment_dialpad_dialpad);
		return root;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		mJavelin = JavelinClient.getInstance(getActivity(), TapShieldApplication.JAVELIN_CONFIG);
		mEmergencyManager = EmergencyManager.getInstance(getActivity());
		
		mProgressColors = getResources().getIntArray(R.array.ts_background_alert_countdown);
		mProgress.setListener((AnimatedVerticalColorProgress.Listener) getActivity());
		mDialpad.setDialpadListener(this);
	}
	
	@Override
	public void onResume() {
		super.onResume();

		if (!mEmergencyManager.isRunning()) {
			return;
		}

		long duration = mEmergencyManager.getDuration();
		long startAt = mEmergencyManager.getElapsed();
		mProgress.start(duration, startAt, mProgressColors);
		
		if (mEmergencyManager.isTransmitting()) {
			mProgress.end();
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		mProgress.cancel();
	}

	@Override
	public void onInputComplete(String input) {
		//By default the Dialpad view has a limit of 4 entries
		if (input.equals(mJavelin.getUserManager().getUser().getDisarmCode())) {
			TwilioManager.getInstance(getActivity()).notifyEnd();
			mEmergencyManager.cancel();
			UiUtils.startActivityNoStack(getActivity(), MainActivity.class);
		} else {
			mDialpad.setError(R.string.ts_dialpad_message_error);
		}
	}
	
	public final void notifyAlertStarted() {
		mProgress.end();
	}

	@Override
	public void onInputChange(String newInput) {}
}
