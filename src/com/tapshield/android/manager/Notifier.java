package com.tapshield.android.manager;

import java.util.List;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.BigTextStyle;
import android.support.v4.app.TaskStackBuilder;

import com.tapshield.android.R;
import com.tapshield.android.api.JavelinSocialReportingManager;
import com.tapshield.android.ui.activity.AlertActivity;
import com.tapshield.android.ui.activity.ChatActivity;
import com.tapshield.android.ui.activity.MainActivity;
import com.tapshield.android.ui.activity.MassAlertsActivity;

public class Notifier {

	public static final int NOTIFICATION_YANK = 10;
	public static final int NOTIFICATION_CONNECTING = 20;
	public static final int NOTIFICATION_ESTABLISHED = 30;
	public static final int NOTIFICATION_FAILED_ALERT = 40;
	public static final int NOTIFICATION_COMPLETED = 50;
	public static final int NOTIFICATION_CHAT = 60;
	public static final int NOTIFICATION_MASS = 70;
	public static final int NOTIFICATION_TWILIO_FAILURE = 80;
	public static final int NOTIFICATION_CRIME_REPORT = 90;
	
	private static Notifier mInstance;
	private Context mContext;
	private NotificationManager mNotificationManager;
	
	public static Notifier getInstance(Context context) {
		if (mInstance == null) {
			mInstance = new Notifier(context);
		}
		return mInstance;
	}
	
	private Notifier(Context context) {
		mContext = context.getApplicationContext();
	}
	
	private void getManager() {
		if (mNotificationManager == null) {
			mNotificationManager = (NotificationManager)
					mContext.getSystemService(Context.NOTIFICATION_SERVICE);
		}
	}
	
	//create a builder with common settings for all notifications (name, icon, etc.)
	private NotificationCompat.Builder getCommonBuilder() {
		return new NotificationCompat.Builder(mContext)
				.setContentTitle(mContext.getString(R.string.ts_notification_title))
				.setSmallIcon(R.drawable.ic_stat)
				.setDefaults(Notification.DEFAULT_ALL);
	}

	private Notification buildYank() {
		return getCommonBuilder()
				.setContentText(mContext.getString(R.string.ts_notification_message_yank))
				.setContentIntent(getPendingIntentWithBackStack(MainActivity.class))
				.setOngoing(true)
				.setAutoCancel(false)
				.build();
	}
	
	private Notification buildConnecting() {
		return getCommonBuilder()
				.setContentText(mContext.getString(R.string.ts_notification_message_alert_connecting))
				.setContentIntent(getPendingIntentWithBackStack(MainActivity.class))
				.setAutoCancel(false)
				.setOngoing(true)
				.build();
	}
	
	private Notification buildEstablished() {
		return getCommonBuilder()
				.setContentText(mContext.getString(R.string.ts_notification_message_alert_established))
				.setContentIntent(getPendingIntentWithBackStack(MainActivity.class))
				.setAutoCancel(false)
				.setOngoing(true)
				.setOnlyAlertOnce(true)
				.build();
	}
	
	private Notification buildCompleted() {
		return getCommonBuilder()
				.setContentText(mContext.getString(R.string.ts_notification_message_alert_completed))
				.setAutoCancel(true)
				.setOngoing(false)
				.build();
	}
	
	private Notification buildFailedAlert() {
		return getCommonBuilder()
				.setContentText(mContext.getString(R.string.ts_notification_message_alert_failed))
				.setContentIntent(getPendingIntentWithBackStack(MainActivity.class))
				.setAutoCancel(true)
				.build();
	}
	
	private Notification buildChat(List<String> chatMessages) {
		if (chatMessages == null) {
			return null;
		}

		int count = chatMessages.size();

		if (count == 0) {
			return null;
		}

		//override default builder settings
		String title = mContext.getString(R.string.ts_notification_title_chat);
		String content = count == 1 ? count + " message." : count + " messages.";

		Intent chat = new Intent(mContext, ChatActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 1, chat,
				PendingIntent.FLAG_UPDATE_CURRENT);
		
		NotificationCompat.Builder builder = getCommonBuilder()
				.setPriority(NotificationCompat.PRIORITY_MAX)
				.setContentTitle(title)
				.setContentText(content)
				//.setContentIntent(getPendingIntentWithBackStack(MainActivity.class, AlertActivity.class, ChatActivity.class))
				.setContentIntent(pendingIntent)
				.setAutoCancel(true);

		//set a preview of the single message (if just one)
		if (count == 1) {
			content = "\"" + chatMessages.get(0) + "\"";
			builder.setContentText(content);
		}

		//inbox style settings if version supports it
		if (count > 1) {
			NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
			String styleTitle = title + " (" + count + ")";

			for (String message : chatMessages) {
				inboxStyle.addLine("\"" + message + "\"");
			}

			inboxStyle.setBigContentTitle(styleTitle);
			builder.setStyle(inboxStyle);
		}

		return builder.build();
	}
	
