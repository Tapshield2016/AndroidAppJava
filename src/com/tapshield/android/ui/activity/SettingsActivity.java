package com.tapshield.android.ui.activity;

import android.app.ActionBar;
import android.os.Bundle;
import android.view.MenuItem;

import com.tapshield.android.R;
import com.tapshield.android.ui.fragment.SettingsFragment;
import com.tapshield.android.utils.UiUtils;

public class SettingsActivity extends BaseFragmentActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);

		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		
		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.container, new SettingsFragment()).commit();
		}
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		
		switch (item.getItemId()) {
		case android.R.id.home:
			UiUtils.startActivityNoStack(this, MainActivity.class);
			return true;
		}
		
		return false;
	}
}
