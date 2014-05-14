package com.tapshield.android.ui.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.utils.UiUtils;

public class OutOfBoundsActivity extends Activity implements OnClickListener {

	private Button mButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_outofbounds);
		mButton = (Button) findViewById(R.id.outofbounds_button);
		mButton.setOnClickListener(this);
	}

	@Override
	public void onClick(View view) {
		String secondaryNumber = JavelinClient
				.getInstance(this, TapShieldApplication.JAVELIN_CONFIG)
				.getUserManager()
				.getUser()
				.agency
				.secondaryNumber;
		
		UiUtils.MakePhoneCall(this, secondaryNumber);
		UiUtils.startActivityNoStack(this, MainActivity.class);
	}
}
