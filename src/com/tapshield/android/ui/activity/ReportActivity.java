package com.tapshield.android.ui.activity;

import org.joda.time.DateTime;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.location.Location;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.location.LocationListener;
import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinSocialReportingManager;
import com.tapshield.android.api.JavelinSocialReportingManager.SocialReportingListener;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.location.LocationTracker;
import com.tapshield.android.utils.SocialReportsUtils;
import com.tapshield.android.utils.UiUtils;

public class ReportActivity extends BaseFragmentActivity
		implements LocationListener, SocialReportingListener {

	public static final String EXTRA_TYPE_INDEX = "com.tapshield.android.intent.extra.report_type_index";
	
	private static final String DATETIME_FORMAT = "MM/dd/yyyy HH:mm";
	
	private TextView mTypeText;
	private ImageView mTypeImage;
	private TextView mDatetime;
	private EditText mDescription;
	private CheckBox mAnonymous;
	
	private JavelinSocialReportingManager mJavelinReporter;
	private LocationTracker mTracker;
	private Location mLocation;
	private ProgressDialog mReportingDialog;
	private boolean mReporting = false;
	private boolean mWaitingForLocation = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_report);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		mTypeText = (TextView) findViewById(R.id.report_text_type);
		mTypeImage = (ImageView) findViewById(R.id.report_image_type);
		mDatetime = (TextView) findViewById(R.id.report_text_datetime);
		mDescription = (EditText) findViewById(R.id.report_edit);
		mAnonymous = (CheckBox) findViewById(R.id.report_checkbox);

		mJavelinReporter = JavelinClient
				.getInstance(this, TapShieldApplication.JAVELIN_CONFIG)
				.getSocialReportingManager();
		mTracker = LocationTracker.getInstance(this);
		mReportingDialog = getReportingDialog();
		
		setUi();
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.done, menu);
		return true;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mTracker.addLocationListener(this);
		mTracker.start();
	}
	
	@Override
	protected void onPause() {
		mTracker.removeLocationListener(this);
		mTracker.stop();
		super.onPause();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			UiUtils.startActivityNoStack(this, MainActivity.class);
			return true;
		case R.id.action_done:
			report();
			return true;
		}
		return false;
	}
	
	private void setUi() {
		int typeIndex = getIntent().getIntExtra(EXTRA_TYPE_INDEX, -1);
		if (typeIndex >= 0) {
			String type = JavelinSocialReportingManager.TYPE_LIST[typeIndex];
			int res = SocialReportsUtils.getImageResourceByType(type);
			mTypeImage.setImageResource(res);
			mTypeText.setText(type);
			mDatetime.setText(new DateTime().toString(DATETIME_FORMAT));
		}
	}
	
	private ProgressDialog getReportingDialog() {
		ProgressDialog p = new ProgressDialog(this);
		p.setIndeterminate(true);
		p.setMessage(getString(R.string.ts_reporting_dialog_message));
		p.setCancelable(true);
		p.setOnCancelListener(new DialogInterface.OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
				finish();
			}
		});
		return p;
	}
	
	private void report() {
		if (mLocation == null) {
			mWaitingForLocation = true;
			mReportingDialog.show();
		}
		
		if (mLocation != null && !mReporting) {
			mReporting = true;
			mReportingDialog.show();
			
			String type = mTypeText.getText().toString().trim();
			String description = mDescription.getText().toString();
			String latitude = Double.toString(mLocation.getLatitude());
			String longitude = Double.toString(mLocation.getLongitude());
			boolean anonymously = mAnonymous.isChecked();
			
			mJavelinReporter.report(description, type, latitude, longitude, anonymously, this);
			//javelinsocualreportingmanager.report(type, description, latitude, longitude, this);
			//toast(ok), dialog.dismiss(), finish() on callback ^
		}
	}
	
	@Override
	public void onReport(boolean ok, int code, String errorIfNotOk) {
		mReportingDialog.dismiss();
		if (ok) {
			UiUtils.toastShort(this, getString(R.string.ts_reporting_toast_successful));
			UiUtils.startActivityNoStack(this, MainActivity.class);
		} else {
			final String errorMessage =
					getString(R.string.ts_reporting_dialog_message_error) + " (" + code + ")";
			new AlertDialog
					.Builder(this)
					.setTitle(R.string.ts_common_error)
					.setMessage(errorMessage)
					.setNeutralButton(R.string.ts_common_ok, null)
					.create()
					.show();
		}
	}
	
	@Override
	public void onLocationChanged(Location location) {
		mLocation = location;
		if (mWaitingForLocation) {
			report();
		}
	}
}
