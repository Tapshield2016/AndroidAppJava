package com.tapshield.android.ui.activity;

import android.app.Activity;
import android.os.Bundle;

import com.tapshield.android.R;
import com.tapshield.android.R.id;
import com.tapshield.android.R.layout;
import com.tapshield.android.ui.fragment.SettingsFragment;

public class SettingsActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);

		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.container, new SettingsFragment()).commit();
		}
	}
}
