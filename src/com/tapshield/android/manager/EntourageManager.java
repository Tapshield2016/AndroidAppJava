package com.tapshield.android.manager;

import java.util.ArrayList;
import java.util.List;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.location.Location;
import android.os.SystemClock;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinEntourageManager;
import com.tapshield.android.api.JavelinEntourageManager.EntourageListener;
import com.tapshield.android.api.googledirections.model.Route;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.service.EntourageArrivalCheckService;
import com.tapshield.android.ui.activity.AlertActivity;
import com.tapshield.android.ui.activity.MainActivity;
import com.tapshield.android.utils.ContactsRetriever.Contact;


public class EntourageManager implements EntourageListener {

	public static final String ACTION_ENTOURAGE_ARRIVAL_CHECK = "com.tapshield.android.action.ENTOURAGE_ARRIVAL_CHECK";
	
	private static final String PREFERENCES = "com.tapshield.android.preferences.entourage";
	private static final String KEY_SET = "com.tapshield.android.preferences.key.set";
	private static final String KEY_ROUTE = "com.tapshield.android.preferences.key.route";
	private static final String KEY_START = "com.tapshield.android.preferences.key.startat";
	private static final String KEY_DURATION = "com.tapshield.android.preferences.key.duration";
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
	private long mDuration;
	private boolean mSet;
	private int mMembersAdditionIndex = 0;
	private int mMemberAdditionRetry = 0;
	private int mMemberDeletionIndex = 0;
	private List<Listener> mListeners;
	private List<Integer> mMembersToDelete;
	
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
		mListeners = new ArrayList<Listener>();
		mMembersToDelete = new ArrayList<Integer>();
		load();
	}
	
	private void load() {
		
		Gson gson = new Gson();
		
		mSet = mPreferences.getBoolean(KEY_SET, false);
		
		if (mSet) {
			mRoute = gson.fromJson(mPreferences.getString(KEY_ROUTE, null), Route.class);
			//load startAt here, setFlags() method will set the rest with this and the route
			mStartAt = mPreferences.getLong(KEY_START, 0);
			mDuration = mPreferences.getLong(KEY_DURATION, 0);
			setFlags(mRoute);
		}
		
		if (mPreferences.contains(KEY_MEMBERS)) {
			mMembers = gson.fromJson(mPreferences.getString(KEY_MEMBERS, null),
					EntourageMembers.class);
		} else {
			mMembers = new EntourageMembers();
		}
	}
	
	private void save() {
		SharedPreferences.Editor editor = mPreferences.edit();
		Gson gson = new Gson();
		editor.putBoolean(KEY_SET, mSet);
		editor.putString(KEY_ROUTE, gson.toJson(mRoute));
		editor.putLong(KEY_START, mStartAt);
		editor.putLong(KEY_DURATION, mDuration);
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
		Intent receiver = new Intent(ACTION_ENTOURAGE_ARRIVAL_CHECK);
		PendingIntent operation = PendingIntent.getBroadcast(mContext, 1, receiver, 0);
		return operation;
	}
	
	public void start(Route r, Contact... contacts) {
		start(r, -1, contacts);
	}
	
	public void start(Route r, long wantedDurationSeconds, Contact... contacts) {
		if (isSet()) {
			return;
		}
		
		Log.i("aaa", "start r.name=" + r.destinationName());
		
		long durationSeconds =
				wantedDurationSeconds >= 0 ? wantedDurationSeconds : r.durationSeconds();

		mMembersToDelete.clear();
		addMembers(contacts);
		//preset startAt here, setFlags() method will set the rest with this and the route
		mStartAt = System.currentTimeMillis();
		setFlags(r);
		mDuration = durationSeconds * 1000;
		long extraBuffer = (long) (mDuration * ARRIVE_BUFFER_FACTOR);
		scheduleAlertIn(mDuration + extraBuffer);
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

		removeMembers();
		mSet = false;
		mRoute = null;
		mStartAt = 0;
		unscheduledAlert();
		save();
	}
	
	public boolean isSet() {
		return mSet;
	}
	
	public long getStartAt() {
		return isSet() ? mStartAt : -1;
	}
	
	public long getDurationMilli() {
		return isSet() ? mDuration : -1;
	}
	
	public Route getRoute() {
		return mRoute;
	}
	
	public void drawOnMap(GoogleMap m) {
		Route r = getRoute();
		
		MarkerOptions destination = new MarkerOptions()
				.icon(BitmapDescriptorFactory.fromResource(R.drawable.ts_icon_entourage_destination))
				.anchor(0.5f, 1.0f)
				.position(new LatLng(r.endLat(), r.endLon()));

		Resources res = mContext.getResources();
		
		float routeWidth = (float) res.getInteger(R.integer.ts_entourage_route_width);
		PolylineOptions route = new PolylineOptions()
				.color(res.getColor(R.color.ts_brand_light))
				.width(routeWidth);
		
		for (Location l : r.decodedOverviewPolyline()) {
			route.add(new LatLng(l.getLatitude(), l.getLongitude()));
		}
		
		m.addMarker(destination);
		m.addPolyline(route);
		
		LatLngBounds bounds = new LatLngBounds(
				new LatLng(r.boundsSwLat(), r.boundsSwLon()),
				new LatLng(r.boundsNeLat(), r.boundsNeLon()));
		CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 200);
		m.moveCamera(cameraUpdate);
	}
	
	public void notifyReceiverTriggered(Intent intent) {
		//start service to notify via Entourage API and start an alert
		
		Intent entourageArrivalCheckService = new Intent(mContext, EntourageArrivalCheckService.class);
		mContext.startService(entourageArrivalCheckService);
	}
	
	public void notifyUserMissedETA() {
		
		Log.i("aaa", "notifyUserMissedETA");
		
		EmergencyManager manager = EmergencyManager.getInstance(mContext);
		manager.startNow(EmergencyManager.TYPE_START_REQUESTED);
		
		
		Intent home = new Intent(mContext, MainActivity.class)
				.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		
		Intent alert = new Intent(mContext, AlertActivity.class)
				.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		Intent[] stack = new Intent[] {home, alert};
		
		mContext.startActivities(stack);
	}
	
	public void setMembers(Contact... contacts) {}
	
	public void addMembers(Contact... contacts) {
		
		if (contacts == null || contacts.length == 0) {
			return;
		}
		
		mMembers.members().clear();
		
		for (Contact c : contacts) {
			EntourageMember m = new EntourageMember();
			m.name(c.name());
			
			if (!c.email().isEmpty()) {
				m.email(c.email().get(0));
			}
			
			if (!c.phone().isEmpty()) {
				m.phone(c.phone().get(0));
			}
			
			mMembers.members.add(m);
		}
		
		mMembersAdditionIndex = 0;
		addMemberViaJavelin();
	}
	
	private void removeMembers() {
		mMemberDeletionIndex = 0;
		removeMemberViaJavelin();
	}
	
	private void removeMemberViaJavelin() {
		int id = mMembersToDelete.get(mMemberDeletionIndex);
		mEntourage.removeMemberWithId(id, this);
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
			mMembersToDelete.add(memberId);
			
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
	public void onMemberRemoved(boolean ok, int memberId, String errorIfNotOk) {
		if (ok) {
			boolean stillLeft = mMemberDeletionIndex + 1 < mMembersToDelete.size();
			if (stillLeft) {
				mMemberDeletionIndex++;
				removeMemberViaJavelin();
			} else {
				mMembersToDelete.clear();
			}
		} else {
			removeMemberViaJavelin();
		}
	}

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
		
		Log.i("aaa", "settemp r.name=" + r.destinationName());
		
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
