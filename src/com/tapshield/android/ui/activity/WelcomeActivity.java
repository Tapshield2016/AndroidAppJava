package com.tapshield.android.ui.activity;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinUserManager;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.utils.UiUtils;

public class WelcomeActivity extends FragmentActivity {

	private Button mLogin;
	private Button mRegistration;
	
	@Override
	protected void onCreate(Bundle b) {
		super.onCreate(b);
		setContentView(R.layout.activity_welcome);

		mLogin = (Button) findViewById(R.id.welcome_button_login);
		mRegistration = (Button) findViewById(R.id.welcome_button_registration);
		
		mLogin.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				UiUtils.startActivityNoStack(WelcomeActivity.this, LoginActivity.class);
			}
		});
		
		mRegistration.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				UiUtils.startActivityNoStack(WelcomeActivity.this, RegistrationActivity.class);
			}
		});
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		JavelinUserManager userManager = JavelinClient.getInstance(this,
				TapShieldApplication.JAVELIN_CONFIG).getUserManager();
		
		if (userManager.isPresent()) {
			finish();
		}
	}
}
