package com.tapshield.android.manager;

import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.location.LocationListener;
import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinClient.OnAgenciesFetchListener;
import com.tapshield.android.api.JavelinUserManager;
import com.tapshield.android.api.model.Agency;
import com.tapshield.android.api.model.User;
import com.tapshield.android.api.model.User.Email;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.location.LocationTracker;
import com.tapshield.android.ui.activity.AddEmailActivity;
import com.tapshield.android.ui.activity.SetOrganizationActivity;
import com.tapshield.android.ui.activity.VerifyPhoneActivity;
import com.tapshield.android.utils.UiUtils;

public class SessionManager implements LocationListener, OnAgenciesFetchListener {

	private static final int NOTIFICATION_ID = 1010;
	private static final String PREFERENCES_KEY_CHECK = "com.tapshield.android.preferences.key_sessionmanager_check";
	
	private static SessionManager mInstance;
	
	private Context mContext;
	private JavelinClient mJavelin;
	private LocationTracker mTracker;
	private NotificationManager mNotificationManager;
	private SharedPreferences mPreferences;
	
	private boolean mGotLocation;

	private SessionManager(Context context) {
		mContext = context.getApplicationContext();
		mJavelin = JavelinClient.getInstance(mContext, TapShieldApplication.JAVELIN_CONFIG);
		mTracker = LocationTracker.getInstance(mContext);
		mNotificationManager = (NotificationManager) mContext
				.getSystemService(Context.NOTIFICATION_SERVICE);
		mPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
	}
	
	public static SessionManager getInstance(Context context) {
		if (mInstance == null) {
			mInstance = new SessionManager(context);
		}
		return mInstance;
	}

	@Override
	public void onLocationChanged(Location location) {
		if (mGotLocation) {
			return;
		}
		
		mGotLocation = true;
		mTracker.stop();
		mTracker.removeLocationListener(this);
		
		mJavelin.fetchAgenciesNearby(location.getLatitude(), location.getLongitude(), 10f, this);
	}

	@Override
	public void onAgenciesFetch(boolean successful, List<Agency> agencies, Throwable exception) {
		if (successful && !agencies.isEmpty()) {
			final int num = agencies.size();
			String title = "Organization(s) Nearby";
			String messagePrefix = num == 1 ? agencies.get(0).name + " is" : Integer.toString(num);
			String messageSuffix = " nearby. Tap to " + (num == 1 ? "join" : "pick") + ".";
			String message = messagePrefix + messageSuffix;
			
			Intent intent = new Intent(mContext, SetOrganizationActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			
			if (num == 1) {
				String serialized = Agency.serializeToString(agencies.get(0));
				intent.putExtra(SetOrganizationActivity.EXTRA_SET_ORGANIZATION, serialized);
			}
			
			PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 1, intent,
					PendingIntent.FLAG_UPDATE_CURRENT);
			
			Notification notification = new NotificationCompat.Builder(mContext)
					.setContentTitle(title)
					.setContentText(message)
					.setAutoCancel(true)
					.setDefaults(Notification.DEFAULT_ALL)
					.setSmallIcon(R.drawable.ic_stat)
					.setContentIntent(pendingIntent)
					.build();
			
			mNotificationManager.notify(NOTIFICATION_ID, notification);
		}
	}
	
	public void check(final Context context) {
		JavelinUserManager userManager = mJavelin.getUserManager();
		
		if (userManager == null || !userManager.isPresent()) {
			return;
		}
		
		User user = userManager.getUser();
		
		//no set org, background check for nearby ones, otherwise, check for missing required info
		
		if (!user.belongsToAgency()) {
			
			//if key is set to false, do not check--default is true for sporadic checks
			if (!mPreferences.getBoolean(PREFERENCES_KEY_CHECK, true)) {
				return;
			}
			
			mGotLocation = false;
			mTracker.start();
			mTracker.addLocationListener(this);
			return;
		}
		
		if (!isRequiredDomainSupported(user)) {
			
			Bundle extras = null;
			
			String unverifiedEmail = getUnverifiedRequirementMatchingEmail(user);
			if (unverifiedEmail != null) {
				extras = new Bundle();
				extras.putString(AddEmailActivity.EXTRA_UNVERIFIED_EMAIL, unverifiedEmail);
			}
			
			UiUtils.startActivityNoStack(context, AddEmailActivity.class, extras);
			return;
		}
		
		if (user.belongsToAgency() && !user.isPhoneNumberVerified()) {
			UiUtils.startActivityNoStack(context, VerifyPhoneActivity.class);
			return;
		}
		
		//by getting past the conditional statements, we are past required info, update remote user
		updateRemoteUser();
	}
	
	public void setSporadicChecks(boolean enableSporadicChecks) {
		SharedPreferences.Editor editor = mPreferences.edit();
		editor.putBoolean(PREFERENCES_KEY_CHECK, enableSporadicChecks);
		editor.commit();
	}
	
	public void updateRemoteUser() {
		mJavelin.getUserManager().updateRequiredInformation(null);
	}
	
	private boolean isRequiredDomainSupported(final User user) {
		if (!user.agency.requiredDomainEmails) {
			return true;
		}
		
		final String domain = user.agency.domain;
		
		for (Email email : user.allEmails.getList()) {
			if (email.get().endsWith(domain) && email.isActive()) {
				return true;
			}
		}
		
		return false;
	}
	
	private String getUnverifiedRequirementMatchingEmail(final User user) {
		final String domain = user.agency.domain;
		
		for (Email email : user.allEmails.getList()) {
			if (email.get().endsWith(domain) && !email.isActive()) {
				return email.get();
			}
		}
		
		return null;
	}
}
