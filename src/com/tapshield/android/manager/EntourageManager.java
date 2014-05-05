package com.tapshield.android.manager;

import java.util.ArrayList;
import java.util.List;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinEntourageManager;
import com.tapshield.android.api.JavelinEntourageManager.EntourageListener;
import com.tapshield.android.api.googledirections.model.Route;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.receiver.EntourageReceiver;
import com.tapshield.android.service.EntourageArrivalCheckService;
import com.tapshield.android.utils.ContactsRetriever.Contact;


public class EntourageManager implements EntourageListener {

	public static final String ACTION_ENTOURAGE_ARRIVAL_CHECK = "com.tapshield.android.action.ENTOURAGE_ARRIVAL_CHECK";
	
	private static final String PREFERENCES = "com.tapshield.android.preferences.entourage";
	private static final String KEY_SET = "com.tapshield.android.preferences.key.set";
	private static final String KEY_ROUTE = "com.tapshield.android.preferences.key.route";
	private static final String KEY_START = "com.tapshield.android.preferences.key.startat";
	private static final String KEY_MEMBERS = "com.tapshield.android.preferences.key.members";
	
	private static final String KEY_ROUTE_TEMP = "com.tapshield.android.preferences.key.route_temp";

	private static final float ARRIVE_BUFFER_FACTOR = 0.0f;
	
	private static EntourageManager mIt = null;
	
	private Context mContext;
	private SharedPreferences mPreferences;
	private JavelinEntourageManager mEntourage;
	private EntourageMembers mMembers;
	private AlarmManager mAlarmManager;
	private Route mRoute;
	private long mStartAt;
	private boolean mSet;
	private int mMembersAdditionIndex = 0;
	private int mMemberAdditionRetry = 0;
	private List<Listener> mListeners;
	
	public static EntourageManager get(Context context) {
		if (mIt == null) {
			mIt = new EntourageManager(context);
		}
		return mIt;
	}
	
	private EntourageManager(Context c) {
		mContext = c.getApplicationContext();
		mEntourage = JavelinClient
				.getInstance(mContext, TapShieldApplication.JAVELIN_CONFIG)
				.getEntourageManager();
		mPreferences = mContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
		mAlarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
		load();
	}
	
	private void load() {
		mSet = mPreferences.getBoolean(KEY_SET, false);
		if (mSet) {
			Gson gson = new Gson();
			mRoute = gson.fromJson(mPreferences.getString(KEY_ROUTE, null), Route.class);
			//load startAt here, setFlags() method will set the rest with this and the route
			mStartAt = mPreferences.getLong(KEY_START, 0);
			setFlags(mRoute);
			
			if (mPreferences.contains(KEY_MEMBERS)) {
				mMembers = gson.fromJson(mPreferences.getString(KEY_MEMBERS, null),
						EntourageMembers.class);
			} else {
				mMembers = new EntourageMembers();
			}
		}
	}
	
	private void save() {
		SharedPreferences.Editor editor = mPreferences.edit();
		Gson gson = new Gson();
		editor.putBoolean(KEY_SET, mSet);
		editor.putString(KEY_ROUTE, gson.toJson(mRoute));
		editor.putLong(KEY_START, mStartAt);
		editor.putString(KEY_MEMBERS, gson.toJson(mMembers));
		editor.apply();
	}
	
	private void scheduleAlertIn(long inMillseconds) {
		mAlarmManager.set(
				AlarmManager.ELAPSED_REALTIME_WAKEUP,
				SystemClock.elapsedRealtime() + inMillseconds,
				getPendingIntent());
	}
	
	private void unscheduledAlert() {
		mAlarmManager.cancel(getPendingIntent());
	}
	
	private PendingIntent getPendingIntent() {
		Intent receiver = new Intent(mContext, EntourageReceiver.class);
		PendingIntent operation = PendingIntent.getBroadcast(mContext, 1, receiver, 0);
		return operation;
	}
	
	public void start(Route r, Contact... contacts) {
		if (isSet()) {
			return;
		}

		addMembers(contacts);
		//preset startAt here, setFlags() method will set the rest with this and the route
		mStartAt = System.currentTimeMillis();
		setFlags(r);
		long durationMilli = mRoute.durationSeconds() * 1000;
		long extraBuffer = (long) (durationMilli * ARRIVE_BUFFER_FACTOR);
		scheduleAlertIn(durationMilli + extraBuffer);
		save();
	}
	
	//keep this flags in this method since it is also called when loading preference-set
	private void setFlags(Route r) {
		mSet = true;
		mRoute = r;
	}
	
	public void stop() {
		if (!isSet()) {
			return;
		}
		
		mSet = false;
		mRoute = null;
		mStartAt = 0;
		unscheduledAlert();
		save();
	}
	
	public boolean isSet() {
		return mSet;
	}
	
	public Route getRoute() {
		return mRoute;
	}
	
