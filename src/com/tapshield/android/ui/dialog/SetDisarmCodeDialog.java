package com.tapshield.android.ui.dialog;

import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinUserManager;
import com.tapshield.android.api.model.User;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.ui.view.Dialpad;
import com.tapshield.android.ui.view.Dialpad.DialpadListener;
import com.tapshield.android.utils.UiUtils;

public class SetDisarmCodeDialog extends DialogFragment implements DialpadListener {

	private JavelinUserManager mUserManager;
	private Dialpad mDialpad;
	private OnCancelListener mCancelListener;
	
	public SetDisarmCodeDialog() {
		setStyle(STYLE_NORMAL, android.R.style.Theme_DeviceDefault_Dialog);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		getDialog().setTitle("Passcode not set");
		return inflater.inflate(R.layout.dialog_disarmcode, container);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		mDialpad = (Dialpad) view.findViewById(R.id.dialog_disarmcode_dialpad);
		mDialpad.setMessage("This will help us verify your identity when using the app. Set it now:");
		mDialpad.setDialpadListener(this);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mUserManager = JavelinClient
				.getInstance(getActivity(),TapShieldApplication.JAVELIN_CONFIG)
				.getUserManager();
	}
	
	@Override
	public void onCancel(DialogInterface dialog) {
		if (mCancelListener != null) {
			mCancelListener.onCancel(dialog);
		} else {
			super.onCancel(dialog);
		}
	}

	@Override
	public void onInputComplete(String input) {
		User u = mUserManager.getUser();
		u.setDisarmCode(input);
		mUserManager.setUser(u);
		mDialpad.setClickable(false);
		mDialpad.setMessage("Saving passcode...");
		mUserManager.updateRequiredInformation(
				new JavelinUserManager.OnUserRequiredInformationUpdateListener() {
			
			@Override
			public void onUserRequiredInformationUpdate(boolean successful, Throwable e) {
				if (successful) {
					UiUtils.toastShort(getActivity(), "Passcode saved");
					dismiss();
				} else {
					mDialpad.setMessage(e.getMessage());
				}
			}
		});
	}

	@Override
	public void onInputChange(String newInput) {}
	
	public void setOnCancelListener(OnCancelListener l) {
		mCancelListener = l;
	}
}
