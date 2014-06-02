package com.tapshield.android.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinSocialReportingManager;
import com.tapshield.android.api.JavelinSocialReportingManager.SocialReportingListener;
import com.tapshield.android.api.JavelinUtils;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.ui.activity.ReportActivity;
import com.tapshield.android.utils.UiUtils;

public class SocialReportingService extends Service implements SocialReportingListener {

	public static final String EXTRA_TYPE = "com.tapshield.android.extra.reportingservice_type";
	public static final String EXTRA_DESCRIPTION = "com.tapshield.android.extra.reportingservice_description";
	public static final String EXTRA_LOC_LAT = "com.tapshield.android.extra.reportingservice_location_lat";
	public static final String EXTRA_LOC_LON = "com.tapshield.android.extra.reportingservice_location_lon";
	public static final String EXTRA_ANONYMOUS = "com.tapshield.android.extra.reportingservice_anonymous";
	public static final String EXTRA_MEDIA_URI = "com.tapshield.android.extra.reportingservice_media_uri";
	
	private static final int NOTIFICATION_UPLOAD_ID = 100;
	private static final int NOTIFICATION_ERROR_ID = 200;
	
	private NotificationManager mNotificationManager;
	private JavelinSocialReportingManager mJavelinReporter;
	private NotificationCompat.Builder mNotificationBuilder;
	private Notification mNotificationError;
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mJavelinReporter = JavelinClient.getInstance(this, TapShieldApplication.JAVELIN_CONFIG)
				.getSocialReportingManager();
		mNotificationBuilder =
				new NotificationCompat.Builder(this)
				.setOngoing(true)
				.setSmallIcon(R.drawable.ic_stat)
				.setDefaults(Notification.DEFAULT_ALL)
				.setOnlyAlertOnce(true)
				.setContentTitle(getString(R.string.ts_reporting_media_notification_title))
				.setContentText(getString(R.string.ts_reporting_media_notification_preparing_message));

		Intent report = new Intent(this, ReportActivity.class)
				.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)
				.putExtras(intent.getExtras());
		
		PendingIntent retryPendingIntent = PendingIntent.getActivity(this, 1, report,
				PendingIntent.FLAG_UPDATE_CURRENT);
		
		mNotificationError =
				new NotificationCompat.Builder(this)
				.setSmallIcon(R.drawable.ic_stat)
				.setDefaults(Notification.DEFAULT_ALL)
				.setContentTitle(getString(R.string.ts_reporting_media_notification_title))
				.setContentText(getString(R.string.ts_reporting_media_notification_error_message))
				.setContentIntent(retryPendingIntent)
				.setAutoCancel(true)
				.build();
		
		Uri[] mediaUris = null;
		String[] uriStrings = null;
		if ((uriStrings = intent.getStringArrayExtra(EXTRA_MEDIA_URI)) != null
				&& uriStrings.length > 0) {
			mediaUris = new Uri[uriStrings.length];
			for (int u = 0; u < mediaUris.length; u++) {
				mediaUris[u] = Uri.parse(uriStrings[u]);
			}
		}
		
		new ReportUploader(
				intent.getStringExtra(EXTRA_TYPE),
				intent.getStringExtra(EXTRA_DESCRIPTION),
				intent.getStringExtra(EXTRA_LOC_LAT),
				intent.getStringExtra(EXTRA_LOC_LON),
				intent.getBooleanExtra(EXTRA_ANONYMOUS, false),
				mediaUris)
				.execute();
		return START_STICKY;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	//given string values for type, description, location lat, location lon, and media uri
	//float value given for updating a progress bar if necessary (if media is attached)
	//returning value being string (of media url if present) or null
	
	private class ReportUploader extends AsyncTask<Void, Float, String[]>
			implements JavelinUtils.S3UploadListener {

		private long mTransferred = 0;
		private String type;
		private String description;
		private String lat;
		private String lon;
		private Uri[] mediaUris;
		private boolean anonymous = false;
		
		public ReportUploader(String type, String description, String lat, String lon,
				boolean anonymous, Uri... stringMediaUris) {
			this.type = type;
			this.description = description;
			this.lat = lat;
			this.lon = lon;
			this.anonymous = anonymous;
			this.mediaUris = stringMediaUris;
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mNotificationManager.notify(NOTIFICATION_UPLOAD_ID, mNotificationBuilder.build());
		}
		
		@Override
		protected String[] doInBackground(Void... strings) {
			String[] mediaUrls = null;
			
			//right now only one media upload is supported
			if (mediaUris != null && mediaUris.length > 0) {
				Uri uri = mediaUris[0];
				mediaUrls = new String[1];
				mediaUrls[0] = JavelinUtils.syncUploadFileToS3WithUri(SocialReportingService.this,
						TapShieldApplication.JAVELIN_CONFIG, uri, this);
			}
			
			return mediaUrls;
		}
		
		@Override
		protected void onProgressUpdate(Float... values) {
			
			//each media file has same # of steps

			//progress has 100 * # of media files steps
			int max = 100 * mediaUris.length;
			
			//current media file uploaded
			int which = (int) (1f * values[1]);
			
			//set progress of current media file adding segments of finished media files
			int progress = ((int) (100f * values[0])) + (which * 100);
			
			mNotificationBuilder.setProgress(max, progress, false);
			mNotificationManager.notify(NOTIFICATION_UPLOAD_ID, mNotificationBuilder.build());
		}
		
		@Override
		protected void onPostExecute(String[] mediaUrls) {
			
			//since only one media is supported at the time, a type has to be set for the media file
			//  and set null the other 2
			String[] mediaParams = new String[3];

			String url = mediaUrls == null ? null : mediaUrls[0];
			String mediaType = url == null ? null : getContentResolver().getType(mediaUris[0]);
			
			if (mediaType == null) {
				//it means mediaUrls is also null, no media present, leave current null params array
			} else if (mediaType.contains("audio")) {
				mediaParams[0] = url;
			} else if (mediaType.contains("image")) {
				mediaParams[1] = url;
			} else if (mediaType.contains("video")) {
				mediaParams[2] = url;
			}
			
			mJavelinReporter.report(description, type, lat, lon, anonymous,
					SocialReportingService.this, mediaParams[0], mediaParams[1], mediaParams[2]);
			super.onPostExecute(mediaUrls);
		}
		
		@Override
		public void onUploading(Uri uri, long newBytesTransferred, long total) {
			//only one medial file upload is supported at this time, pass second arg as 0f
			mTransferred += newBytesTransferred;

			//long is an integer, double will allow decimals for float casting as progress value
			double transferred = (double) mTransferred;
			double max = (double) total;
			
			publishProgress((float)(transferred/max), 0f);
		}
		
		@Override
		public void onError(Uri uri, String error) {
			Log.e("aaa", "onerror " + error);
			reportError();
		}
	}

	@Override
	public void onReport(boolean ok, int code, String errorIfNotOk) {
		if (ok) {
			mNotificationManager.cancel(NOTIFICATION_UPLOAD_ID);
			UiUtils.toastLong(SocialReportingService.this, "Report uploaded!");
		} else {
			Log.e("aaa", "onreport " + errorIfNotOk);
			reportError();
		}
	}
	
	private void reportError() {
		mNotificationManager.cancel(NOTIFICATION_UPLOAD_ID);
		mNotificationManager.notify(NOTIFICATION_ERROR_ID, mNotificationError);
	}
}
