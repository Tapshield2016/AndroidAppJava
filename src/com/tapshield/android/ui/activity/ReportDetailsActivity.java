package com.tapshield.android.ui.activity;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinSocialReportingManager.SocialReportingListener;
import com.tapshield.android.api.model.SocialCrime;
import com.tapshield.android.api.model.SocialCrime.SocialCrimes;
import com.tapshield.android.api.spotcrime.SpotCrimeClient;
import com.tapshield.android.api.spotcrime.SpotCrimeClient.SpotCrimeCallback;
import com.tapshield.android.api.spotcrime.model.Crime;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.utils.DateTimeUtils;
import com.tapshield.android.utils.SocialReportsUtils;
import com.tapshield.android.utils.SpotCrimeUtils;
import com.tapshield.android.utils.UiUtils;

public class ReportDetailsActivity extends BaseFragmentActivity
		implements SpotCrimeCallback, SocialReportingListener {

	public static final String EXTRA_REPORT_ID = "com.tapshield.android.intent.extra.report_id";
	public static final String EXTRA_REPORT_TYPE = "com.tapshield.android.intent.extra.report_type";
	
	public static final int TYPE_SPOTCRIME = 100;
	public static final int TYPE_SOCIALCRIME = 200;
	
	private JavelinClient mJavelin;
	private ImageView mTypeImage;
	private TextView mTypeText;
	private TextView mDateTime;
	private TextView mPlace;
	private TextView mDescription;
	private TextView mYourReport;
	private FrameLayout mMediaContainer;
	private ProgressBar mMediaLoading;
	private ProgressDialog mLoading;
	private boolean mCurrentUserIsTheReporter;
	private String mCurrentUserUrl;
	private ProgressDialog mDeleting;
	private SocialCrime mSocialCrime;
	
	@Override
	protected void onCreate(Bundle savedInstance) {
		super.onCreate(savedInstance);
		setContentView(R.layout.activity_report_details);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		mTypeImage = (ImageView) findViewById(R.id.report_image_type);
		mTypeText = (TextView) findViewById(R.id.report_text_type);
		mDateTime = (TextView) findViewById(R.id.report_text_datetime);
		mPlace = (TextView) findViewById(R.id.report_text_location);
		mDescription = (TextView) findViewById(R.id.report_text_description);
		mYourReport = (TextView) findViewById(R.id.report_text_yourreport);
		mMediaContainer = (FrameLayout) findViewById(R.id.report_media_container);
		mMediaLoading = (ProgressBar) findViewById(R.id.report_media_loading);
		
		mJavelin = JavelinClient.getInstance(this, TapShieldApplication.JAVELIN_CONFIG);
		mCurrentUserUrl = mJavelin.getUserManager().getUser().url;
		mCurrentUserIsTheReporter = false;
		
		mLoading = getLoadingDialog();
		
		Intent intent;
		Bundle extras;
		if ((intent = getIntent()) != null
				&& ((extras = intent.getExtras()) != null)
				&& extras.containsKey(EXTRA_REPORT_TYPE)
				&& extras.containsKey(EXTRA_REPORT_ID)) {
			
			mLoading.show();
			
			boolean isSpotCrime = extras.getInt(EXTRA_REPORT_TYPE) == TYPE_SPOTCRIME;
			
			//social crimes handle direct urls, while spotcrime gives an integer value
			if (isSpotCrime) {
				int crimeId = extras.getInt(EXTRA_REPORT_ID);
				SpotCrimeClient
						.getInstance(TapShieldApplication.SPOTCRIME_CONFIG)
						.details(crimeId, this);
			} else {
				String urlId = extras.getString(EXTRA_REPORT_ID);
				mJavelin
						.getSocialReportingManager()
						.details(urlId, this);
			}
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		if (mCurrentUserIsTheReporter) {
			getMenuInflater().inflate(R.menu.delete, menu);
			return true;
		}
		
		return false;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			done();
			return true;
		case R.id.action_delete:
			if (mCurrentUserIsTheReporter && mSocialCrime != null) {
				mDeleting = getDeletingDialog();
				mDeleting.show();
				mJavelin
						.getSocialReportingManager()
						.deleteReport(mSocialCrime, this);
			}
			return true;
		}
		return false;
	}
	
	private ProgressDialog getLoadingDialog() {
		ProgressDialog d = new ProgressDialog(this);
		d.setMessage(getString(R.string.ts_reporting_dialog_loading_message));
		d.setIndeterminate(true);
		d.setOnCancelListener(new DialogInterface.OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
				done();
			}
		});
		return d;
	} 
	
	private ProgressDialog getDeletingDialog() {
		ProgressDialog d = new ProgressDialog(this);
		d.setMessage(getString(R.string.ts_reporting_dialog_delete_message));
		d.setIndeterminate(true);
		d.setOnCancelListener(new DialogInterface.OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
				error(null);
				done();
			}
		});
		return d;
	}
	
	private void setDetails(final String type, final String dateTime, final String place,
			final String description, final int extraType) {
		
		String simplifiedType = type.trim().toLowerCase(Locale.getDefault());
		
		final int typeImageResource =
				(extraType == TYPE_SPOTCRIME) ?
				SpotCrimeUtils.getDrawableOfType(simplifiedType, false) :
				SocialReportsUtils.getDrawableOfType(simplifiedType, false);
		
		final int typeColorResource = (extraType == TYPE_SPOTCRIME) ?
				R.color.ts_alert_red : R.color.ts_brand_light;
		final int typeColor = getResources().getColor(typeColorResource);
		
		mTypeImage.setImageResource(typeImageResource);
		mTypeText.setText(type);
		mTypeText.setTextColor(typeColor);
		mDateTime.setText(dateTime);
		mDescription.setText(description);
		
		if (place != null) {
			mPlace.setText(place);
			mPlace.setVisibility(View.VISIBLE);
		}
		
		mLoading.dismiss();
	}

	//method only for social crimes (spotcrime does not support media)
	private void setMedia(SocialCrime socialCrime) {
		
		String urlAudio = socialCrime.getReportAudio();
		String urlImage = socialCrime.getReportImage();
		String urlVideo = socialCrime.getReportVideo();
		
		if (urlAudio == null && urlImage == null && urlVideo == null) {
			return;
		}

		mMediaLoading.setVisibility(View.VISIBLE);
		
		LayoutParams mediaLayout = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT,
				Gravity.CENTER);
		
		if (urlImage != null) {
			ImageView image = new ImageView(this);
			image.setScaleType(ScaleType.CENTER_INSIDE);
			mMediaContainer.addView(image, mediaLayout);
			new ImageDownloader(image).execute(urlImage);
		} else if (urlVideo != null) {
			final MediaController controller = new MediaController(this, false);
			
			MediaPlayer.OnPreparedListener onPrepare = new MediaPlayer.OnPreparedListener() {
				
				@Override
				public void onPrepared(MediaPlayer mp) {
					mMediaLoading.setVisibility(View.GONE);
					controller.show(500);
				}
			};
			
			VideoView video = new VideoView(this);
			video.setOnPreparedListener(onPrepare);
			video.setMediaController(controller);
			video.setVideoPath(urlVideo);
			video.start();
			
			controller.setMediaPlayer(video);
			
			mMediaContainer.addView(video, 0, mediaLayout);
		} else if (urlAudio != null) {
			//audio not supported for the time being
			mMediaLoading.setVisibility(View.GONE);
		}
	}
	
	@Override
	public void onRequest(boolean ok, List<Crime> results, String errorIfNotOk) {}

	@Override
	public void onDetail(boolean ok, Crime crime, String errorIfNotOk) {
		if (ok) {
			setDetails(crime.getType(),
					DateTimeUtils.getTimeLabelFor(SpotCrimeUtils.getDateTimeFromCrime(crime)),
					crime.getAddress(), crime.getDescription(),
					TYPE_SPOTCRIME);
		} else {
			error(errorIfNotOk);
			done();
		}
	}

	@Override
	public void onReport(boolean ok, int code, String errorIfNotOk) {}

	@Override
	public void onFetch(boolean ok, int code, SocialCrimes socialCrimes, String errorIfNotOk) {}

	@Override
	public void onDetails(boolean ok, int code, SocialCrime socialCrime, String errorIfNotOk) {
		
		if (ok) {
			
			mSocialCrime = socialCrime;
			
			if (socialCrime.getReporter().equals(mCurrentUserUrl)) {
				mCurrentUserIsTheReporter = true;
				invalidateOptionsMenu();
				mYourReport.setVisibility(View.VISIBLE);
			}
			
			setDetails(socialCrime.getTypeName(), DateTimeUtils.getTimeLabelFor(socialCrime.getDate()),
					null, socialCrime.getBody(), TYPE_SOCIALCRIME);
			setMedia(socialCrime);
		} else {
			error(errorIfNotOk);
			done();
		}
	}
	
	@Override
	public void onDelete(boolean ok, int code, SocialCrime socialCrime,
			String errorIfNotOk) {
		if (mDeleting != null) {
			mDeleting.dismiss();
		}
		
		if (ok) {
			UiUtils.toastShort(this, "Report deleted!");
			UiUtils.startActivityNoStack(this, MainActivity.class);
		}
	}
	
	private void error(String message) {
		if (message != null) {
			UiUtils.toastShort(this, message);
		}
	}
	
	private void done() {
		finish();
	}
	
	private class ImageDownloader extends AsyncTask<String, Void, Bitmap> {

		private ImageView imageView;
		
		public ImageDownloader(ImageView imageView) {
			this.imageView = imageView;
		}
		
		@Override
		protected Bitmap doInBackground(String... urls) {
			if (urls == null || urls[0] == null) {
				return null;
			}

			Bitmap bitmap = null;
			
			HttpURLConnection connection = null; 
			try {
				URL url = new URL(urls[0]);
				connection = (HttpURLConnection) url.openConnection();
				InputStream in = connection.getInputStream();
				bitmap = BitmapFactory.decodeStream(in);
				in.close();
			} catch (Exception e) {
				return null;
			} finally {
				if (connection != null) {
					connection.disconnect();
				}
			}

			return bitmap;
		}
		
		@Override
		protected void onPostExecute(Bitmap result) {
			super.onPostExecute(result);
			if (isFinishing()) {
				return;
			}
			
			if (result == null) {
				UiUtils.toastLong(ReportDetailsActivity.this, "Error downloading media. Try again later.");
			} else {
				imageView.setImageBitmap(result);
				mMediaLoading.setVisibility(View.GONE);
			}
		}
	}
}