	private Notification buildMass() {
		return getCommonBuilder()
				.setContentText(mContext.getString(R.string.ts_notification_message_massalert))
				.setContentIntent(getPendingIntentWithBackStack(MainActivity.class, MassAlertsActivity.class))
				.setAutoCancel(true)
				.build();
	}
	
	private Notification buildTwilioFailure() {
		String text = mContext.getString(R.string.ts_notification_message_twilio_failed);
		String ticker = mContext.getString(R.string.ts_notification_ticker_twilio_failed);
		
		return getCommonBuilder()
				.setContentText(text)
				.setTicker(ticker)
				.setContentIntent(getPendingIntentWithBackStack(
						MainActivity.class, AlertActivity.class))
				.setOngoing(true)
				.setPriority(NotificationCompat.PRIORITY_MAX)
				.build();
	}
	
	private Notification buildCrimeReport(final String message, final String id, final Bundle extras) {
		
		String title = extras.getString(JavelinSocialReportingManager.KEY_PUSHMESSAGE_TITLE,
				mContext.getString(R.string.app_name));
		
		return getCommonBuilder()
				.setContentTitle(title)
				.setContentText(message)
				.setPriority(NotificationCompat.PRIORITY_DEFAULT)
				.setAutoCancel(true)
				.setStyle(new BigTextStyle()
						.bigText(message))
				.build();
	}
	
	private PendingIntent getPendingIntentWithBackStack(Class<? extends Activity>... activitiesClasses) {
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);
		
		for (Class clss : activitiesClasses) {
			Intent intent = new Intent(mContext, clss);
			stackBuilder.addNextIntent(intent);
		}
		
		return stackBuilder.getPendingIntent(1, PendingIntent.FLAG_UPDATE_CURRENT);
	}
	
	public void notify(int notificationId) {
		notify(notificationId, null);
	}
	
	public void notifyCrimeReport(final String message, final String id, final Bundle extras) {
		getManager();
		Notification notification = buildCrimeReport(message, id, extras);
		if (notification != null) {
			mNotificationManager.notify(NOTIFICATION_CRIME_REPORT, notification);
		}
	}
	
	public void notifyChat(List<String> chatMessages) {
		notify(NOTIFICATION_CHAT, chatMessages);
	}
	
	private void notify(int notificationId, List<String> chatMessages) {
		getManager();

		Notification notification = null;
		
		switch (notificationId) {
		case NOTIFICATION_YANK:
			dismissAlertRelated();
			notification = buildYank();
			break;
		case NOTIFICATION_CONNECTING:
			dismissAlertRelated();
			notification = buildConnecting();
			break;
		case NOTIFICATION_ESTABLISHED:
			dismissAlertRelated();
			notification = buildEstablished();
			break;
		case NOTIFICATION_FAILED_ALERT:
			dismissAlertRelated();
			notification = buildFailedAlert();
			break;
		case NOTIFICATION_COMPLETED:
			dismissAlertRelated();
			notification = buildCompleted();
			break;
		case NOTIFICATION_CHAT:
			notification = buildChat(chatMessages);
			break;
		case NOTIFICATION_MASS:
			notification = buildMass();
			break;
		case NOTIFICATION_TWILIO_FAILURE:
			notification = buildTwilioFailure();
			break;
		}
		
		if (notification != null) {
			mNotificationManager.notify(notificationId, notification);
		}
	}
	
	public void dismiss(int notificationId) {
		getManager();
		mNotificationManager.cancel(notificationId);
	}
	
	//all notifications are shown one at a time
	// 1. yank if headsets unplugged
	// 2. yank dismissed if connecting
	// 3. yank and connecting dismissed if established
	// all alert-related 'notify*' methods call this one to ensure that automatically
	public void dismissAlertRelated() {
		getManager();
		mNotificationManager.cancel(NOTIFICATION_YANK);
		mNotificationManager.cancel(NOTIFICATION_CONNECTING);
		mNotificationManager.cancel(NOTIFICATION_ESTABLISHED);
		mNotificationManager.cancel(NOTIFICATION_FAILED_ALERT);
	}
}
