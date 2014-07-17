package com.tapshield.android.service;

import java.util.HashMap;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.speech.tts.UtteranceProgressListener;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.tapshield.android.R;
import com.tapshield.android.ui.activity.MainActivity;

@SuppressWarnings("deprecation")
public class EntourageTtsTimeNotificationService extends Service implements OnInitListener {

	public static final String EXTRA_MESSAGE = "com.tapshield.android.intent.extra.entourage_tts_m";
	
	private static final int NOTIFICATION_ID = 1000;
	
	private NotificationManager mNotificationManager;
	private TextToSpeech mTts;
	private HashMap<String, String> mTtsIds;
	private String mTitle;
	private String mMessage;
	
	@Override
	public IBinder onBind(Intent i) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mTtsIds = new HashMap<String, String>();
		mTtsIds.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
				Long.toString(System.currentTimeMillis()));
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i("bbb", "tts notif service started");
		String extraMessage = intent.getStringExtra(EXTRA_MESSAGE);
		mTitle = extraMessage != null ? "Entourage" : getResources().getString(R.string.app_name);
		mMessage = extraMessage != null ? extraMessage : "Entourage will end soon.";
		showNotification();
		mTts = new TextToSpeech(this, this);
		return START_STICKY;
	}

	private void showNotification() {
		
		Intent home = new Intent(this, MainActivity.class);
		home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent tap = PendingIntent.getActivity(this, 50, home, 0);
		
		Notification notification = new NotificationCompat.Builder(this)
				.setSmallIcon(R.drawable.ic_stat)
				.setContentTitle(mTitle)
				.setContentText(mMessage)
				.setContentIntent(tap)
				.setAutoCancel(true)
				.setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
				.build();
		mNotificationManager.notify(NOTIFICATION_ID, notification);
	}
	
	@SuppressLint("NewApi")
	@Override
	public void onInit(int status) {
		if (status == TextToSpeech.SUCCESS) {
			int langAvailable = mTts.isLanguageAvailable(Locale.US);
			boolean ok = langAvailable == TextToSpeech.LANG_AVAILABLE
					|| langAvailable == TextToSpeech.LANG_COUNTRY_AVAILABLE
					|| langAvailable == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE;
			if (ok) {

				if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {

					mTts.setOnUtteranceCompletedListener(new OnUtteranceCompletedListener() {

						@Override
						public void onUtteranceCompleted(String utteranceId) {
							mTts.shutdown();
							stopSelf();
						}
					});
				} else {
					UtteranceProgressListener upl = new UtteranceProgressListener() {

						@Override
						public void onStart(String utteranceId) {}

						@Override
						public void onError(String utteranceId) {}

						@Override
						public void onDone(String utteranceId) {
							mTts.shutdown();
							stopSelf();
						}
					};
					mTts.setOnUtteranceProgressListener(upl);
				}

				mTts.speak(mMessage, TextToSpeech.QUEUE_FLUSH, mTtsIds);
			}
		}
	}
	
	public static void dismissNotifications(Context context) {
		((NotificationManager) context.getSystemService(NOTIFICATION_SERVICE))
				.cancel(NOTIFICATION_ID);
	}
}
