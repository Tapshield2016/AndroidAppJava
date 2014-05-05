package com.tapshield.android.ui.fragment;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.SearchView.OnQueryTextListener;

import com.tapshield.android.R;
import com.tapshield.android.utils.ContactsRetriever;
import com.tapshield.android.utils.ContactsRetriever.Contact;
import com.tapshield.android.utils.ContactsRetriever.ContactsRetrieverListener;

public class PickDestinationContactFragment extends BasePickDestinationFragment
		implements ContactsRetrieverListener {

	private List<Contact> mContacts = new ArrayList<Contact>();
	private ContactAdapter mAdapter;
	private MenuItem mSearch;
	private String mOptionalDestinationName;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mAdapter = new ContactAdapter(getActivity(), mContacts);
		setListAdapter(mAdapter);
		new ContactsRetriever(getActivity(), this).execute(ContactsRetriever.TYPE_POSTAL);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.pickdestination, menu);
		
		mSearch = menu.findItem(R.id.action_pickdestination_search);
		mSearch.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
			
			@Override
			public boolean onMenuItemActionExpand(MenuItem item) {
				return true;
			}
			
			@Override
			public boolean onMenuItemActionCollapse(MenuItem item) {
				mAdapter.clearFilter();
				return true;
			}
		});
		
		SearchView searchView = (SearchView) mSearch.getActionView();
		searchView.setQueryHint(
				getResources().getString(R.string.ts_fragment_pickdestination_contact_search_hint));
		searchView.setOnQueryTextListener(new OnQueryTextListener() {
			
			@Override
			public boolean onQueryTextSubmit(String query) {
				return true;
			}
			
			@Override
			public boolean onQueryTextChange(String newText) {
				mAdapter.filterWith(newText);
				return true;
			}
		});
		
		collapseSearch();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		collapseSearch();
	}
	
	private void collapseSearch() {
		if (mSearch != null) {
			mSearch.collapseActionView();
		}
	}
	
	@Override
	public void onContactsRetrieval(List<Contact> contacts) {
		mContacts.clear();
		mContacts.addAll(contacts);
		mAdapter.notifyDataSetChanged();
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		//get contact and prompt the user to pick one if 2+ are available, otherwise, pick destination
		Contact c = mAdapter.getItem(position);
		mOptionalDestinationName = c.name();
		
		if (c.address().size() == 1) {
			destinationPicked(c.address().get(0), mOptionalDestinationName);
		} else {
			String[] options = new String[c.address().size()];
			for (int a = 0; a < options.length; a++) {
				options[a] = c.address().get(a);
			}
			
			showSelectionDialog(c.name(), options);
		}
	}
	
	private void showSelectionDialog(String title, final String[] options) {
		new AlertDialog.Builder(getActivity())
				.setTitle(title)
				.setItems(options, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Log.i("aaa", "selected=" + options[which]);
						destinationPicked(options[which], mOptionalDestinationName);
					}
				})
				.create()
				.show();
	}
	
	private class ContactAdapter extends BaseAdapter {

		private Context mContext;
		private List<Contact> mItems;
		private List<Contact> mFiltered;
		private boolean mFilter = false;
		
		public ContactAdapter(Context context, List<Contact> contacts) {
			mContext = context;
			mItems = contacts;
			mFiltered = new ArrayList<Contact>();
		}
		
		public final void filterWith(String query) {
			if (mItems == null || mItems.isEmpty()) {
				return;
			}
			
			mFilter = query != null && !query.isEmpty();
			
			if (mFilter) {
				mFiltered.clear();
				for (Contact c : mItems) {
					if (c.name().toLowerCase().contains(query)) {
						mFiltered.add(c);
					}
				}
			}
			
			notifyDataSetChanged();
		}
		
		public final void clearFilter() {
			filterWith(null);
		}
		
		@Override
		public int getCount() {
			return mFilter ? mFiltered.size() : mItems.size();
		}

		@Override
		public Contact getItem(int position) {
			return mFilter ? mFiltered.get(position) : mItems.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View view, ViewGroup parent) {
			if (view == null) {
				view = LayoutInflater.from(mContext).inflate(R.layout.item_destination, parent, false);
			}

			TextView name = (TextView) view.findViewById(R.id.item_destination_text_name);
			TextView address = (TextView) view.findViewById(R.id.item_destination_text_address);
			
			Contact c = getItem(position);
			String addresses =
					c.address().size() == 1 ? 
					c.address().get(0) :
					c.address().size() + " addresses";;
			
			name.setText(c.name());
			address.setText(addresses);
			
			return view;
		}
	}
}