	public void drawOnMap(GoogleMap m) {
		//draw and position all entourage-specific elements in the map
	}
	
	public void notifyReceiverTriggered(Intent intent) {
		//start service to notify via Entourage API and start an alert
		
		Intent entourageArrivalCheckService = new Intent(mContext, EntourageArrivalCheckService.class);
		mContext.startService(entourageArrivalCheckService);
		
		int type = intent.getIntExtra(EmergencyManager.EXTRA_TYPE, EmergencyManager.TYPE_START_REQUESTED);
		EmergencyManager manager = EmergencyManager.getInstance(mContext);
		manager.startNow(type);
	}
	
	public void setMembers(Contact... contacts) {}
	
	public void addMembers(Contact... contacts) {
		
		mMembers.members().clear();
		
		for (Contact c : contacts) {
			EntourageMember m = new EntourageMember();
			m.name(c.name());
			m.email(c.email().get(0));
			m.phone(c.phone().get(0));
			
			mMembers.members.add(m);
		}
		
		mMembersAdditionIndex = 0;
		addMemberViaJavelin();
	}
	
	private void addMemberViaJavelin() {
		EntourageMember first = mMembers.members().get(mMembersAdditionIndex);
		
		if (first.phone() != null) {
			mEntourage.addMemberWithPhone(first.name(), first.phone(), this);
		} else {
			mEntourage.addMemberWithEmail(first.name(), first.email(), this);
		}
	}
	
	public void removeMember(Contact contact) {}
	
	public void messageMembers(String message) {
		mEntourage.messageMembers(message, this);
	}

	@Override
	public void onMemberAdded(boolean ok, int memberId, String errorIfNotOk) {
		Log.i("aaa", "ent add ok=" + ok + " m=" + (ok ? memberId : errorIfNotOk)
				+ " " + (mMembersAdditionIndex + 1) + "/" + mMembers.members().size());
		
		if (ok) {
			mMemberAdditionRetry = 0;

			boolean stillLeft = mMembersAdditionIndex + 1 < mMembers.members().size();
			
			if (stillLeft) {
				mMembersAdditionIndex++;
				addMemberViaJavelin();
			}
		} else {
			if (mMemberAdditionRetry < 3) {
				mMemberAdditionRetry++;
				addMemberViaJavelin();
			} else {
				Log.i("aaa", "ent add retries exceeded (" + mMemberAdditionRetry + ")");
			}
		}
	}

	@Override
	public void onMemberRemoved(boolean ok, int memberId, String errorIfNotOk) {}

	@Override
	public void onMessage(boolean ok, String message, String errorIfNotOk) {
		Log.i("aaa", "ent message ok=" + ok + " m=" + (ok ? message : errorIfNotOk));
	}
	
	//keep track of members after:
	//Contact class must have a toJson() and fromJson() methods to serialize into preferences
	//clear contacts on stop()
	
	public boolean hasTemporaryRoute() {
		return mPreferences.contains(KEY_ROUTE_TEMP);
	}
	
	public void setTemporaryRoute(Route r) {
		Gson gson = new Gson();
		
		SharedPreferences.Editor editor = mPreferences.edit();
		editor.putString(KEY_ROUTE_TEMP, gson.toJson(r));
		editor.apply();
	}
	
	public Route getTemporaryRoute() {
		Gson gson = new Gson();
		
		Route r = gson.fromJson(mPreferences.getString(KEY_ROUTE_TEMP, null), Route.class);
		
		SharedPreferences.Editor editor = mPreferences.edit();
		editor.remove(KEY_ROUTE_TEMP);
		editor.apply();
		
		return r;
	}
	
	public void addListener(final Listener l) {
		if (!mListeners.contains(l)) {
			mListeners.add(l);
		}
	}
	
	public void removeListener(final Listener l) {
		if (mListeners.contains(l)) {
			mListeners.remove(l);
		}
	}
	
	public interface Listener {
		void onMessageSent(final boolean ok, final String message, final String errorIfNotOk);
	}
	
	private class EntourageMembers {
		@SerializedName("entourage_members")
		private List<EntourageMember> members;
		
		public EntourageMembers() {
			members = new ArrayList<EntourageMember>();
		}
		
		public List<EntourageMember> members() {
			return members;
		}
	}
	
	private class EntourageMember {
		@SerializedName("url")
		private String mUrl;
		
		@SerializedName("name")
		private String mName;
		
		private String mEmail;
		private String mPhone;
		
		public void url(String url) {
			mUrl = url;
		}
		
		public String url() {
			return mUrl;
		}
		
		public void name(String name) {
			mName = name;
		}
		
		public String name() {
			return mName;
		}
		
		public void email(String email) {
			mEmail = email;
		}
		
		public String email() {
			return mEmail;
		}
		
		public void phone(String phone) {
			mPhone = phone;
		}
		
		public String phone() {
			return mPhone;
		}
	}
}
