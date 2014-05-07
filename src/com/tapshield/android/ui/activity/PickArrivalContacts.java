package com.tapshield.android.ui.activity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.joda.time.Duration;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.tapshield.android.R;
import com.tapshield.android.manager.EntourageManager;
import com.tapshield.android.ui.adapter.ArrivalContactAdapter;
import com.tapshield.android.ui.view.CircleSeekBar;
import com.tapshield.android.utils.ContactsRetriever;
import com.tapshield.android.utils.ContactsRetriever.Contact;
import com.tapshield.android.utils.ContactsRetriever.ContactsRetrieverListener;
import com.tapshield.android.utils.UiUtils;

public class PickArrivalContacts extends Activity
		implements ContactsRetrieverListener, OnItemClickListener, OnSeekBarChangeListener {

	private EntourageManager mEntourage;
	private GridView mGrid;
	private ArrivalContactAdapter mAdapter;
	private ContactSelectionFragment mSelectionFragment;
	private boolean mSelectionFragmentShown = false;
	private List<Contact> mContacts = new ArrayList<Contact>();
	private List<Contact> mChosen = new ArrayList<Contact>();
	
	private CircleSeekBar mEtaKnob;
	private TextView mEtaText;
	private long mEta;
	private long mEtaMilli;
	private long mEtaMilliPerStep;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_pickarrivalcontacts);

		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		
		mEntourage = EntourageManager.get(this);
		mEtaKnob = (CircleSeekBar) findViewById(R.id.pickarrivalcontacts_circleseekbar);
		mEtaText = (TextView) findViewById(R.id.pickarrivalcontacts_text_eta);
		
		mAdapter = new ArrivalContactAdapter(this, mChosen, R.layout.item_arrivalcontact);
		
		mGrid = (GridView) findViewById(R.id.pickarrivalcontacts_grid);
		mGrid.setAdapter(mAdapter);
		mGrid.setOnItemClickListener(this);
		
		if (savedInstanceState != null) {
			mSelectionFragment = (ContactSelectionFragment) getFragmentManager()
					.findFragmentById(R.id.pickarrivalcontacts_container);
			
			mSelectionFragmentShown = mSelectionFragment != null;
		}
		
		mEtaKnob.setOnSeekBarChangeListener(this);
		mEta = mEtaMilli = mEntourage.getTemporaryRoute().durationSeconds() * 1000;
		mEtaMilliPerStep = 2 * mEtaMilli / mEtaKnob.getMax(); //times 2 to go up to double the eta
		updateEta();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mContacts.clear();
		new ContactsRetriever(this, this).execute(
				ContactsRetriever.TYPE_EMAIL |
				ContactsRetriever.TYPE_PHONE |
				ContactsRetriever.TYPE_PHOTO);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean shown = isSelectionFragmentShown();
		
		int menuResource = R.menu.start;
		
		if (shown) {
			menuResource = R.menu.done;
		} else if (mEntourage.isSet()) {
			menuResource = R.menu.stop;
		}
		
		getMenuInflater().inflate(menuResource, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_done:
			addSelectedContacts();
			return true;
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
	public void onBackPressed() {
		if (isSelectionFragmentShown()) {
			addSelectedContacts();
			hideSelectionFragment();
			return;
		}
		
		super.onBackPressed();
	}
	
	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
		if (position == 0) {
			showSelectionFragment();
			passContactsToSelectionFragment();
		}
	}
	
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		mEta = progress * mEtaMilliPerStep;
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
	
	@Override
	public void onContactsRetrieval(List<Contact> contacts) {
		if (isFinishing()) {
			return;
		}
		
		mContacts.clear();
		mContacts.addAll(contacts);

		passContactsToSelectionFragment();
	}
	
	private void startEntourage() {
		Contact[] chosenArray = new Contact[mChosen.size()];
		
		for (int i = 0; i < mChosen.size(); i++) {
			chosenArray[i] = mChosen.get(i);
		}

		
		mEntourage.start(mEntourage.getTemporaryRoute(), chosenArray);
		UiUtils.startActivityNoStack(this, MainActivity.class);
	}
	
	private void passContactsToSelectionFragment() {
		int len = mContacts.size();
		
		if (len == 0 || !isSelectionFragmentShown() || getSelectionFragment().areItemsSet()) {
			return;
		}
		
		String[] names = new String[len];
		
		for (int i = 0; i < len; i++) {
			names[i] = mContacts.get(i).name();
		}
		
		getSelectionFragment().setItems(names);
	}
	
	private boolean isSelectionFragmentShown() {
		return mSelectionFragmentShown && mSelectionFragment != null;
	}
	
	private ContactSelectionFragment getSelectionFragment() {
		return mSelectionFragment;
	}
	
	private void showSelectionFragment() {

		if (isSelectionFragmentShown()) {
			return;
		}
		
		if (mSelectionFragment == null) {
			mSelectionFragment = new ContactSelectionFragment();
		}

		getFragmentManager()
				.beginTransaction()
				.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
				.add(R.id.pickarrivalcontacts_container, mSelectionFragment)
				.commit();
		
		mSelectionFragmentShown = true;
		
		invalidateOptionsMenu();
	}
	
	private void hideSelectionFragment() {
		if (!isSelectionFragmentShown()) {
			return;
		}

		getFragmentManager()
				.beginTransaction()
				.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
				.remove(getSelectionFragment())
				.commit();

		mSelectionFragmentShown = false;
		
		invalidateOptionsMenu();
	}
	
	private void addSelectedContacts() {
		SparseBooleanArray checked = getSelectionFragment().getCheckedPositions();
		
		hideSelectionFragment();
		
		mChosen.clear();
		
		if (checked != null) {
			
			for (int i = 0; i < checked.size(); i++) {
				if (checked.valueAt(i)) {
					mChosen.add(mContacts.get(checked.keyAt(i)));
				}
			}
		}
		
		mAdapter.notifyDataSetChanged();
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
	
	public static class ContactSelectionFragment extends ListFragment {

		private ListView list;
		private String[] items;
		private ArrayAdapter<String> adapter;
		private boolean itemsSet = false;
		
		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			getView().setBackgroundColor(getActivity().getResources().getColor(R.color.ts_gray_light));
			list = getListView();
			list.setFastScrollEnabled(true);
			list.setFastScrollAlwaysVisible(true);
			list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		}
		
		public void setItems(String[] contactNames) {
			items = Arrays.copyOf(contactNames, contactNames.length);
			adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_multiple_choice, items);
			setListAdapter(adapter);
			adapter.notifyDataSetChanged();
			itemsSet = true;
		}
		
		public SparseBooleanArray getCheckedPositions() {
			return list.getCheckedItemPositions();
		}
		
		public boolean areItemsSet() {
			return itemsSet;
		}
	}
}
