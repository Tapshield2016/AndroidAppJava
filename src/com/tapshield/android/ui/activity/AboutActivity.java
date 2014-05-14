package com.tapshield.android.ui.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.tapshield.android.R;

public class AboutActivity extends Activity implements OnClickListener {
	
	private Button mEula;
	private Button mThirdParty;
	private TextView mVersion;
	private AlertDialog mCreditDialog;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);
		
		mEula = (Button) findViewById(R.id.about_button_eula);
		mThirdParty = (Button) findViewById(R.id.about_button_thirdparty);
		mVersion = (TextView) findViewById(R.id.about_text_version);
		mCreditDialog = getCreditDialog();
		
		mVersion.setText(getVersion());
		
		mEula.setOnClickListener(this);
		mThirdParty.setOnClickListener(this);
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.about_button_eula:
			String url = getString(R.string.ts_misc_online_eula);
			Intent eula = new Intent(Intent.ACTION_VIEW);
			eula.setData(Uri.parse(url));
			startActivity(eula);
			break;
		case R.id.about_button_thirdparty:
			mCreditDialog.show();
			break;
		}
	}
	
	private String getVersion() {
		try {
			PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			return "Version " + pInfo.versionName;
		} catch (Exception e) {
			return new String();
		}
	}
	
	private AlertDialog getCreditDialog() {
		return new AlertDialog.Builder(this)
				.setTitle(R.string.ts_about_dialog_thirdparty_title)
				.setMessage(R.string.ts_about_dialog_thirdparty_message)
				.setNeutralButton(R.string.ts_common_ok, null)
				.create();
	}
}
