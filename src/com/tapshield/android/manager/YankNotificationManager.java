package com.tapshield.android.manager;

import com.tapshield.android.R;
import com.tapshield.android.ui.EmergencyActivity;
import com.tapshield.android.utils.Utils.HardwareUtils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

public class YankNotificationManager {

	private static final int ID = 5;
	private static Context mContext;
	private static NotificationManager mManager;
	private static Notification mNotification;
	
	/**
	 * Show the notification via NotificationManager
	 * @param context Context from which the method is being called.
	 */
	public static void notify(Context context) {
		mContext = context;
		getServiceIfNecessary();
		instantiateIfNecessary();
		
		mManager.notify(ID, mNotification);
	}
	
	/** Dismiss the notification if there via NotificationManager.
	 */
	public static void dismiss() {
		getServiceIfNecessary();
		try {
			mManager.cancel(ID);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void getServiceIfNecessary() {
		if (mManager == null && mContext != null) {
			mManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
		}
	}
	
	private static void instantiateIfNecessary() {
		if (mNotification == null && mContext != null) {
			Uri defaultNotification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
			
			Intent activity = new Intent(mContext, EmergencyActivity.class);
			activity.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			PendingIntent pending = PendingIntent.getActivity(mContext, 0, activity,
					PendingIntent.FLAG_UPDATE_CURRENT);
			
			NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext)
					.setSmallIcon(R.drawable.ic_notify)
					.setContentTitle(mContext.getString(R.string.notification_yank_title))
					.setContentText(mContext.getString(R.string.notification_yank_text))
					.setTicker(mContext.getString(R.string.notification_yank_ticker))
					.setSound(defaultNotification)
					.setContentIntent(pending)
					.setAutoCancel(true);
			mNotification = builder.build();
		}
	}
}
