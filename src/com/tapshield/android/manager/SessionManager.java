package com.tapshield.android.manager;

import java.util.List;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

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
import com.tapshield.android.ui.activity.MainActivity;
import com.tapshield.android.ui.activity.SetOrganizationActivity;
import com.tapshield.android.ui.activity.VerifyPhoneActivity;
import com.tapshield.android.ui.dialog.SetPasscodeDialog;
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
	private SetPasscodeDialog mSetPasscodeDialog;
	
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
			
			Intent parent = new Intent(mContext, MainActivity.class);
			parent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			Intent pick = new Intent(mContext, SetOrganizationActivity.class);
			
			if (num == 1) {
				String serialized = Agency.serializeToString(agencies.get(0));
				pick.putExtra(SetOrganizationActivity.EXTRA_SET_ORGANIZATION, serialized);
			}
			
			TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext)
					.addNextIntent(parent)
					.addNextIntent(pick);
			
			PendingIntent pendingIntent = stackBuilder.getPendingIntent(1,
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
	
	public void check(final Activity activity) {
		final JavelinUserManager userManager = mJavelin.getUserManager();
		
		if (userManager == null || !userManager.isPresent()) {
			return;
		}
		
		final User user = userManager.getUser();
		
		//request passcode to be set; finish if they fail to provide it
		if (!user.hasDisarmCode()) {
			mSetPasscodeDialog = new SetPasscodeDialog();
			mSetPasscodeDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

				@Override
				public void onCancel(DialogInterface dialog) {
					if (!user.hasDisarmCode()) {
						activity.finish();
					}
				}
			});
			mSetPasscodeDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {

				@Override
				public void onDismiss(DialogInterface dialog) {
					if (user.hasDisarmCode()) {
						//make a check once passcode/disarm code has been set
						check(activity);
					}
				}
			});

			if (!mSetPasscodeDialog.isVisible()) {
				mSetPasscodeDialog.show(activity.getFragmentManager(),
						SetPasscodeDialog.class.getSimpleName());
			}

			return;
		}
		
		//no set org, background check for nearby ones, otherwise, check for missing required info
		
		if (!user.belongsToAgency() && !userManager.hasTemporaryAgency()) {
			
			//if key is set to false, do not check--default is true for sporadic checks
			if (!mPreferences.getBoolean(PREFERENCES_KEY_CHECK, true)) {
				return;
			}
			
			mGotLocation = false;
			mTracker.start();
			mTracker.addLocationListener(this);
			return;
		}
		
		if (areUserEmailsNotSupportingOrg()) {
			
			Bundle extras = null;
			
			String unverifiedEmail = getUnverifiedRequirementMatchingEmail(userManager);
			if (unverifiedEmail != null) {
				extras = new Bundle();
				extras.putString(AddEmailActivity.EXTRA_UNVERIFIED_EMAIL, unverifiedEmail);
			}
			
			Intent addEmail = new Intent(activity, AddEmailActivity.class);
			activity.startActivity(addEmail);
			return;
		}
		
		if ((user.belongsToAgency() || userManager.hasTemporaryAgency())
				&& !user.isPhoneNumberVerified()) {
			//checking activity must be finished so if phone verif step is skipped, can be finished
			// without returning and prompting the user to the same step into a loop
			UiUtils.startActivityNoStack(activity, VerifyPhoneActivity.class);
			activity.finish();
			return;
		}

		//at this point a temporary organization is not used anymore, but has to be set as the main org
		// since everything required has been given
		if (userManager.hasTemporaryAgency()) {
			final String message = "You just joined " + userManager.getTemporaryAgency().name + "!";
			UiUtils.toastLong(mContext, message);
			userManager.setTemporaryAgencyAsMain();
		}
		
		//by getting past the conditional statements, we are past required info, update remote user
		userManager.updateRequiredInformation(null);
	}
	
	public void setSporadicChecks(boolean enableSporadicChecks) {
		SharedPreferences.Editor editor = mPreferences.edit();
		editor.putBoolean(PREFERENCES_KEY_CHECK, enableSporadicChecks);
		editor.commit();
	}
	
	public boolean areUserEmailsNotSupportingOrg() {
		return mJavelin.getUserManager().hasTemporaryAgency()
				&& !isRequiredDomainSupported(mJavelin.getUserManager());
	}
	
	private boolean isRequiredDomainSupported(final JavelinUserManager userManager) {

		final Agency tempAgency = userManager.getTemporaryAgency();
		
		if (!tempAgency.requiredDomainEmails) {
			return true;
		}
		
		final String domain = tempAgency.domain;
		
		for (Email email : userManager.getUser().allEmails.getList()) {
			if (email.get().endsWith(domain) && email.isActive()) {
				return true;
			}
		}
		
		return false;
	}
	
	private String getUnverifiedRequirementMatchingEmail(final JavelinUserManager userManager) {
		final String domain = userManager.getTemporaryAgency().domain;
		
		for (Email email : userManager.getUser().allEmails.getList()) {
			if (email.get().endsWith(domain) && !email.isActive()) {
				return email.get();
			}
		}
		
		return null;
	}
}
