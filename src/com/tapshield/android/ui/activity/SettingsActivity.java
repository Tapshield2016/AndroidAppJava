package com.tapshield.android.ui.activity;

import android.os.Bundle;

import com.tapshield.android.R;
import com.tapshield.android.ui.fragment.SettingsFragment;

public class SettingsActivity extends BaseFragmentActivity {

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
