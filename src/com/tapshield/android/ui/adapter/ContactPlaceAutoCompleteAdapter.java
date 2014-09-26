package com.tapshield.android.ui.adapter;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.tapshield.android.R;
import com.tapshield.android.api.googleplaces.model.AutocompletePlace;
import com.tapshield.android.utils.ContactsRetriever.Contact;

public class ContactPlaceAutoCompleteAdapter extends BaseAdapter implements Filterable {

	private Context mContext;
	private Filter mFilter;
	private List<Contact> mContacts;
	private List<AutocompletePlace> mPlaces;
	private String mSearchLabel;
	
	public ContactPlaceAutoCompleteAdapter(Context context,
			List<Contact> contacts, List<AutocompletePlace> places) {
		mContext = context;
		mContacts = contacts;
		mPlaces = places;
		mFilter = buildFilter();
	}
	
	@Override
	public int getCount() {
		//plus both data set sizes, we add another extra item to do a general search of the input 
		return 1 + mContacts.size() + mPlaces.size();
	}
	
	@Override
	public Object getItem(int position) {
		/*
		if past the first data set (contacts) then return places with delta value,
		  otherwise return contact at index position
		delta subtracts number of contacts to the position to get relative index of places
		
		example:
		
		contacts = [0][1]
		places = [2][3]
		
		position = 2 (index 0 and 1 for contacts, while 2+ for places)
		delta = position - contacts.size() = 0
		places[delta] being the place with relative index
		 */
		
		if (isFirst(position)) {
			return mSearchLabel;
		}
		
		int delta = position - mContacts.size() - getOffset();
		return isContact(position) ? mContacts.get(position - getOffset()) : mPlaces.get(delta);
	}
	
	public boolean isFirst(int position) {
		return position == 0;
	}
	
	public boolean isContact(int position) {
		return !isFirst(position) && !mContacts.isEmpty() && position - getOffset() < mContacts.size();
	}
	
	public boolean isPlace(int position) {
		return !isFirst(position) && !isContact(position) && !mPlaces.isEmpty();
	}
	
	//offset added by permanent items on the list (search suggestion at 0)
	private int getOffset() {
		return 1;
	}
	
	@Override
	public long getItemId(int position) {
		return position;
	}
	
	public void setSearchTerm(String searchTerm) {
		mSearchLabel = String.format("Search for '%s'", searchTerm);
		notifyDataSetChanged();
	}
	
	@Override
	public Filter getFilter() {
		return mFilter;
	}
	
	private Filter buildFilter() {
		Filter filter = new Filter() {
			
			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				FilterResults results = new FilterResults();
				
				if (constraint != null) {
					
					List<Object> all = new ArrayList<Object>();
					all.addAll(mContacts);
					all.addAll(mPlaces);
					
					results.values = all;
					results.count = getCount();
				}
				
				return results;
			}
			
			@Override
			protected void publishResults(CharSequence constraint, FilterResults results) {
				if (results != null && results.count > 0) {
                    notifyDataSetChanged();
                }
                else {
                    notifyDataSetInvalidated();
                }
			}
		};
		
		return filter;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		
		//with different layouts based on the data sets, inflation must go through every time
		LayoutInflater inflater = LayoutInflater.from(mContext);
		
		if (inflater == null) {
			inflater = (LayoutInflater) mContext
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
		
		//set default layout (first item--search) and then check for other cases (contact or place)
		int layoutRes = R.layout.item_autocomplete_search;
		
		if (isContact(position)) {
			layoutRes = R.layout.item_autocomplete_contact;
		} else if (isPlace(position)) {
			layoutRes = R.layout.item_autocomplete_place;
		}
		
		view = inflater.inflate(layoutRes, null);
		
		Object item = getItem(position);
		
		//first (search), contact, or place
		if (isFirst(position)) {
			TextView label = (TextView) view.findViewById(R.id.item_autocomplete_search_label);
			label.setText(mSearchLabel);
		} else if (isContact(position)) {
			
			Contact contact = (Contact) item;
			
			TextView name = (TextView) view.findViewById(R.id.item_autocomplete_contact_name);
			TextView address = (TextView) view.findViewById(R.id.item_autocomplete_contact_address);
			
			name.setText(contact.name());
			
			address.setText(
					contact.address().size() >= 2
					? contact.address().size() + " Addresses"
					: contact.address().get(0));
		} else if (isPlace(position)){
			AutocompletePlace place = (AutocompletePlace) item;
			
			TextView description = (TextView) view.findViewById(R.id.item_autocomplete_place_description);
			description.setText(place.description());
		}
		
		return view;
	}
}
