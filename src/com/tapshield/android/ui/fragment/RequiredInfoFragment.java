package com.tapshield.android.ui.fragment;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinUserManager.OnUserSignUpListener;
import com.tapshield.android.api.model.Agency;
import com.tapshield.android.api.model.User;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.utils.StringUtils;
import com.tapshield.android.utils.UiUtils;

public class RequiredInfoFragment extends BaseFragment implements OnUserSignUpListener {

	public static final String EXTRA_USER = "com.tapshield.android.extra.requiredinfofragment.user";
	
	private JavelinClient mJavelin;
	private EditText mPasscode;
	private EditText mEmail;
	private EditText mPassword;
	private EditText mPhone;
	private Agency mSelectedOrganization;
	
	private boolean mRequestPhone = false;
	private ProgressDialog mDialogSignUp;
	
	private User mUser;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		
		mJavelin = JavelinClient.getInstance(getActivity(), TapShieldApplication.JAVELIN_CONFIG);
		
		mDialogSignUp = new ProgressDialog(getActivity());
		mDialogSignUp.setTitle("signing up");
		mDialogSignUp.setMessage("please wait...");
		mDialogSignUp.setIndeterminate(true);
		mDialogSignUp.setCancelable(false);
	
		Bundle args = getArguments();
		if (args != null) {
			String serOrg = getArguments().getString(OrganizationSelectionFragment.EXTRA_AGENCY, null);
			if (serOrg != null) {
				mSelectedOrganization = Agency.deserializeFromString(serOrg);
				mRequestPhone = mSelectedOrganization != null;
			}
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_requiredinfo, container, false);
		
		mEmail = (EditText) view.findViewById(R.id.fragment_requiredinfo_edit_email);
		mPassword = (EditText) view.findViewById(R.id.fragment_requiredinfo_edit_password);
		mPasscode = (EditText) view.findViewById(R.id.fragment_requiredinfo_edit_passcode);
		mPhone = (EditText) view.findViewById(R.id.fragment_requiredinfo_edit_phone);
		
		if (mRequestPhone) {
			mPhone.setVisibility(View.VISIBLE);
		}
		
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.next, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		switch (item.getItemId()) {
		case R.id.action_next:
			registerUser();
			return true;
		}
		
		return false;
	}
	
	private void registerUser() {
		
		String passcode = getTextOffEditText(mPasscode);
		String email = getTextOffEditText(mEmail);
		String password = getTextOffEditText(mPassword);
		String phone = mRequestPhone ? getTextOffEditText(mPhone) : null;
		
		if (!StringUtils.isEmailValid(email)) {
			mEmail.setError("email is not valid");
		} else if (password.length() < 4) {
			mPassword.setError("has to have at least 4 characters");
		} else if (!StringUtils.isFourDigitsNoSpaceValid(passcode)) {
			mPasscode.setError("has to have 4 digits");
		} else if (mRequestPhone && !StringUtils.isPhoneNumberValid(phone)) {
			mPhone.setError("has to have 10 digits");
		} else {
			//meaning no error was found, attempt to sign up
			mDialogSignUp.show();
			
			//set user to be a reference to be passed forward after a successful signup
			mUser = new User();
			mUser.url = "dummy-url";
			mUser.agency = mSelectedOrganization;
			mUser.email = email;
			mUser.username = mUser.email;
			mUser.setPassword(password);
			mUser.phoneNumber = phone;
			
			mJavelin.getUserManager().signUp(mSelectedOrganization, email, password, phone, passcode, null, null, this);
		}
	}
	
	private void showErrorDialog(String message) {
		AlertDialog errorDialog = new AlertDialog.Builder(getActivity())
				.setTitle("error")
				.setMessage(message)
				.setPositiveButton("ok", null)
				.create();
		errorDialog.show();
	}
	
	private String getTextOffEditText(EditText editText) {
		return editText.getText().toString().trim();
	}
	
	@Override
	public void onUserSignUp(boolean successful, Throwable e) {
		mDialogSignUp.dismiss();
		
		if (successful) {
			UiUtils.toastShort(getActivity(), "registered");
			
			try {
				Bundle extras = new Bundle();
				extras.putString(EXTRA_USER, User.serialize(mUser));
				userRequestProceed(extras);
			} catch (Exception je) {
				onUserSignUp(false, je);
			}
		} else {
			showErrorDialog(e.getMessage());
		}
	}
}
