package com.tapshield.android.app;

import java.util.List;

import com.tapshield.android.api.JavelinAlertManager.AlertListener;
import com.tapshield.android.api.JavelinChatManager.OnNewIncomingChatMessagesListener;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinConfig;
import com.tapshield.android.api.JavelinMassAlertManager.OnNewMassAlertListener;
import com.tapshield.android.api.googleplaces.GooglePlacesConfig;
import com.tapshield.android.api.spotcrime.SpotCrimeConfig;
import com.tapshield.android.api.JavelinUserManager;
import com.tapshield.android.manager.Notifier;

import android.app.Application;

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
			.key("2be4edd6ebd10379d1a1eb6600747726654fc81645ecae386a9a9a440329")
			.build();
	
	public static GooglePlacesConfig GOOGLEPLACES_CONFIG =
			new GooglePlacesConfig.Builder()
			.key("AIzaSyA5m917LQ6E-9V2tEXhLRl4nhEtbY01ny4")
			.build();
	
	//public static boolean IN_APP_EMERGENCY_DIALOG_SHOWED = false;
	
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
