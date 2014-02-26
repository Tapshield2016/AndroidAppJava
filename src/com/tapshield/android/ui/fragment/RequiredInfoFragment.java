package com.tapshield.android.ui.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;

import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinUserManager.OnUserSignUpListener;
import com.tapshield.android.api.model.Agency;
import com.tapshield.android.api.model.User;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.ui.activity.OrganizationSelectionActivity;
import com.tapshield.android.utils.StringUtils;
import com.tapshield.android.utils.UiUtils;

public class RequiredInfoFragment extends BaseFragment implements OnClickListener, OnUserSignUpListener {

	public static final String EXTRA_USER = "com.tapshield.android.extra.requiredinfofragment.user";
	
	private static final int REQUEST_CODE_SET_PICTURE = 1;
	private static final int REQUEST_CODE_SET_AGENCY = 2;
	
	private JavelinClient mJavelin;
	private ImageButton mPicture;
	private EditText mFirstName;
	private EditText mLastName;
	private EditText mDisarmCode;
	private EditText mEmail;
	private EditText mPassword;
	private EditText mPhone;
	private CheckBox mTermsConditions;
	private Button mOrganization;
	private Button mRegister;
	private Agency mSelectedOrganization;
	
	private ProgressDialog mDialogSignUp;
	
	private User mUser;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mJavelin = JavelinClient.getInstance(getActivity(), TapShieldApplication.JAVELIN_CONFIG);
		
		mDialogSignUp = new ProgressDialog(getActivity());
		mDialogSignUp.setTitle("signing up");
		mDialogSignUp.setMessage("please wait...");
		mDialogSignUp.setIndeterminate(true);
		mDialogSignUp.setCancelable(false);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_requiredinfo, container, false);
		
		mPicture = (ImageButton) view.findViewById(R.id.fragment_requiredinfo_imagebutton_picture);
		mFirstName = (EditText) view.findViewById(R.id.fragment_requiredinfo_edit_firstname);
		mLastName = (EditText) view.findViewById(R.id.fragment_requiredinfo_edit_lastname);
		mDisarmCode = (EditText) view.findViewById(R.id.fragment_requiredinfo_edit_disarmcode);
		mEmail = (EditText) view.findViewById(R.id.fragment_requiredinfo_edit_email);
		mPassword = (EditText) view.findViewById(R.id.fragment_requiredinfo_edit_password);
		mPhone = (EditText) view.findViewById(R.id.fragment_requiredinfo_edit_phone);
		mTermsConditions = (CheckBox)
				view.findViewById(R.id.fragment_requiredinfo_checkbox_termsconditions);
		mOrganization = (Button) view.findViewById(R.id.fragment_requiredinfo_button_organization);
		mRegister = (Button) view.findViewById(R.id.fragment_requiredinfo_button_register);
		
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mPicture.setOnClickListener(this);
		mOrganization.setOnClickListener(this);
		mRegister.setOnClickListener(this);
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.fragment_requiredinfo_imagebutton_picture:
			//prompt user to select or take a picture
			break;
		case R.id.fragment_requiredinfo_button_organization:
			Intent agencySelector = new Intent(getActivity(), OrganizationSelectionActivity.class);
			startActivityForResult(agencySelector, REQUEST_CODE_SET_AGENCY);
			break;
		case R.id.fragment_requiredinfo_button_register:
			registerUser();
			break;
		}
	}

	private void registerUser() {
		
		String firstName = getTextOffEditText(mFirstName);
		String lastName = getTextOffEditText(mLastName);
		String disarmCode = getTextOffEditText(mDisarmCode);
		String email = getTextOffEditText(mEmail);
		String password = getTextOffEditText(mPassword);
		String phone = getTextOffEditText(mPhone);
		
		if (!StringUtils.isNameValid(firstName)) {
			mFirstName.setError("cannot be empty");
		} else if (!StringUtils.isNameValid(lastName)) {
			mLastName.setError("cannot be empty");
		} else if (!StringUtils.isFourDigitsNoSpaceValid(disarmCode)) {
			mDisarmCode.setError("has to have 4 digits");
		} else if (!StringUtils.isEmailValid(email)) {
			mEmail.setError("email is not valid");
		} else if (password.length() < 4) {
			mPassword.setError("has to have at least 4 characters");
		} else if (!StringUtils.isPhoneNumberValid(phone)) {
			mPhone.setError("has to have 10 digits");
		} else if (mSelectedOrganization == null) {
			UiUtils.toastShort(getActivity(), "you need to select an organization");
		} else if (!mTermsConditions.isChecked()) {
			UiUtils.toastShort(getActivity(), "you need to accept terms and conditions");
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
			mUser.setDisarmCode(disarmCode);
			mUser.firstName = firstName;
			mUser.lastName = lastName;
			mUser.phoneNumber = phone;
			
			mJavelin.getUserManager().signUp(mSelectedOrganization, email, password, phone, disarmCode, firstName, lastName, this);
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
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			switch (requestCode) {
			case REQUEST_CODE_SET_PICTURE:
				//set picture
				break;
			case REQUEST_CODE_SET_AGENCY:
				String serializedAgency = data.getStringExtra(OrganizationSelectionActivity.EXTRA);
				mSelectedOrganization = Agency.deserializeFromString(serializedAgency);
				mOrganization.setText(mSelectedOrganization.name);
				break;
			}
		}
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
