package com.tapshield.android.ui.fragment;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinUserManager;
import com.tapshield.android.api.model.User;
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

	private ProgressDialog mWorkingDialog;
	private AlertDialog mForgotPasscodeDialog;
	private EditText mPassword;
	private int mWrongInputCount = 0;
	
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
		
		mWorkingDialog = getWorkingDialog();
		mForgotPasscodeDialog = getForgotPasscodeDialog();
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
			end();
		} else {
			mDialpad.setError(R.string.ts_dialpad_message_error);
			mWrongInputCount++;
			if (mWrongInputCount >= 4) {
				mForgotPasscodeDialog.show();
			}
		}
	}
	
	@Override
	public void onInputChange(String newInput) {}
	
	public final void notifyAlertStarted() {
		mProgress.end();
	}
	
	private ProgressDialog getWorkingDialog() {
		ProgressDialog dialog = new ProgressDialog(getActivity());
		dialog.setMessage("Working. Please wait...");
		dialog.setCancelable(false);
		return dialog;
	}
	
	private AlertDialog getForgotPasscodeDialog() {
		
		View content = LayoutInflater
				.from(getActivity())
				.inflate(R.layout.dialog_dialpad_forgotpasscode, null);
		
		mPassword = (EditText) content.findViewById(R.id.dialog_dialpad_forgotpasscode_edit);
		
		return new AlertDialog.Builder(getActivity())
				.setTitle("Forgot your passcode?")
				.setView(content)
				.setCancelable(true)
				.setPositiveButton("Disarm", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (mJavelin
								.getUserManager()
								.getUser()
								.equalsPassword(mPassword.getText().toString())) {
							end();
						} else {
							//there is a chance the user reset their password, re-login is required
							mDialpad.postDelayed(new Runnable() {
								
								@Override
								public void run() {
									mForgotPasscodeDialog.show();
									mPassword.setError("Wrong password");
								}
							}, 750);
						}
					}
				})
				.setNeutralButton("Reset Password", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mWorkingDialog.show();
						mJavelin.sendPasswordResetEmail(mJavelin.getUserManager().getUser().email,
								new JavelinClient.OnRequestPasswordResetListener() {
									
									@Override
									public void onRequestPasswordReset(boolean successful, Throwable e) {
										mWorkingDialog.dismiss();

										if (successful) {
											UiUtils.toastShort(getActivity(), "Email sent. Check your inbox!");
											mJavelin.getUserManager().logOut(new JavelinUserManager.OnUserLogOutListener() {
												
												@Override
												public void onUserLogOut(boolean successful, Throwable e) {
													end();
												}
											});
										} else {
											//show again if unsuccessful request to reset password
											mForgotPasscodeDialog.show();
										}
									}
								});
					}
				})
				.create();
	}
	
	private void end() {
		TwilioManager.getInstance(getActivity()).notifyEnd();
		mEmergencyManager.cancel();
		UiUtils.startActivityNoStack(getActivity(), MainActivity.class);
	}
}
