package com.tapshield.android.ui.activity;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.widget.GridView;

import com.tapshield.android.R;
import com.tapshield.android.ui.adapter.ArrivalContactAdapter;
import com.tapshield.android.utils.ContactsRetriever;
import com.tapshield.android.utils.ContactsRetriever.Contact;
import com.tapshield.android.utils.ContactsRetriever.ContactsRetrieverListener;

public class PickArrivalContacts extends Activity implements ContactsRetrieverListener {

	private GridView mGrid;
	private ArrivalContactAdapter mAdapter;
	private List<Contact> mContacts = new ArrayList<Contact>();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_pickarrivalcontacts);

		mAdapter = new ArrivalContactAdapter(this, mContacts, R.layout.item_arrivalcontact);
		
		mGrid = (GridView) findViewById(R.id.pickarrivalcontacts_grid);
		mGrid.setAdapter(mAdapter);
		
		new ContactsRetriever(this, this).execute(ContactsRetriever.TYPE_PHOTO | ContactsRetriever.TYPE_EMAIL);
	}

	@Override
	public void onContactsRetrieval(List<Contact> contacts) {
		mContacts.clear();
		mContacts.addAll(contacts);
		mAdapter.notifyDataSetChanged();
	}
}
