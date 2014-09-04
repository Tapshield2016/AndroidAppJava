package com.tapshield.android.ui.activity;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
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

public class VerifyPhoneActivity extends BaseFragmentActivity
		implements OnPhoneNumberVerificationSmsCodeVerifiedListener,
		OnUserRequiredInformationUpdateListener {

	private JavelinUserManager mUserManager;
	
	private Button mResend;
	private EditText mPhone;
	private EditText mCode;
	
	private ProgressDialog mDialog;
	
	@Override
	protected void onCreate(Bundle savedInstance) {
		super.onCreate(savedInstance);
		setContentView(R.layout.activity_verifyphone);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		UiUtils.setStepIndicatorInActionBar(this, 2, 3,
				R.string.ts_registration_actionbar_title_phoneconfirmation);
		
		mUserManager = JavelinClient
				.getInstance(this, TapShieldApplication.JAVELIN_CONFIG)
				.getUserManager();
		
		mResend = (Button) findViewById(R.id.verifyphone_button_resend);
		mPhone = (EditText) findViewById(R.id.verifyphone_edit_phone);
		mCode = (EditText) findViewById(R.id.verifyphone_edit_code);
		
		mResend.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//if phone changed, update required information before verifying the code
				if (userChangedPhoneNumber()) {
					String newPhone = mPhone.getText().toString();
					User user = mUserManager.getUser();
					user.phoneNumber = newPhone;
					mUserManager.setUser(user);
					mUserManager.updateRequiredInformation(VerifyPhoneActivity.this);
				} else {
					mUserManager.resendPhoneNumberVerificationSms();
				}
			}
		});

		mDialog = new ProgressDialog(this);
		mDialog.setTitle(R.string.ts_verifyphone_wait_title);
		mDialog.setMessage(getString(R.string.ts_verifyphone_wait_message));
		mDialog.setIndeterminate(true);
		mDialog.setCancelable(false);
		
		TextWatcher phoneWatcher = new TextWatcher() {
			
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
		
		TextWatcher codeWatcher = new TextWatcher() {
			
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
		
		String phone = mUserManager.getUser().phoneNumber;
		if (phone != null) {
			mPhone.setText(phone);
		}
		mPhone.addTextChangedListener(phoneWatcher);
		mCode.addTextChangedListener(codeWatcher);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			return true;
		}
		return false;
	}
	
	@Override
	public void onBackPressed() {
		mUserManager.clearTemporaryAgency();
		finish();
	}
	
	private boolean userChangedPhoneNumber() {
		String latest = mPhone.getText().toString();
		String original = mUserManager.getUser().phoneNumber;
		return !latest.equals(original);
	}
	
	private void checkCurrentCode() {
		mDialog.show();

		String attemptedCode = mCode.getText().toString();
		mUserManager.verifyPhoneNumberWithCode(attemptedCode, this);
	}

	@Override
	public void onNewPhoneNumberVerificationSmsCodeVerified(boolean success, String reason) {
		mDialog.dismiss();
		if (success) {
			UiUtils.toastShort(this, getString(R.string.ts_verifyphone_ok));
			UiUtils.startActivityNoStack(this, MainActivity.class);
			finish();
		} else {
			mCode.setText(new String());
			UiUtils.toastLong(this, getString(R.string.ts_verifyphone_error_prefix) + reason);
		}
	}

	@Override
	public void onUserRequiredInformationUpdate(boolean successful, Throwable e) {
		if (successful) {
			mUserManager.resendPhoneNumberVerificationSms();
			mCode.setEnabled(true);
			mCode.requestFocus();
		} else {
			UiUtils.toastLong(this, getString(R.string.ts_verifyphone_update_error_prefix) + e.getMessage());
		}
	}
	
	//on button request code click:
	//if phone is not in user, update phone number (usermananger.updaterequiredinformation(null))
	//request code
}
