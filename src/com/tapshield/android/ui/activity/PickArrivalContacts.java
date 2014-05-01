package com.tapshield.android.ui.activity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

import com.tapshield.android.R;
import com.tapshield.android.ui.adapter.ArrivalContactAdapter;
import com.tapshield.android.utils.ContactsRetriever;
import com.tapshield.android.utils.ContactsRetriever.Contact;
import com.tapshield.android.utils.ContactsRetriever.ContactsRetrieverListener;

public class PickArrivalContacts extends Activity implements ContactsRetrieverListener, OnItemClickListener {

	private GridView mGrid;
	private ArrivalContactAdapter mAdapter;
	private ContactSelectionFragment mSelectionFragment;
	private boolean mSelectionFragmentShown = false;
	private List<Contact> mContacts = new ArrayList<Contact>();
	private List<Contact> mChosen = new ArrayList<Contact>();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_pickarrivalcontacts);

		mAdapter = new ArrivalContactAdapter(this, mChosen, R.layout.item_arrivalcontact);
		
		mGrid = (GridView) findViewById(R.id.pickarrivalcontacts_grid);
		mGrid.setAdapter(mAdapter);
		mGrid.setOnItemClickListener(this);
		
		if (savedInstanceState != null) {
			mSelectionFragment = (ContactSelectionFragment) getFragmentManager()
					.findFragmentById(R.id.pickarrivalcontacts_container);
			
			mSelectionFragmentShown = mSelectionFragment != null;
		}
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
		
		int menuResource = shown ? R.menu.done : R.menu.start;
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
			
			return true;
		}
		return false;
	}
	
	@Override
	public void onBackPressed() {
		if (isSelectionFragmentShown()) {
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
	public void onContactsRetrieval(List<Contact> contacts) {
		if (isFinishing()) {
			return;
		}
		
		mContacts.clear();
		mContacts.addAll(contacts);

		passContactsToSelectionFragment();
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
