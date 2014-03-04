package com.tapshield.android.ui.fragment;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinUserManager;
import com.tapshield.android.api.JavelinUserManager.OnPhoneNumberVerificationSmsCodeVerifiedListener;
import com.tapshield.android.api.JavelinUserManager.OnUserRequiredInformationUpdateListener;
import com.tapshield.android.api.model.User;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.utils.UiUtils;

public class PhoneConfirmationFragment extends BaseFragment
		implements OnPhoneNumberVerificationSmsCodeVerifiedListener, OnClickListener,
				OnUserRequiredInformationUpdateListener {

	private EditText mPhone;
	private EditText mCode;
	private Button mResend;
	
	private ProgressDialog mDialog;
	
	private JavelinUserManager mUserManager;
	
	private TextWatcher mPhoneWatcher;
	private TextWatcher mCodeWatcher;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mUserManager = JavelinClient.getInstance(getActivity(), TapShieldApplication.JAVELIN_CONFIG)
				.getUserManager();
		
		mDialog = new ProgressDialog(getActivity());
		mDialog.setTitle("please wait");
		mDialog.setMessage("verifying code...");
		mDialog.setIndeterminate(true);
		mDialog.setCancelable(false);
		
		mPhoneWatcher = new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (userChangedPhoneNumber()) {
					mCode.setText(new String());
					mCode.setEnabled(false);
				}
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			
			@Override
			public void afterTextChanged(Editable s) {}
		};
		
		mCodeWatcher = new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (s.length() >= 4) {
					checkCurrentCode();
				}
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			
			@Override
			public void afterTextChanged(Editable s) {}
		};
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_phoneconfirmation, container, false);
		mPhone = (EditText) view.findViewById(R.id.fragment_phoneconfirmation_edit_phone);
		mCode = (EditText) view.findViewById(R.id.fragment_phoneconfirmation_edit_code);
		mResend = (Button) view.findViewById(R.id.fragment_phoneconfirmation_button_resend);
		return view;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		String phone = mUserManager.getUser().phoneNumber;
		mPhone.setText(phone);
		mPhone.addTextChangedListener(mPhoneWatcher);
		mCode.addTextChangedListener(mCodeWatcher);
		mResend.setOnClickListener(this);
	}
	
	private void checkCurrentCode() {
		mDialog.show();

		String attemptedCode = mCode.getText().toString();
		mUserManager.verifyPhoneNumberWithCode(attemptedCode, this);
	}
	
	private boolean userChangedPhoneNumber() {
		String latest = mPhone.getText().toString();
		String original = mUserManager.getUser().phoneNumber;
		return !latest.equals(original);
	}

	@Override
	public void onNewPhoneNumberVerificationSmsCodeVerified(boolean success, String reason) {
		mDialog.dismiss();
		if (success) {
			UiUtils.toastShort(getActivity(), "code verified");
			userRequestProceed();
		} else {
			mCode.setText(new String());
			UiUtils.toastLong(getActivity(), "error verifying:" + reason);
		}
	}

	@Override
	public void onClick(View v) {
		//if phone changed, update required information before verifying the code
		if (userChangedPhoneNumber()) {
			String newPhone = mPhone.getText().toString();
			User user = mUserManager.getUser();
			user.phoneNumber = newPhone;
			mUserManager.setUser(user);
			mUserManager.updateRequiredInformation(this);
		} else {
			mUserManager.resendPhoneNumberVerificationSms();
		}
	}

	@Override
	public void onUserRequiredInformationUpdate(boolean successful, Throwable e) {
		if (successful) {
			mUserManager.resendPhoneNumberVerificationSms();
			mCode.setEnabled(true);
			mCode.requestFocus();
		} else {
			UiUtils.toastLong(getActivity(), "error updating changed number:" + e.getMessage());
		}
	}
}
