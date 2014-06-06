package com.tapshield.android.ui.activity;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.joda.time.DateTime;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;
import android.widget.VideoView;

import com.google.android.gms.location.LocationListener;
import com.tapshield.android.R;
import com.tapshield.android.api.JavelinSocialReportingManager;
import com.tapshield.android.location.LocationTracker;
import com.tapshield.android.service.SocialReportingService;
import com.tapshield.android.utils.SocialReportsUtils;
import com.tapshield.android.utils.UiUtils;

public class ReportActivity extends BaseFragmentActivity
		implements LocationListener, OnClickListener {

	public static final String EXTRA_TYPE_INDEX = "com.tapshield.android.intent.extra.report_type_index";

	private static final int INTENT_REQUEST_PICK_AUDIO = 10;
	private static final int INTENT_REQUEST_PICK_IMAGE = 20;
	private static final int INTENT_REQUEST_PICK_VIDEO = 30;
	private static final int INTENT_REQUEST_TAKE_AUDIO = 40;
	private static final int INTENT_REQUEST_TAKE_IMAGE = 50;
	private static final int INTENT_REQUEST_TAKE_VIDEO = 60;

	private enum OptionType {
		EXISTING,
		NEW
	}
	
	private enum MediaType {
		AUDIO,
		IMAGE,
		VIDEO
	}
	
	private static final String DATETIME_FORMAT = "MM/dd/yyyy HH:mm";
	
	private TextView mTypeText;
	private ImageView mTypeImage;
	private TextView mDatetime;
	private EditText mDescription;
	private CheckBox mAnonymous;
	private Button mMediaExisting;
	private Button mMediaNew;
	private ImageButton mMediaRemove;

	private boolean mReporting = false;
	private boolean mWaitingForLocation = false;
	
	private LocationTracker mTracker;
	private Location mLocation;
	private ProgressDialog mReportingDialog;
	
	private AlertDialog.Builder mMediaTypeDialogBuilder;
	private Uri mMediaUri;
	private File mMediaNewPhotoFile;
	private OptionType mOptionType;
	private MediaType mMediaType;
	
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
		mMediaExisting = (Button) findViewById(R.id.report_button_media_existing);
		mMediaNew = (Button) findViewById(R.id.report_button_media_new);
		mMediaRemove = (ImageButton) findViewById(R.id.report_button_media_remove);
		
		mTracker = LocationTracker.getInstance(this);
		mReportingDialog = getReportingDialog();
		
		mMediaExisting.setOnClickListener(this);
		mMediaNew.setOnClickListener(this);
		mMediaRemove.setOnClickListener(this);
		
		setTypeUi();
		
		mMediaTypeDialogBuilder = getBasicMediaTypeDialogBuilder();
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
		setMediaUi();
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
		
		switch (view.getId()) {
		case R.id.report_button_media_existing:
			showMediaTypeDialog(OptionType.EXISTING);
			break;
		case R.id.report_button_media_new:
			showMediaTypeDialog(OptionType.NEW);
			break;
		case R.id.report_button_media_remove:
			mMediaNewPhotoFile = null;
			mMediaUri = null;
			setMediaUi();
			break;
		}
	}
	
	private void setTypeUi() {
		int typeIndex = getIntent().getIntExtra(EXTRA_TYPE_INDEX, -1);
		if (typeIndex >= 0) {
			String type = JavelinSocialReportingManager.TYPE_LIST[typeIndex];
			int res = SocialReportsUtils.getDrawableOfType(type, false);
			mTypeImage.setImageResource(res);
			mTypeText.setText(type);
			mDatetime.setText(new DateTime().toString(DATETIME_FORMAT));
			
			String description = getIntent().getStringExtra(SocialReportingService.EXTRA_DESCRIPTION);
			if (description != null) {
				mDescription.setText(description);
			}
			
			boolean anon = getIntent().getBooleanExtra(SocialReportingService.EXTRA_ANONYMOUS, false);
			mAnonymous.setChecked(anon);
		}
	}
	
	private void setMediaUi() {
		
		boolean mediaSelected = mMediaUri != null;
		
		findViewById(R.id.report_layout_media_options)
				.setVisibility(mediaSelected ? View.GONE : View.VISIBLE);
		
		findViewById(R.id.report_layout_media_preview)
				.setVisibility(mediaSelected ? View.VISIBLE: View.GONE);
		
		FrameLayout container = (FrameLayout) findViewById(R.id.report_layout_media_preview_container);
		
		container.removeAllViews();
		
		if (mediaSelected) {

			FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
					FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT,
					Gravity.CENTER);

			MediaType type = getMimeTypeAsMediaType(mMediaUri);

			if (type == MediaType.IMAGE) {
				ImageView image = new ImageView(this);
				image.setLayoutParams(params);
				image.setScaleType(ScaleType.CENTER_INSIDE);
				image.setImageURI(mMediaUri);
				container.addView(image);
			} else if (type == MediaType.VIDEO) {
				VideoView video = new VideoView(this);
				video.setVideoURI(mMediaUri);
				video.setLayoutParams(params);
				container.addView(video);
				video.start();
			}
		}
	}
	
	private ProgressDialog getReportingDialog() {
		ProgressDialog p = new ProgressDialog(this);
		p.setIndeterminate(true);
		p.setMessage(getString(R.string.ts_reporting_location_dialog_message));
		p.setCancelable(true);
		p.setOnCancelListener(new DialogInterface.OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
				finish();
			}
		});
		return p;
	}
	
	private AlertDialog.Builder getBasicMediaTypeDialogBuilder() {
		return new AlertDialog.Builder(this)
				.setTitle(R.string.ts_reporting_dialog_media_options_title)
				.setPositiveButton(R.string.ts_reporting_dialog_media_options_button_video,
						new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mMediaType = MediaType.VIDEO;
						performMediaAction();
					}
				})
				.setNeutralButton(R.string.ts_reporting_dialog_media_options_button_image,
						new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mMediaType = MediaType.IMAGE;
						performMediaAction();
					}
				})
				.setNegativeButton(R.string.ts_reporting_dialog_media_options_button_audio,
						new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mMediaType = MediaType.AUDIO;
						performMediaAction();
					}
				});
	}
	
	private void showMediaTypeDialog(OptionType optionType) {
		
		mOptionType = optionType;
		
		int messageResource = mOptionType ==
				OptionType.EXISTING ?
				R.string.ts_reporting_dialog_media_options_message_existing :
				R.string.ts_reporting_dialog_media_options_message_new;
		
		mMediaTypeDialogBuilder
				.setMessage(messageResource)
				.create()
				.show();
	}
	
	private void performMediaAction() {
		
		int intentRequestCode = -1;
		
		if (mOptionType == OptionType.EXISTING) {
			if (mMediaType == MediaType.AUDIO) {
				intentRequestCode = INTENT_REQUEST_PICK_AUDIO;
			} else if (mMediaType == MediaType.IMAGE) {
				intentRequestCode = INTENT_REQUEST_PICK_IMAGE;
			} else if (mMediaType == MediaType.VIDEO) {
				intentRequestCode = INTENT_REQUEST_PICK_VIDEO;
			}
		} else if (mOptionType == OptionType.NEW) {
			if (mMediaType == MediaType.AUDIO) {
				intentRequestCode = INTENT_REQUEST_TAKE_AUDIO;
			} else if (mMediaType == MediaType.IMAGE) {
				intentRequestCode = INTENT_REQUEST_TAKE_IMAGE;
			} else if (mMediaType == MediaType.VIDEO) {
				intentRequestCode = INTENT_REQUEST_TAKE_VIDEO;
			}
		}
		
		//set most common intent for following cases
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		
		switch (intentRequestCode) {
		case INTENT_REQUEST_PICK_AUDIO:
			intent.setType("audio/*");
			break;
		case INTENT_REQUEST_PICK_IMAGE:
			intent.setType("image/*");
			break;
		case INTENT_REQUEST_PICK_VIDEO:
			intent.setType("video/*");
			break;
		case INTENT_REQUEST_TAKE_AUDIO:
			//record audio
			break;
		case INTENT_REQUEST_TAKE_IMAGE:
			intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		    if (intent.resolveActivity(getPackageManager()) != null) {
		    	
		    	String name = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
		    	
		    	File photoFile = null;
		        try {
		        	File dir = Environment
		        			.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
		            photoFile = new File(dir, name + ".jpg");
		            
		            if (!photoFile.exists()) {
		            	photoFile.createNewFile();
		            }
		        } catch (Exception e) {}

		        if (photoFile != null) {
		        	mMediaNewPhotoFile = photoFile;
		        	intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
		        }
		    }
			break;
		case INTENT_REQUEST_TAKE_VIDEO:
			intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
			break;
		}
		
		if (intent != null && intent.resolveActivity(getPackageManager()) != null) {
			startActivityForResult(intent, intentRequestCode);
		}
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
			//copy type extra so in case of error the notification pending intent recreates it
			extras.putInt(EXTRA_TYPE_INDEX, getIntent().getIntExtra(EXTRA_TYPE_INDEX, -1));
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
		
		mMediaUri = null;

		//if different, it means the reference for new image via camera can be discarded due to a
		//  different media being retrieved
		if (requestCode == INTENT_REQUEST_TAKE_IMAGE) {
			ContentValues values = new ContentValues();
			values.put(MediaStore.Images.Media.DATA, mMediaNewPhotoFile.getAbsolutePath());
			mMediaUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
					values);
		} else {
			mMediaNewPhotoFile = null;
		}

		//deal with uri-returned data (all picks and video recording)
		if (requestCode == INTENT_REQUEST_PICK_AUDIO
				|| requestCode == INTENT_REQUEST_PICK_IMAGE
				|| requestCode == INTENT_REQUEST_PICK_VIDEO
				|| requestCode == INTENT_REQUEST_TAKE_VIDEO) {

			if (data != null) {
				mMediaUri = data.getData();
			}
		}
		
		setMediaUi();
	}
	
	private MediaType getMimeTypeAsMediaType(final Uri uri) {
		ContentResolver contentResolver = getContentResolver();
		String mimeType = contentResolver.getType(uri);
		
		if (mimeType == null) {
			return null;
		}
		
		if (mimeType.contains("audio")) {
			return MediaType.AUDIO;
		} else if (mimeType.contains("image")) {
			return MediaType.IMAGE;
		} else if (mimeType.contains("video")) {
			return MediaType.VIDEO;
		}
		
		return null;
	}
	
	private String getNameWithUri(final Uri uri) {
		ContentResolver contentResolver = getContentResolver();
		String[] fileInfo = {OpenableColumns.DISPLAY_NAME};
		
		Cursor cursor = contentResolver.query(uri, fileInfo, null, null, null);
		
		if (cursor == null || !cursor.moveToFirst()) {
			return null;
		}
		
		int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
		String name = cursor.getString(nameIndex);
		
		cursor.close();
		
		return name;
	}
}
