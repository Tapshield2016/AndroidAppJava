package com.tapshield.android.ui.fragment;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;

import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinUserManager;
import com.tapshield.android.api.JavelinUserManager.OnUserLogInListener;
import com.tapshield.android.api.model.User;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.ui.activity.FacebookLoginActivity;
import com.tapshield.android.ui.activity.GooglePlusLoginActivity;
import com.tapshield.android.ui.activity.RegistrationActivity;
import com.tapshield.android.utils.StringUtils;
import com.tapshield.android.utils.UiUtils;

public class LoginFragment extends BaseFragment implements OnClickListener, OnMenuItemClickListener,
		OnUserLogInListener {

	private JavelinClient mJavelin;
	private JavelinUserManager mUserManager;
	
	private View mForm;
	private View mOptions;
	private Button mLoginOption;
	private Button mSignUpOption;
	private EditText mEmail;
	private EditText mPassword;
	private Button mLogin;
	private TextView mNoAccount;
	private TextView mForgotPassword;
	private TextView mDisclaimer;
	private ProgressDialog mLoggingIn;
	
	private boolean mLoginPressed;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_login, container, false);
		
		mForm = root.findViewById(R.id.fragment_login_linear_form);
		mOptions = root.findViewById(R.id.fragment_login_linear_bottom);
		
		mLoginOption = (Button) root.findViewById(R.id.fragment_login_button_login);
		mSignUpOption = (Button) root.findViewById(R.id.fragment_login_button_signup);
		
		mEmail = (EditText) root.findViewById(R.id.fragment_login_form_edit_email);
		mPassword = (EditText) root.findViewById(R.id.fragment_login_form_edit_password);
		mLogin = (Button) root.findViewById(R.id.fragment_login_form_button_login);
		mNoAccount = (TextView) root.findViewById(R.id.fragment_login_form_text_noaccount);
		mForgotPassword = (TextView) root.findViewById(R.id.fragment_login_form_text_forgotpassword);
		mDisclaimer = (TextView) root.findViewById(R.id.fragment_login_text_disclaimer);
		
		return root;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mJavelin = JavelinClient.getInstance(getActivity(),TapShieldApplication.JAVELIN_CONFIG);
		mUserManager = mJavelin.getUserManager();
		
		mLoggingIn = getLoggingDialog();
		
		mLogin.setOnClickListener(this);
		mLoginOption.setOnClickListener(this);
		mSignUpOption.setOnClickListener(this);
		
		mNoAccount.setOnClickListener(this);
		mForgotPassword.setOnClickListener(this);
		
		mDisclaimer.setText(Html.fromHtml(mDisclaimer.getText().toString()));
		mDisclaimer.setMovementMethod(LinkMovementMethod.getInstance());
	}
	
	private ProgressDialog getLoggingDialog() {
		ProgressDialog d = new ProgressDialog(getActivity());
		d.setTitle(R.string.ts_welcome_fragment_login_dialog_loggingin_title);
		d.setMessage(getActivity().getString(R.string.ts_welcome_fragment_login_dialog_loggingin_message));
		d.setIndeterminate(true);
		d.setCancelable(false);
		return d;
	}
	
	@Override
	public void onClick(View v) {

		switch (v.getId()) {
		case R.id.fragment_login_form_button_login:
			attemptLogin();
			break;
		case R.id.fragment_login_form_text_noaccount:
			mForm.setVisibility(View.INVISIBLE);
			mOptions.setVisibility(View.VISIBLE);
			mSignUpOption.performClick();
			break;
		case R.id.fragment_login_form_text_forgotpassword:
			requestPasswordReset();
			break;
		case R.id.fragment_login_button_login: case R.id.fragment_login_button_signup:
			
			mLoginPressed = v.getId() == R.id.fragment_login_button_login;

			PopupMenu menu = new PopupMenu(getActivity(), v);
			menu.inflate(R.menu.login_signup);
			menu.setOnMenuItemClickListener(this);
			menu.show();
			
			break;
		}
	}

	@Override
	public boolean onMenuItemClick(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_email:
			if (mLoginPressed) {
				mForm.setVisibility(View.VISIBLE);
				mOptions.setVisibility(View.INVISIBLE);
			} else {
				startRegistration();
			}
			break;
		case R.id.menu_facebook:
			Intent facebookSignIn = new Intent(getActivity(), FacebookLoginActivity.class);
			startActivity(facebookSignIn);
			break;
		case R.id.menu_googleplus:
			Intent googlePlusSignIn = new Intent(getActivity(), GooglePlusLoginActivity.class);
			startActivity(googlePlusSignIn);
			break;
		default:
			return false;
		}
		return true;
	}
	
	private void requestPasswordReset() {
		String email = mEmail.getText().toString().trim();
		
		if (email == null || email.isEmpty() || !StringUtils.isEmailValid(email)) {
			UiUtils.toastLong(getActivity(), getActivity().getString(
					R.string.ts_welcome_fragment_login_toast_passwordreset_emailinvalid));
			return;
		}
		
		mJavelin.sendPasswordResetEmail(email, new JavelinClient.OnRequestPasswordResetListener() {
			
			@Override
			public void onRequestPasswordReset(boolean successful, Throwable e) {
				String m = successful ? getActivity().getString(
						R.string.ts_welcome_fragment_login_toast_passwordreset_ok) :
						"Error:" + e.getMessage();
				UiUtils.toastLong(getActivity(), m);
			}
		});
	}
	
	private void startRegistration() {
		Intent registration = new Intent(getActivity(), RegistrationActivity.class);
		startActivity(registration);
	}
	
	private void attemptLogin() {
		String email = mEmail.getText().toString().trim();
		String password = mPassword.getText().toString().trim();
		
		if (email == null || email.isEmpty() || !StringUtils.isEmailValid(email)) {
			UiUtils.toastShort(getActivity(), getActivity().getString(
					R.string.ts_welcome_fragment_login_toast_login_emailinvalid));
			return;
		}
		
		if (password == null || password.isEmpty()) {
			UiUtils.toastShort(getActivity(), getActivity().getString(
					R.string.ts_welcome_fragment_login_toast_login_passwordinvalid));
			return;
		}
		
		mLoggingIn.show();
		mUserManager.logIn(email, password, this);
	}

	@Override
	public void onUserLogIn(boolean successful, User user, int errorCode, Throwable e) {
		mLoggingIn.dismiss();
		if (successful) {
			UiUtils.welcomeUser(getActivity());
			getActivity().finish();
		} else {
			int messageRes = R.string.ts_welcome_fragment_login_toast_login_error_default;
			
			if (errorCode == JavelinUserManager.CODE_ERROR_WRONG_CREDENTIALS) {
				messageRes = R.string.ts_welcome_fragment_login_toast_login_error_badcredentials;
			} else if (errorCode == JavelinUserManager.CODE_ERROR_UNVERIFIED_EMAIL) {
				messageRes = R.string.ts_welcome_fragment_login_toast_login_error_emailunverified;
			}
			
			String message = getActivity().getString(messageRes);
			UiUtils.toastLong(getActivity(), message);
		}
	}
	
	public boolean onBackPressed() {
		if (mForm.getVisibility() == View.VISIBLE) {
			mForm.setVisibility(View.INVISIBLE);
			mOptions.setVisibility(View.VISIBLE);
			return true;
		}
		return false;
	}
}
