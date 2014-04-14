package com.tapshield.android.manager;

import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.support.v4.app.NotificationCompat;

import com.tapshield.android.R;

public class Notifier {

	public static final int NOTIFICATION_YANK = 10;
	public static final int NOTIFICATION_CONNECTING = 20;
	public static final int NOTIFICATION_ESTABLISHED = 30;
	public static final int NOTIFICATION_FAILED_ALERT = 40;
	public static final int NOTIFICATION_COMPLETED = 50;
	public static final int NOTIFICATION_CHAT = 60;
	public static final int NOTIFICATION_MASS = 70;
	public static final int NOTIFICATION_TWILIO_FAILURE = 80;
	
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
				.setContentTitle("TapShield")
				.setSmallIcon(R.drawable.ic_stat)
				.setDefaults(Notification.DEFAULT_ALL);
	}

	private Notification buildYank() {
		return getCommonBuilder()
				.setContentTitle("TapShield")
				.setContentText("Yank enabled")
				.setOngoing(true)
				.setAutoCancel(false)
				.build();
	}
	
	private Notification buildConnecting() {
		return getCommonBuilder()
				.setContentText("Connecting...")
				.setAutoCancel(false)
				.setOngoing(true)
				.build();
	}
	
	private Notification buildEstablished() {
		return getCommonBuilder()
				.setContentText("Established")
				.setAutoCancel(false)
				.setOngoing(true)
				.setOnlyAlertOnce(true)
				.build();
	}
	
	private Notification buildCompleted() {
		return getCommonBuilder()
				.setContentText("Completed by dispatcher")
				.setAutoCancel(false)
				.setOngoing(true)
				.build();
	}
	
	private Notification buildFailedAlert() {
		return getCommonBuilder()
				.setContentText("Server unreachable")
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
		String title = "TapShield Messages";
		String content = count == 1 ? count + " message." : count + " messages.";

		NotificationCompat.Builder builder = getCommonBuilder()
				.setPriority(NotificationCompat.PRIORITY_MAX)
				.setContentTitle(title)
				.setContentText(content)
				.setAutoCancel(true);

		//set a preview of the single message (if just one)
		if (count == 1) {
			content = "\"" + chatMessages.get(0) + "\"";
			builder.setContentText(content);
		}

		//inbox style settings if version supports it
		if (count > 1) {
			NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
			String styleTitle = "TapShield Messages (" + count + ")";

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
				.setContentText("New mass alert")
				.setAutoCancel(true)
				.build();
	}
	
	private Notification buildTwilioFailure() {
		String text = "call failed. tap for details.";
		String ticker = "tapshield: call failed.";
		
		return getCommonBuilder()
				.setContentText(text)
				.setTicker(ticker)
				.setOngoing(true)
				.setPriority(NotificationCompat.PRIORITY_MAX)
				.build();
	}

	public void notify(int notificationId) {
		notify(notificationId, null);
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
