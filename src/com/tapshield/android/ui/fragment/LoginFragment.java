package com.tapshield.android.ui.fragment;

import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinClient.OnRequestPasswordResetListener;
import com.tapshield.android.api.JavelinUserManager;
import com.tapshield.android.api.JavelinUserManager.OnUserLogInListener;
import com.tapshield.android.api.model.User;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.ui.activity.MainActivity;
import com.tapshield.android.utils.UiUtils;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class LoginFragment extends BaseWelcomeFragment
		implements OnClickListener, OnUserLogInListener, OnRequestPasswordResetListener {

	private EditText mEmail;
	private EditText mPassword;
	private Button mLogin;
	private TextView mForgotPassword;
	
	private JavelinClient mJavelin;
	
	public LoginFragment() {
		setTitle("Login");
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_login, container, false);
		
		mEmail = (EditText) view.findViewById(R.id.fragment_login_edit_email);
		mPassword = (EditText) view.findViewById(R.id.fragment_login_edit_password);
		mLogin = (Button) view.findViewById(R.id.fragment_login_button_login);
		mForgotPassword = (TextView) view.findViewById(R.id.fragment_login_text_forgotpassword);
		
		return view;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		mJavelin = JavelinClient.getInstance(getActivity(), TapShieldApplication.JAVELIN_CONFIG);
		
		mLogin.setOnClickListener(this);
		mForgotPassword.setOnClickListener(this);
	}

	@Override
	public void onClick(View view) {
		
		JavelinUserManager userManager = mJavelin.getUserManager();
		
		String email = mEmail.getText().toString();
		String password = mPassword.getText().toString();
		
		if (view.getId() == R.id.fragment_login_button_login) {
			boolean ok = email != null && !email.isEmpty() && password != null && !password.isEmpty();
			
			if (ok) {
				userManager.logIn(email, password, this);
			}
		} else if (view.getId() == R.id.fragment_login_text_forgotpassword) {
			if (email == null || email.isEmpty()) {
				UiUtils.toastShort(getActivity(), "please enter email before starting password recovery");
				return;
			}
			
			mJavelin.sendPasswordResetEmail(email, this);
		}
	}

	@Override
	public void onUserLogIn(boolean successful, User user, int errorCode, Throwable e) {
		if (successful) {
			UiUtils.startActivityNoStack(getActivity(), MainActivity.class);
		} else {
			mPassword.setText(new String());
			if (errorCode == JavelinUserManager.CODE_ERROR_UNVERIFIED_EMAIL) {
				UiUtils.toastShort(getActivity(), "unverified email");
			} else if (errorCode == JavelinUserManager.CODE_ERROR_WRONG_CREDENTIALS) {
				UiUtils.toastShort(getActivity(), "wrong login combination");
			}
		}
	}

	@Override
	public void onRequestPasswordReset(boolean successful, Throwable e) {
		if (successful) {
			UiUtils.toastShort(getActivity(), "pass reset instructions have been sent to your email");
		} else {
			UiUtils.toastLong(getActivity(), e.getMessage());
		}
	}
}
