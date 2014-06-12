package com.tapshield.android.ui.activity;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.Duration;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import android.app.ActionBar;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.google.gson.Gson;
import com.tapshield.android.R;
import com.tapshield.android.api.googledirections.model.Route;
import com.tapshield.android.manager.EntourageManager;
import com.tapshield.android.manager.EntourageManager.SyncStatus;
import com.tapshield.android.ui.adapter.ArrivalContactAdapter;
import com.tapshield.android.ui.dialog.ContactPickerDialogFragment;
import com.tapshield.android.ui.view.CircleSeekBar;
import com.tapshield.android.utils.ContactsRetriever.Contact;
import com.tapshield.android.utils.UiUtils;

public class PickArrivalContacts extends BaseFragmentActivity
		implements OnItemClickListener, OnSeekBarChangeListener, EntourageManager.Listener {

	private EntourageManager mEntourage;
	private GridView mGrid;
	private ArrivalContactAdapter mAdapter;
	private ContactPickerDialogFragment mContactPicker;
	private List<Contact> mChosen = new ArrayList<Contact>();
	
	private CircleSeekBar mEtaKnob;
	private TextView mEtaText;
	private TextView mBusy;
	private long mEta;
	private long mEtaMilli;
	private long mEtaMilliPerStep;
	private Route mRoute;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_pickarrivalcontacts);

		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		
		mEntourage = EntourageManager.get(this);
		mEtaKnob = (CircleSeekBar) findViewById(R.id.pickarrivalcontacts_circleseekbar);
		mEtaText = (TextView) findViewById(R.id.pickarrivalcontacts_text_eta);
		mBusy = (TextView) findViewById(R.id.pickarrivalcontacts_text_busy);
		
		mAdapter = new ArrivalContactAdapter(this, mChosen, R.layout.item_arrivalcontact);

		mContactPicker = new ContactPickerDialogFragment();
		mContactPicker.setListener(new ContactPickerDialogFragment.ContactPickerListener() {
				
			@Override
			public void onContactsPick(List<Contact> contacts) {
				mChosen.clear();
				mChosen.addAll(contacts);
				mAdapter.notifyDataSetChanged();
			}
		});
		mContactPicker.show(this);
		
		mGrid = (GridView) findViewById(R.id.pickarrivalcontacts_grid);
		mGrid.setAdapter(mAdapter);
		mGrid.setOnItemClickListener(this);
		
		mRoute = mEntourage.isSet() ? mEntourage.getRoute() : mEntourage.getTemporaryRoute();
		
		mEtaKnob.setOnSeekBarChangeListener(this);
		mEta = mEtaMilli = mRoute.durationSeconds() * 1000;
		mEtaMilliPerStep = 2 * mEtaMilli / mEtaKnob.getMax(); //times 2 to go up to double the eta
		updateEta();
		
		UiUtils.showTutorialTipDialog(
				this,
				R.string.ts_entourage_tutorial_contacts_title,
				R.string.ts_entourage_tutorial_contacts_message,
				"entourage.members");
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mBusy.setVisibility(mEntourage.isSet() ? View.VISIBLE : View.GONE);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		int menuResource = mEntourage.isSet() ? R.menu.stop : R.menu.start;
		getMenuInflater().inflate(menuResource, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_start:
			startEntourage();
			return true;
		case R.id.action_stop:
			mEntourage.stop();
			UiUtils.startActivityNoStack(this, MainActivity.class);
			return true;
		case android.R.id.home:
			UiUtils.startActivityNoStack(this, MainActivity.class);
			return true;
		}
		return false;
	}
	
	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
		if (position == 0) {
			mContactPicker.show(this);
		} else if (position > 0) {
			Contact c = mAdapter.getItem(position);
			mContactPicker.removeContact(this, c);
			mChosen.remove(c);
			mAdapter.notifyDataSetChanged();
		}
	}
	
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		mEta = (progress + 1) * mEtaMilliPerStep;
		updateEta();
	}

	@Override
	public void onStartTrackingTouch(SeekBar arg0) {}

	@Override
	public void onStopTrackingTouch(SeekBar arg0) {}
	
	private void updateEta() {
		Duration eta = new Duration(mEta);
		
		PeriodFormatter formatter = new PeriodFormatterBuilder()
				.printZeroAlways()
				.minimumPrintedDigits(2)
				.appendHours()
				.appendSuffix(":")
				.appendMinutes()
				.appendSuffix(":")
				.appendSeconds()
				.toFormatter();
		String formatted = formatter.print(eta.toPeriod());
		
		mEtaText.setText(formatted);
	}
	
	private void startEntourage() {
		Contact[] chosenArray = new Contact[mChosen.size()];
		
		for (int i = 0; i < mChosen.size(); i++) {
			chosenArray[i] = mChosen.get(i);
		}

		long etaSeconds = mEta/1000;
		
		Log.i("aaa", "Entourage route=" + mRoute);
		
		mEntourage.start(mRoute, etaSeconds, chosenArray);
		UiUtils.startActivityNoStack(this, MainActivity.class);
	}

	@Override
	public void onStatusChange(SyncStatus status, String extra) {
		boolean idle = status.equals(SyncStatus.IDLE);
		boolean busy = status.equals(SyncStatus.BUSY);
	}

	@Override
	public void onMessageSent(boolean ok, String message, String errorIfNotOk) {
		
	}
	
	/*
	private void addRemoveMessageEntourage() {
		JavelinEntourageManager entourage = JavelinClient.getInstance(this, TapShieldApplication.JAVELIN_CONFIG)
				.getEntourageManager();
		entourage.addMemberWithEmail("Human", "ajlelorriaga@gmail.com", this);
		entourage.addMemberWithPhone("turian", "786-942-2568", this);
		entourage.removeMemberWithId(72, this);
		entourage.messageMembers("idk man", this);
	}
	*/
}
