package com.tapshield.android.ui.activity;

import org.joda.time.DateTime;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.google.android.gms.location.LocationListener;
import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinSocialReportingManager;
import com.tapshield.android.api.JavelinSocialReportingManager.SocialReportingListener;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.location.LocationTracker;
import com.tapshield.android.service.SocialReportingService;
import com.tapshield.android.utils.SocialReportsUtils;
import com.tapshield.android.utils.UiUtils;

public class ReportActivity extends BaseFragmentActivity
		implements LocationListener, SocialReportingListener, OnClickListener {

	public static final String EXTRA_TYPE_INDEX = "com.tapshield.android.intent.extra.report_type_index";

	private static final int INTENT_REQUEST_PICK_AUDIO = 10;
	private static final int INTENT_REQUEST_PICK_IMAGE = 20;
	private static final int INTENT_REQUEST_PICK_VIDEO = 30;
	private static final int INTENT_REQUEST_TAKE_AUDIO = 40;
	private static final int INTENT_REQUEST_TAKE_IMAGE = 50;
	private static final int INTENT_REQUEST_TAKE_VIDEO = 60;
	private static final String DATETIME_FORMAT = "MM/dd/yyyy HH:mm";
	
	private TextView mTypeText;
	private ImageView mTypeImage;
	private TextView mDatetime;
	private EditText mDescription;
	private CheckBox mAnonymous;
	private Button mMediaAudio;
	private Button mMediaImage;
	private Button mMediaVideo;
	
	private JavelinSocialReportingManager mJavelinReporter;
	private LocationTracker mTracker;
	private Location mLocation;
	private ProgressDialog mReportingDialog;
	private boolean mReporting = false;
	private boolean mWaitingForLocation = false;
	private Uri mMediaUri;
	
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
		mMediaAudio = (Button) findViewById(R.id.report_button_media_audio);
		mMediaImage = (Button) findViewById(R.id.report_button_media_image);
		mMediaVideo = (Button) findViewById(R.id.report_button_media_video);

		mJavelinReporter = JavelinClient
				.getInstance(this, TapShieldApplication.JAVELIN_CONFIG)
				.getSocialReportingManager();
		mTracker = LocationTracker.getInstance(this);
		mReportingDialog = getReportingDialog();
		
		mMediaAudio.setOnClickListener(this);
		mMediaImage.setOnClickListener(this);
		mMediaVideo.setOnClickListener(this);
		
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
	
	@Override
	public void onClick(View view) {
		
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_GET_CONTENT);
		
		int requestCode = 0;
		
		switch (view.getId()) {
		case R.id.report_button_media_audio:
			intent.setType("audio/*");
			requestCode = INTENT_REQUEST_PICK_AUDIO;
			break;
		case R.id.report_button_media_image:
			intent.setType("image/*");
			requestCode = INTENT_REQUEST_PICK_AUDIO;
			break;
		case R.id.report_button_media_video:
			intent.setType("video/*");
			requestCode = INTENT_REQUEST_PICK_AUDIO;
			break;
		default:
			intent = null;
		}
		
		if (intent != null) {
			startActivityForResult(intent, requestCode);
		}
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
			
			if (mReportingDialog.isShowing()) {
				mReportingDialog.dismiss();
			}
			
			String type = mTypeText.getText().toString().trim();
			String description = mDescription.getText().toString();
			String latitude = Double.toString(mLocation.getLatitude());
			String longitude = Double.toString(mLocation.getLongitude());
			boolean anonymously = mAnonymous.isChecked();
			
			Bundle extras = new Bundle();
			extras.putString(SocialReportingService.EXTRA_TYPE, type);
			extras.putString(SocialReportingService.EXTRA_DESCRIPTION, description);
			extras.putString(SocialReportingService.EXTRA_LOC_LAT, latitude);
			extras.putString(SocialReportingService.EXTRA_LOC_LON, longitude);
			extras.putBoolean(SocialReportingService.EXTRA_ANONYMOUS, anonymously);
			extras.putStringArray(SocialReportingService.EXTRA_MEDIA_URI,
					mMediaUri == null ? null : new String[]{ mMediaUri.toString() });
			
			Intent service = new Intent(ReportActivity.this, SocialReportingService.class);
			service.putExtras(extras);
			startService(service);
			
			UiUtils.startActivityNoStack(this, MainActivity.class);
			
			//mJavelinReporter.report(description, type, latitude, longitude, anonymously, this);
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
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (resultCode != RESULT_OK) {
			return;
		}
		
		Uri uri;
		
		if (data == null || (uri = data.getData()) == null) {
			return;
		}
		
		if (requestCode == INTENT_REQUEST_PICK_AUDIO
				|| requestCode == INTENT_REQUEST_PICK_IMAGE
				|| requestCode == INTENT_REQUEST_PICK_VIDEO) {
			
			mMediaUri = uri;
			
			//kitkat (4.4) implements a new storage access framework
			if (TapShieldApplication.VERSION_AT_LEAST_KITKAT) {
				//get inputstream via filedescriptor and filename before passing to aws
				
			} else {
				//get file object
			}
			
			ContentResolver contentResolver = getContentResolver();
			
			String[] fileInfo = {OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};
			
			Cursor cursor = contentResolver.query(uri, fileInfo, null, null, null);
			
			if (cursor == null || !cursor.moveToFirst()) {
				UiUtils.toastShort(this, "Cursor (" + cursor + ") is null or does not have first");
				return;
			}
			
			int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
			int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
			
			String name = cursor.getString(nameIndex);
			String size = null;
			
			ObjectMetadata metadata = new ObjectMetadata();
			metadata.setContentType(contentResolver.getType(uri));
			
			if (!cursor.isNull(sizeIndex) && (size = cursor.getString(sizeIndex)) != null) {
				metadata.setContentLength(Long.parseLong(size));
			}
			
			cursor.close();
			
			String message = name + (size != null ? " (" + size + ")" : new String());
			
			new AlertDialog.Builder(this)
					.setTitle("Media file selected")
					.setMessage(message)
					.create()
					.show();
		}
	}
}
