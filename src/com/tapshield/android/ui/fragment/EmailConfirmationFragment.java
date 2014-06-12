package com.tapshield.android.ui.fragment;

import org.json.JSONException;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinClient.OnVerificationEmailRequestListener;
import com.tapshield.android.api.JavelinUserManager;
import com.tapshield.android.api.JavelinUserManager.OnUserLogInListener;
import com.tapshield.android.api.JavelinUserManager.OnUserLogOutListener;
import com.tapshield.android.api.JavelinUserManager.OnUserSignUpListener;
import com.tapshield.android.api.model.User;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.utils.UiUtils;

public class EmailConfirmationFragment extends BaseFragment 
	implements OnClickListener, OnUserLogInListener, OnUserLogOutListener, OnUserSignUpListener,
			OnVerificationEmailRequestListener {

	private JavelinClient mJavelin;
	private JavelinUserManager mUserManager;
	private User mUser;
	
	private EditText mEmail;
	private Button mComplete;
	private Button mResend;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mJavelin = JavelinClient.getInstance(getActivity(), TapShieldApplication.JAVELIN_CONFIG);
		mUserManager = mJavelin.getUserManager();
		
		String serializedUser = getArguments().getString(RequiredInfoFragment.EXTRA_USER);
		
		Log.i("tapshield", "serialized user=" + serializedUser);
		
		String errorRetrievingUser = "error holding user reference from serialized";
		
		if (serializedUser == null) {
			UiUtils.toastShort(getActivity(), errorRetrievingUser);
			userRequestReturn();
			return;
		}
		
		try {
			mUser = User.deserialize(serializedUser);
		} catch (JSONException e) {
			//UiUtils.toastShort(getActivity(), errorRetrievingUser);
			UiUtils.toastShort(getActivity(), e.getMessage());
			userRequestReturn();
			return;
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_emailconfirmation, container, false);
		
		mEmail = (EditText) view.findViewById(R.id.fragment_emailconfirmation_edit_email);
		mComplete = (Button) view.findViewById(R.id.fragment_emailconfirmation_button_complete);
		mResend = (Button) view.findViewById(R.id.fragment_emailconfirmation_button_resend);
		
		return view;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (mUser == null) {
			return;
		}
		
		mComplete.setOnClickListener(this);
		mResend.setOnClickListener(this);
		
		String email = mUser.email;
		mEmail.setText(email);
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.fragment_emailconfirmation_button_complete:
			verifyEmail();
			break;
		case R.id.fragment_emailconfirmation_button_resend:
			resendConfirmationSms();
			break;
		}
	}
	
	private void resendConfirmationSms() {
		mJavelin.resendVerificationEmail(mUser.email, this);
	}
	
	private void verifyEmail() {
		String oldEmail = mUser.email.trim();
		String newEmail = mEmail.getText().toString().trim();
		boolean emailChanged = !oldEmail.equals(newEmail);
		boolean requiredDomain = mUser.agency.requiredDomainEmails;
		boolean wrongDomain = requiredDomain && !newEmail.endsWith(mUser.agency.domain);
		
		//if different email, update local user reference and logout
		//otherwise, attempt to log in with the local reference of the user
		if (wrongDomain) {
			String prefix = getActivity()
					.getString(R.string.ts_fragment_requiredinfo_error_domainrequired_prefix);
			mEmail.setError(prefix + " " + mUser.agency.domain);
		} else if (emailChanged) {
			mUser.email = newEmail;
			mUserManager.logOut(this);
		} else {
			mUserManager.logIn(mUser.username, mUser.getPassword(), this);
		}
	}

	@Override
	public void onUserLogIn(boolean successful, User user, int errorCode, Throwable e) {
		if (successful) {
			UiUtils.toastShort(getActivity(), "Email verified");
			userRequestProceed();
		} else {
			String message = new String();
			switch (errorCode) {
			case JavelinUserManager.CODE_ERROR_UNVERIFIED_EMAIL:
				message = "Unverified email, check your inbox";
				break;
			case JavelinUserManager.CODE_ERROR_WRONG_CREDENTIALS:
				message = "Error with credentials";
				break;
			case JavelinUserManager.CODE_ERROR_OTHER:
				message = "Unexpected error: " + e.getMessage();
				break;
			}
			UiUtils.toastShort(getActivity(), message);
		}
	}
	
	@Override
	public void onUserLogOut(boolean successful, Throwable e) {
		
		//logging out implies the intention of re-signing up with a different email
		if (successful) {
			mUserManager.signUp(mUser.agency, mUser.email, mUser.getPassword(), mUser.phoneNumber,
					mUser.getDisarmCode(), mUser.firstName, mUser.lastName, this);
		}
	}

	@Override
	public void onUserSignUp(boolean successful, Throwable e) {
		if (successful) {
			resendConfirmationSms();
		} else {
			UiUtils.toastShort(getActivity(), e.getMessage());
		}
	}

	@Override
	public void onVerificationEmailRequest(boolean successful, Throwable e) {
		String message = successful ? "Email sent" : "Retry again in a few";
		UiUtils.toastShort(getActivity(), message);
	}
}
