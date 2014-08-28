package com.tapshield.android.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinUserManager;
import com.tapshield.android.api.model.Agency;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.utils.StringUtils;
import com.tapshield.android.utils.UiUtils;

public class AddEmailActivity extends BaseFragmentActivity implements OnClickListener {

	private TextView mInstructions;
	private EditText mEmail;
	private Button mAdd;
	private Button mResend;
	private Button mComplete;
	
	private JavelinClient mJavelin;
	private JavelinUserManager mUserManager;
	
	@Override
	protected void onCreate(Bundle savedInstance) {
		super.onCreate(savedInstance);
		setContentView(R.layout.activity_addemail);
		
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
	}
	
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		
		String domain = mUserManager.getUser().agency.domain;
		String instructions = String.format("The selected organization requires emails ending in" +
				" '%s', which you don't have. Add a valid email to your account.", domain);
		mInstructions.setText(instructions);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.cancel, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_cancel:
			cancel();
			return true;
		}
		return false;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.addemail_button_add:
			if (isEmailValid()) {
				
			}
			break;
		case R.id.addemail_button_resend:
			break;
		case R.id.addemail_button_complete:
			if (isEmailValid()) {
				
			}
			break;
		}
	}
	
	private boolean isEmailValid() {
		String email = mEmail.getText().toString().trim();
		
		if (!StringUtils.isEmailValid(email)) {
			mEmail.setError("Email structure is invalid. Example: user@where.com");
			return false;
		}
		
		Agency agency = mUserManager.getUser().agency;
		
		if (agency.requiredDomainEmails && !email.endsWith(agency.domain)) {
			mEmail.setError("Email must end with '" + agency.domain + "'");
			return false;
		}
		
		return true;
	}
	
	private void cancel() {
		finish();
	}
	
	private void done() {
		UiUtils.startActivityNoStack(this, MainActivity.class);
	}
}
