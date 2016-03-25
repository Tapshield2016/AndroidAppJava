package com.tapshield.android.ui.activity;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
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
import com.tapshield.android.api.model.User;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.utils.UiUtils;

public class ResetPasscodePasswordActivity extends BaseFragmentActivity {

	private JavelinClient mManager;
	private JavelinUserManager mUserManager;
	private EditText mCurrent;
	private EditText mNewPasscode;
	private EditText mConfirmPasscode;
	private Button mForgotBoth;
	private ProgressDialog mWorking;
	private AlertDialog mResetPasswordDialog;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_resetpasscodepassword);
		
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);

		mCurrent = (EditText) findViewById(R.id.resetpasscodepassword_edit_auth);
		mNewPasscode = (EditText) findViewById(R.id.resetpasscodepassword_edit_newpasscode);
		mConfirmPasscode = (EditText) findViewById(R.id.resetpasscodepassword_edit_newpasscode_confirm);
		mForgotBoth = (Button) findViewById(R.id.resetpasscodepassword_button_forgot);
		
		mManager = JavelinClient.getInstance(this, TapShieldApplication.JAVELIN_CONFIG);
		mUserManager = mManager.getUserManager();
		
		mWorking = getWorkingDialog();
		mResetPasswordDialog = getResetPasswordDialog();
		
		mConfirmPasscode.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (s.length() >= 4) {
					updateIfPossible();
				}
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			
			@Override
			public void afterTextChanged(Editable s) {}
		});
		
		mForgotBoth.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				mResetPasswordDialog.show();
			}
		});
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			return true;
		}
		
		return false;
	}
	
	private void updateIfPossible() {
		if (isFormValid()) {
			mWorking.show();
			User user = mUserManager.getUser();
			user.setDisarmCode(mNewPasscode.getText().toString());
			mUserManager.setUser(user);
			mUserManager.updateRequiredInformation(new JavelinUserManager.OnUserRequiredInformationUpdateListener() {
				
				@Override
				public void onUserRequiredInformationUpdate(boolean successful, Throwable e) {
					mWorking.dismiss();
					if (successful) {
						UiUtils.toastShort(ResetPasscodePasswordActivity.this, "Updated!");
						finish();
					} else {
						UiUtils.toastLong(ResetPasscodePasswordActivity.this,
								"Could not update, please try again");
					}
				}
			});
		}
	}
	
	private boolean isFormValid() {
		String enteredAuth = mCurrent.getText().toString();
		String newPasscode = mNewPasscode.getText().toString();
		String confirmPasscode = mConfirmPasscode.getText().toString();
		
		if (!mUserManager.getUser().equalsPassword(enteredAuth)
				&& !mUserManager.getUser().equalsDisarmCode(enteredAuth)) {
			mCurrent.setError("Does not match neither password nor passcode.");
		} else if (newPasscode.isEmpty() || newPasscode.length() != 4) {
			mNewPasscode.setError("Passcode must have 4 digits.");
		} else if (!newPasscode.equals(confirmPasscode)) {
			mConfirmPasscode.setError("Confirmation does not match passcode.");
		} else {
			return true;
		}
		return false;
	}
	
	private ProgressDialog getWorkingDialog() {
		ProgressDialog dialog = new ProgressDialog(this);
		dialog.setMessage("Working. Please wait...");
		dialog.setCancelable(false);
		return dialog;
	}
	
	private AlertDialog getResetPasswordDialog() {
		return new AlertDialog.Builder(this)
				.setTitle("Password Reset")
				.setMessage("In order to reset your password, an email has to be sent to your" +
						" email address, then re-login is necessary to verify your identity." +
						" Do you want to reset your password?")
				.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						
						mWorking.show();
						
						mManager.sendPasswordResetEmail(mUserManager.getUser().email,
								new JavelinClient.OnRequestPasswordResetListener() {
							
							@Override
							public void onRequestPasswordReset(boolean successful, Throwable e) {
								
								mWorking.dismiss();
								
								if (successful) {
									UiUtils.toastShort(ResetPasscodePasswordActivity.this,
											"Email requested. Check your inbox!");
									mUserManager.logOut(new JavelinUserManager.OnUserLogOutListener() {
										
										@Override
										public void onUserLogOut(boolean successful, Throwable e) {}
									});
									
									UiUtils.startActivityNoStack(ResetPasscodePasswordActivity.this,
											MainActivity.class);
								} else {
									UiUtils.toastLong(ResetPasscodePasswordActivity.this,
											"Error requesting password reset email." +
											" Please try again later.");
								}
							}
						});
					}
				})
				.setNegativeButton("No", null)
				.create();
	}
}
