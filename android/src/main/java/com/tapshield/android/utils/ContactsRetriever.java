package com.tapshield.android.utils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;

import com.tapshield.android.utils.ContactsRetriever.Contact;

public class ContactsRetriever extends AsyncTask<Integer, String, List<Contact>> {
	
	public static final int TYPE_EMAIL = 0x01;
	public static final int TYPE_PHONE = 0x02;
	public static final int TYPE_POSTAL = 0x04;
	public static final int TYPE_PHOTO = 0x08;
	
	private ContentResolver mContentResolver;
	private ContactsRetrieverListener mListener;
	
	public ContactsRetriever(Context context, ContactsRetrieverListener l) {
		mContentResolver = context.getContentResolver();
		mListener = l;
	}
	
	@Override
	protected List<Contact> doInBackground(Integer... params) {
		Map<Long, Contact> contacts = new HashMap<Long, Contact>();

		int type = params[0];
		boolean includePhoto = (type & TYPE_PHOTO) > 0;
		
		if ((type & TYPE_EMAIL) > 0) {
			attachContactData(contacts, TYPE_EMAIL, includePhoto);
		}
		
		if ((type & TYPE_PHONE) > 0) {
			attachContactData(contacts, TYPE_PHONE, includePhoto);
		}
		
		if ((type & TYPE_POSTAL) > 0) {
			attachContactData(contacts, TYPE_POSTAL, includePhoto);
		}
		
		List<Contact> results = new ArrayList<Contact>(contacts.values());
		Collections.sort(results);
		return results;
	}
	
	@Override
	protected void onPostExecute(List<Contact> contacts) {
		if (mListener != null) {
			mListener.onContactsRetrieval(contacts);
		}
	}
	
	private void attachContactData(final Map<Long, Contact> contacts, final int type,
			boolean photoRequired) {

		String data;
		String mimeType;
		
		if (type == TYPE_EMAIL) {
			data = Email.ADDRESS;
			mimeType = Email.CONTENT_ITEM_TYPE;
		} else if (type == TYPE_PHONE) {
			data = Phone.NUMBER;
			mimeType = Phone.CONTENT_ITEM_TYPE;
		} else {
			data = StructuredPostal.FORMATTED_ADDRESS;
			mimeType = StructuredPostal.CONTENT_ITEM_TYPE;
		}
		
		String[] projection = new String[] {
				Data.CONTACT_ID,
				Data.MIMETYPE,
				Contacts.DISPLAY_NAME,
				data
				};

		//based on projection...
		final int INDEX_ID = 0;
		final int INDEX_MIMETYPE = 1;
		final int INDEX_NAME = 2;
		final int INDEX_DATA = 3;
		
		Cursor cursor = mContentResolver.query(
				Data.CONTENT_URI,
				projection,
				null, null,
				Contacts.DISPLAY_NAME);
		
		if (cursor.moveToFirst()) {

			do {
				if (mimeType.equals(cursor.getString(INDEX_MIMETYPE))) {
					
					long id = cursor.getLong(INDEX_ID);
					String dataContent = cursor.getString(INDEX_DATA);
					
					Contact c;
					if (contacts.containsKey(id)) {
						c = contacts.get(id);
					} else {
						contacts.put(id, new Contact(id, cursor.getString(INDEX_NAME)));
						c = contacts.get(id);
					}
					
					//set data based on type
					if (type == TYPE_EMAIL) {
						c.email(dataContent);
					} else if (type == TYPE_PHONE) {
						c.phone(dataContent);
					} else if (type == TYPE_POSTAL) {
						c.address(dataContent);
					}
					
					if (c.photo == null && photoRequired) {
						Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, c.id());
						c.photo(Contacts.openContactPhotoInputStream(
								mContentResolver,
								contactUri,
								false));
					}
				}
			} while (cursor.moveToNext());
		}
	}
	
	public static interface ContactsRetrieverListener {
		void onContactsRetrieval(List<Contact> contacts);
	}
	
	public final class Contact implements Comparable<Contact> {
		private long id;
		private String name;
		private List<String> email;
		private List<String> phone;
		private List<String> address;
		private Bitmap photo;
		
		public Contact(long id, String name) {
			this.id = id;
			this.name = name;
			
			email = new ArrayList<String>();
			phone = new ArrayList<String>();
			address = new ArrayList<String>();
		}
		
		public long id() {
			return id;
		}
		
		public String name() {
			return name;
		}
		
		public List<String> email() {
			return email;
		}
		
		public void email(String email) {
			if (!this.email.contains(email)) {
				this.email.add(email);
			}
		}
		
		public List<String> phone() {
			return phone;
		}
		
		public void phone(String phone) {
			if (!this.phone.contains(phone)) {
				this.phone.add(phone);
			}
		}
		
		public List<String> address() {
			return address;
		}
		
		public void address(String street) {
			if (!this.address.contains(street)) {
				this.address.add(street);
			}
		}
		
		public Bitmap photo() {
			return photo;
		}
		
		public void photo(InputStream inputStream) {
			if (inputStream != null) {
				photo = BitmapFactory.decodeStream(inputStream);
			}
		}
		
		public String toString() {
			String m = "(Contact empty)";

			m = "id=" + id() + " name=" + name();
			m = m + " email=" + email();
			m = m + " phone=" + phone();
			m = m + " street=" + address();
			
			return m;
		}

		@Override
		public int compareTo(Contact other) {
			return this.name.compareTo(other.name);
		}
	}
}
