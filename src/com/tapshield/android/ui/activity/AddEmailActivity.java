package com.tapshield.android.ui.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinUserManager;
import com.tapshield.android.api.JavelinUserManager.UserEmailsListener;
import com.tapshield.android.api.model.Agency;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.utils.StringUtils;
import com.tapshield.android.utils.UiUtils;

public class AddEmailActivity extends BaseFragmentActivity
		implements OnClickListener, UserEmailsListener {

	public static final String EXTRA_UNVERIFIED_EMAIL = "com.tapshield.android.extra.unverified_email";
	
	private TextView mInstructions;
	private EditText mEmail;
	private Button mAdd;
	private Button mResend;
	private Button mComplete;
	
	private JavelinClient mJavelin;
	private JavelinUserManager mUserManager;
	
	private ProgressDialog mWorkingDialog;
	
	@Override
	protected void onCreate(Bundle savedInstance) {
		super.onCreate(savedInstance);
		setContentView(R.layout.activity_addemail);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		UiUtils.setStepIndicatorInActionBar(this, 1, 3,
				R.string.ts_registration_actionbar_title_emailverification);
		
		mInstructions = (TextView) findViewById(R.id.addemail_text_instructions);
		mEmail = (EditText) findViewById(R.id.addemail_edit_email);
		mAdd = (Button) findViewById(R.id.addemail_button_add);
		mResend = (Button) findViewById(R.id.addemail_button_resend);
		mComplete = (Button) findViewById(R.id.addemail_button_complete);
		
		mJavelin = JavelinClient.getInstance(this, TapShieldApplication.JAVELIN_CONFIG);
		mUserManager = mJavelin.getUserManager();
		
		mAdd.setOnClickListener(this);
		mResend.setOnClickListener(this);
		mComplete.setOnClickListener(this);
		
		mWorkingDialog = new ProgressDialog(this);
		mWorkingDialog.setCancelable(false);
		mWorkingDialog.setMessage("Working. Please wait...");
		mWorkingDialog.setIndeterminate(true);
		
		String unverifiedEmail = null;
		
		Intent i;
		if ((i = getIntent()) != null && i.hasExtra(EXTRA_UNVERIFIED_EMAIL)) {
			unverifiedEmail = i.getStringExtra(EXTRA_UNVERIFIED_EMAIL);
		}
		
		String domain = mUserManager.getTemporaryAgency().domain;
		String instructionsPrefix = 
				String.format("The selected organization requires emails ending in '%s.'", domain);
		String instructionsSuffix = " Please add one that fulfills the requirement.";

		if (unverifiedEmail != null) {
			instructionsSuffix = " Please complete the verification of this email that does" +
					" match the requirement. You should receive an activation email shortly.";
			mEmail.setText(unverifiedEmail);
			mResend.performClick();
		}
		
		mInstructions.setText(instructionsPrefix + instructionsSuffix);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			cancel();
			return true;
		}
		return false;
	}

	@Override
	public void onClick(View v) {
		
		String email = mEmail.getText().toString();
		
		if (!isEmailValid(email)) {
			return;
		}
		
		switch (v.getId()) {
		case R.id.addemail_button_add:
			addEmail(email);
			break;
		case R.id.addemail_button_resend:
			resendActivation(email);
			break;
		case R.id.addemail_button_complete:
			checkActivation(email);
			break;
		}
	}
	
	private boolean isEmailValid(String email) {
		email = email.trim();
		
		if (!StringUtils.isEmailValid(email)) {
			mEmail.setError("Email structure is invalid. Example: user@where.com");
			return false;
		}
		
		Agency agency = mUserManager.getTemporaryAgency();
		
		if (agency.requiredDomainEmails && !email.endsWith(agency.domain)) {
			mEmail.setError("Email must end with '" + agency.domain + "'");
			return false;
		}
		
		return true;
	}
	
	private void addEmail(String email) {
		mAdd.setEnabled(false);
		mWorkingDialog.show();
		mUserManager.addEmail(email, this);
	}
	
	private void checkActivation(String email) {
		mWorkingDialog.show();
		mUserManager.checkEmailActivation(email, this);
	}
	
	private void resendActivation(String email) {
		mWorkingDialog.show();
		mUserManager.sendEmailActivation(email, this);
	}
	
	@Override
	public void onEmailAdded(boolean successful, String extra) {
		//re-enable add button if attempt failed
		mAdd.setEnabled(!successful);
		mWorkingDialog.dismiss();
		String message = successful ? "Email added!" : extra;
		UiUtils.toastShort(this, message);
	}

	@Override
	public void onEmailActivationChecked(boolean successful, String extra) {
		mWorkingDialog.dismiss();
		String message = successful ? "Email verified!" : extra;
		UiUtils.toastLong(this, message);
		done();
	}

	@Override
	public void onEmailActivationRequested(boolean successful, String extra) {
		mWorkingDialog.dismiss();
		String message = successful ? "Check your inbox!" : extra;
		UiUtils.toastShort(this, message);
	}
	
	private void cancel() {
		mUserManager.clearTemporaryAgency();
		finish();
	}
	
	private void done() {
		UiUtils.startActivityNoStack(this, MainActivity.class);
	}
}
