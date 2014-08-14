package com.tapshield.android.app;

import java.util.HashMap;
import java.util.List;

import android.app.Application;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.tapshield.android.R;
import com.tapshield.android.api.JavelinAlertManager.AlertListener;
import com.tapshield.android.api.JavelinChatManager.OnNewIncomingChatMessagesListener;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinConfig;
import com.tapshield.android.api.JavelinMassAlertManager.OnNewMassAlertListener;
import com.tapshield.android.api.JavelinUserManager;
import com.tapshield.android.api.googledirections.GoogleDirectionsConfig;
import com.tapshield.android.api.googleplaces.GooglePlacesConfig;
import com.tapshield.android.api.spotcrime.SpotCrimeConfig;
import com.tapshield.android.manager.Notifier;

public class TapShieldApplication extends Application {

	public static final String GOOGLE_PLUS_CLIENT_ID = "825930152848.apps.googleusercontent.com";
	
	public static JavelinConfig JAVELIN_CONFIG =
			new JavelinConfig.Builder()
			.baseUrl("https://dev.tapshield.com/")
			.masterToken("35204055c8518dd538f563ee729e70acef71cfeb")
			.gcmSenderId("597251186165")
			.awsSqsAccessKey("AKIAJSDRUWW6PPF2FWWA")
			.awsSqsSecretKey("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")
			.awsSqsQueueName("alert_queue_dev")
			.awsDynamoDbAccessKey("AKIAJJX2VM346XUKRROA")
			.awsDynamoDbSecretKey("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")
			.awsDynamoDbTable("chat_messages_dev")
			.awsS3AccessKey("AKIAJHIUM7YWZW2T2YIA")
			.awsS3SecretKey("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")
			.awsS3Bucket("dev-media-tapshield-com")
			.build();
	
	public static SpotCrimeConfig SPOTCRIME_CONFIG =
			new SpotCrimeConfig.Builder()
			.key("246c313f9889be187cfbca0c3f5a09f9e4a5d8224edbf86ad795c72b0561")
			.build();
	
	public static GooglePlacesConfig GOOGLEPLACES_CONFIG =
			new GooglePlacesConfig.Builder()
			.key("AIzaSyA5m917LQ6E-9V2tEXhLRl4nhEtbY01ny4")
			.build();
	
	public static GoogleDirectionsConfig GOOGLEDIRECTIONS_CONFIG = 
			new GoogleDirectionsConfig.Builder()
			.key("AIzaSyDrODd9nuDCy6-UGC7JkuG85PA7gcvZS8I")
			.build();

	public static final int CRIMES_PERIOD_HOURS = 24;
	public static final float CRIMES_MARKER_OPACITY_MINIMUM = 0.1f;
	public static final int SPOTCRIME_RECORDS_MIN = 10;
	public static final int SPOTCRIME_RECORDS_MAX = 500;
	public static final int SPOTCRIME_UPDATE_FREQUENCY_SECONDS = 60;
	public static final float SPOTCRIME_RADIUS = 0.25f;
	public static final int SPOTCRIME_EXTRA_PERIOD_DAYS = 14;
	public static final int SPOTCRIME_EXTRA_RECORDS_MAX = 50;
	public static final int SOCIAL_CRIMES_UPDATE_FREQUENCY_SECONDS = 15;
	public static final float SOCIAL_CRIMES_RADIUS = 2.0f;
	
	public enum TrackerName {
	    APP_TRACKER // Tracker used only in this app.
	}

	HashMap<TrackerName, Tracker> mTrackers = new HashMap<TrackerName, Tracker>();
	
	public synchronized Tracker getTracker(TrackerName trackerId) {
		
		//as of now, only one tracker is being used, add if not present already
		
		if (!mTrackers.containsKey(trackerId)) {
			GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
			Tracker t = analytics.newTracker(R.xml.googleanalytics);
			mTrackers.put(trackerId, t);
		}
		return mTrackers.get(trackerId);
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		JavelinClient javelin = JavelinClient.getInstance(this, JAVELIN_CONFIG);
		JavelinUserManager userManager = javelin.getUserManager();
		
		//check in with GCM and report if necessary
		userManager.enableGCM();
		
		//refresh current agency data
		userManager.refreshCurrentAgency();
		
		registerListeners();
	}
	
	private void registerListeners() {
		JavelinClient javelin = JavelinClient.getInstance(this, JAVELIN_CONFIG);
		
		javelin.getAlertManager().setAlertListener(new AlertListener() {
			
			@Override
			public void onConnecting() {
				Notifier.getInstance(TapShieldApplication.this).notify(Notifier.NOTIFICATION_CONNECTING);
			}
			
			@Override
			public void onCompleted() {
				Notifier.getInstance(TapShieldApplication.this).notify(Notifier.NOTIFICATION_COMPLETED);
			}
			
			@Override
			public void onCancel() {
				Notifier.getInstance(TapShieldApplication.this).dismissAlertRelated();
			}
			
			@Override
			public void onBackEndNotified() {
				Notifier.getInstance(TapShieldApplication.this).notify(Notifier.NOTIFICATION_ESTABLISHED);
			}
		});
		
		javelin.getChatManager().setNewIncomingMessagesListener(new OnNewIncomingChatMessagesListener() {
			
			@Override
			public void onNewIncomingChatMessages(List<String> incomingMessages) {
				Notifier notifier = Notifier.getInstance(TapShieldApplication.this);
				
				if (incomingMessages == null) {
					notifier.dismiss(Notifier.NOTIFICATION_CHAT);
				} else {
					notifier.notifyChat(incomingMessages);
				}
			}
		});
		
		javelin.getMassAlertManager().setOnNewMassAlertListener(new OnNewMassAlertListener() {
			
			@Override
			public void onNewMassAlert() {
				Notifier.getInstance(TapShieldApplication.this).notify(Notifier.NOTIFICATION_MASS);
			}
		});
	}
}
