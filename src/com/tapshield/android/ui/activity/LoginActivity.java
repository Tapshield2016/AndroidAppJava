package com.tapshield.android.ui.activity;

import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinClient.OnRequestPasswordResetListener;
import com.tapshield.android.api.JavelinUserManager;
import com.tapshield.android.api.JavelinUserManager.OnUserLogInListener;
import com.tapshield.android.api.model.User;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.utils.UiUtils;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class LoginActivity extends Activity
		implements OnClickListener, OnRequestPasswordResetListener, OnUserLogInListener {

	JavelinClient mJavelin;
	JavelinUserManager mUserManager;
	
	private EditText mEmail;
	private EditText mPassword;
	
	private Button mTwitter;
	private Button mFacebook;
	private Button mGooglePlus;
	private Button mLogin;
	private Button mForgotPassword;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		
		mJavelin = JavelinClient.getInstance(this, TapShieldApplication.JAVELIN_CONFIG);
		mUserManager = mJavelin.getUserManager();
		
		mEmail = (EditText) findViewById(R.id.login_edit_email);
		mPassword = (EditText) findViewById(R.id.login_edit_password);
		
		mTwitter = (Button) findViewById(R.id.login_button_twitter);
		mFacebook = (Button) findViewById(R.id.login_button_facebook);
		mGooglePlus = (Button) findViewById(R.id.login_button_googleplus);
		mLogin = (Button) findViewById(R.id.login_button_login);
		mForgotPassword = (Button) findViewById(R.id.login_button_forgotpassword);
		
		mTwitter.setOnClickListener(this);
		mFacebook.setOnClickListener(this);
		mGooglePlus.setOnClickListener(this);
		mLogin.setOnClickListener(this);
		mForgotPassword.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.login_button_twitter:
			break;
		case R.id.login_button_facebook:
			break;
		case R.id.login_button_googleplus:
			break;
		case R.id.login_button_login:
			login();
			break;
		case R.id.login_button_forgotpassword:
			forgotPassword();
			break;
		}
	}
	
	private void login() {
		String email = mEmail.getText().toString();
		String password = mPassword.getText().toString();
		
		boolean ok = email != null && !email.isEmpty() && password != null && !password.isEmpty();
		
		if (ok) {
			mUserManager.logIn(email, password, this);
		}
	}
	
	private void forgotPassword() {
		String email = mEmail.getText().toString();
		
		if (email == null || email.isEmpty()) {
			UiUtils.toastShort(this, "please enter email before starting password recovery");
			return;
		}
		
		mJavelin.sendPasswordResetEmail(email, this);
	}

	@Override
	public void onRequestPasswordReset(boolean successful, Throwable e) {
		if (successful) {
			UiUtils.toastShort(this, "pass reset instructions have been sent to your email");
		} else {
			UiUtils.toastLong(this, e.getMessage());
		}
	}

	@Override
	public void onUserLogIn(boolean successful, User user, int errorCode, Throwable e) {
		if (successful) {
			UiUtils.startActivityNoStack(this, MainActivity.class);
		} else {
			mPassword.setText(new String());
			if (errorCode == JavelinUserManager.CODE_ERROR_UNVERIFIED_EMAIL) {
				UiUtils.toastShort(this, "unverified email");
			} else if (errorCode == JavelinUserManager.CODE_ERROR_WRONG_CREDENTIALS) {
				UiUtils.toastShort(this, "wrong login combination");
			}
		}
	}
}
