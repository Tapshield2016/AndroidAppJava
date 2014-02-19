package com.tapshield.android.app;

import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinConfig;
import com.tapshield.android.api.JavelinUserManager;

import android.app.Application;

public class TapShieldApplication extends Application {

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
	}
}
