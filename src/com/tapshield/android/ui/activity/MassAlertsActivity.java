package com.tapshield.android.ui.activity;

import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ListView;

import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinMassAlertManager;
import com.tapshield.android.api.JavelinMassAlertManager.OnMassAlertUpdateListener;
import com.tapshield.android.api.model.MassAlert;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.manager.EmergencyManager;
import com.tapshield.android.ui.adapter.MassAlertAdapter;
import com.tapshield.android.utils.UiUtils;

public class MassAlertsActivity extends Activity implements OnMassAlertUpdateListener {

	private JavelinMassAlertManager mManager;
	private ListView mList;
	private MassAlertAdapter mAdapter;
	private Runnable mUpdater;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_massalerts);

		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		
		mManager = JavelinClient.getInstance(MassAlertsActivity.this,
				TapShieldApplication.JAVELIN_CONFIG).getMassAlertManager();
		
		mList = (ListView) findViewById(R.id.massalerts_list);
		//pass null list since it will be defaulted to an empty one until manager delivers
		mAdapter = new MassAlertAdapter(MassAlertsActivity.this,
				R.layout.item_mass_alert,
				R.id.item_mass_alert_text_message,
				R.id.item_mass_alert_text_datetime,
				null);
		mList.setAdapter(mAdapter);
		
		mUpdater = new Runnable() {
			
			@Override
			public void run() {
				mAdapter.notifyDataSetChanged();
			}
		};
	}
	
	@Override
	protected void onResume() {
		mManager.addOnMassAlertUpdateListener(this);
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		mManager.removeOnMassAlertUpdateListener(this);
		super.onPause();
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
	
	@Override
	public void onBackPressed() {
		EmergencyManager emergencyManager = EmergencyManager.getInstance(this);
		UiUtils.startActivityNoStack(this,
				emergencyManager.isRunning() ? AlertActivity.class : MainActivity.class);
	}

	@Override
	public void onMassAlertUpdate(List<MassAlert> allMassAlerts) {
		mAdapter.setItemsNoNotifyDataSetChanged(allMassAlerts);
		mList.post(mUpdater);
	}
}
