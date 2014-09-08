package com.tapshield.android.ui.activity;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import nl.matshofman.saxrssreader.RssItem;
import nl.matshofman.saxrssreader.RssReader;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import android.app.ActionBar;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinMassAlertManager;
import com.tapshield.android.api.JavelinMassAlertManager.OnMassAlertUpdateListener;
import com.tapshield.android.api.JavelinUserManager;
import com.tapshield.android.api.model.MassAlert;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.manager.EmergencyManager;
import com.tapshield.android.manager.Notifier;
import com.tapshield.android.ui.adapter.MassAlertAdapter;
import com.tapshield.android.utils.UiUtils;

public class MassAlertsActivity extends BaseFragmentActivity implements OnMassAlertUpdateListener {

	private JavelinClient mJavelin;
	private JavelinMassAlertManager mManager;
	private View mEmpty;
	private ListView mList;
	private MassAlertAdapter mAdapter;
	private List<MassAlert> mItemsRss;
	private List<MassAlert> mItemsMass;
	private List<MassAlert> mItemsAll;
	private Runnable mUpdater;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_massalerts);

		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		
		mItemsRss = new ArrayList<MassAlert>();
		mItemsMass = new ArrayList<MassAlert>();
		mItemsAll = new ArrayList<MassAlert>();
		
		mJavelin = JavelinClient.getInstance(MassAlertsActivity.this,
				TapShieldApplication.JAVELIN_CONFIG);
		mManager = mJavelin.getMassAlertManager();
		
		mEmpty = findViewById(R.id.massalerts_empty);
		mList = (ListView) findViewById(R.id.massalerts_list);
		//pass null list since it will be defaulted to an empty one until manager delivers
		mAdapter = new MassAlertAdapter(MassAlertsActivity.this,
				R.layout.item_mass_alert,
				R.id.item_mass_alert_text_message,
				R.id.item_mass_alert_text_datetime,
				null);
		mList.setAdapter(mAdapter);
		
		mUpdater = new Runnable() {
			
			@Override
			public void run() {
				mAdapter.notifyDataSetChanged();
				//isEmpty() was not overridden, use getCount() instead to check for [non]empty dataset
				mEmpty.setVisibility(mAdapter.getCount() == 0 ? View.VISIBLE : View.GONE);
			}
		};
		
		fetchRssFeedIfAvailable();
	}
	
	@Override
	protected void onResume() {
		Notifier.getInstance(this).dismiss(Notifier.NOTIFICATION_MASS);
		mManager.addOnMassAlertUpdateListener(this);
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		Notifier.getInstance(this).dismiss(Notifier.NOTIFICATION_MASS);
		mManager.removeOnMassAlertUpdateListener(this);
		super.onPause();
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			return true;
		}
		
		return false;
	}
	
	@Override
	public void onBackPressed() {
		EmergencyManager emergencyManager = EmergencyManager.getInstance(this);
		UiUtils.startActivityNoStack(this,
				emergencyManager.isRunning() ? AlertActivity.class : MainActivity.class);
	}

	@Override
	public void onMassAlertUpdate(List<MassAlert> allMassAlerts) {
		mItemsMass.clear();
		mItemsMass.addAll(allMassAlerts);
		updateList();
	}
	
	private void updateList() {
		mItemsAll.clear();
		mItemsAll.addAll(mItemsRss);
		mItemsAll.addAll(mItemsMass);
		mAdapter.setItemsNoNotifyDataSetChanged(mItemsAll);
		mList.post(mUpdater);
	}
	
	private void fetchRssFeedIfAvailable() {
		JavelinUserManager userManager = mJavelin.getUserManager();
		
		if (!userManager.isPresent()
				|| !userManager.getUser().belongsToAgency()
				|| userManager.getUser().agency.rssUrl == null
				|| userManager.getUser().agency.rssUrl.isEmpty()) {
			return;
		}
		
		final String rssUrl = userManager.getUser().agency.rssUrl;
		new MassAlertRssFeedReaderAsync().execute(rssUrl);
	}
	
	private class MassAlertRssFeedReaderAsync extends RssFeedReaderAsync {
		
		@Override
		protected void onPreExecute() {
			//start spinning progress off action bar
		}
		
		@Override
		protected void onPostExecute(List<RssItem> result) {
			
			//after letting parent class (RssFeedReaderAsync) deal with the feed, build MassAlert items
			
			mItemsRss.clear();
			
			Log.i("ts:mass", "RssItem count=" + result.size());
			for (RssItem rssItem : result) {
				
				//java Date to jodatime DateTime
				DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
				String date = new DateTime(rssItem.getPubDate()).toString(dtf);
				
				Log.i("ts:mass", String.format("url=%s date=%s org=%s des=%s", rssItem.getLink(), date, rssItem.getTitle(), rssItem.getDescription()));
				
				MassAlert item = new MassAlert(
						rssItem.getLink(),
						date,
						mJavelin.getUserManager().getUser().agency.name,
						rssItem.getDescription());
				
				mItemsRss.add(item);
			}
			
			updateList();
			//stop spinning progress off action bar
		}
	}
	
	private class RssFeedReaderAsync extends AsyncTask<String, Void, List<RssItem>> {

		@Override
		protected List<RssItem> doInBackground(String... params) {
			List<RssItem> result = null;
			try {
				result = RssReader.read(new URL(params[0])).getRssItems();
			} catch (Exception e) {
				result = null;
			}
			
			return result;
		}
	}
}
