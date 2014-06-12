
package com.tapshield.android.ui.dialog;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.tapshield.android.R;
import com.tapshield.android.utils.ContactsRetriever;
import com.tapshield.android.utils.ContactsRetriever.Contact;
import com.tapshield.android.utils.ContactsRetriever.ContactsRetrieverListener;

public class ContactPickerDialogFragment extends DialogFragment
		implements ContactsRetrieverListener {

	private static final String TAG = ContactPickerDialogFragment.class.getSimpleName();
	private static final String CACHE = ContactPickerDialogFragment.class.getSimpleName() + ".cachedIds.data";
	
	private EditText mSearch;
	private ProgressBar mLoading;
	private ListView mList;
	private ArrayAdapter<String> mArrayAdapter;
	private String mTitle;
	private ContactPickerListener mListener;
	private List<Contact> mContacts = new ArrayList<Contact>();
	private List<String> mNames = new ArrayList<String>();
	private List<Long> mCachedIds = new ArrayList<Long>();
	
	public ContactPickerDialogFragment() {
		mTitle = "Pick Entourage Members";
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		View content = getActivity().getLayoutInflater().inflate(R.layout.dialog_pickcontacts, null);

		mSearch = (EditText) content.findViewById(R.id.dialog_pickcontacts_edit);
		mList = (ListView) content.findViewById(R.id.dialog_pickcontacts_list);
		mLoading = (ProgressBar) content.findViewById(R.id.dialog_pickcontacts_progressbar);

		//filter has yet to be added
		mSearch.setVisibility(View.GONE);
		
		mArrayAdapter = new ArrayAdapter<String>(getActivity(),
				android.R.layout.simple_list_item_multiple_choice, mNames);
		
		mList.setFastScrollEnabled(true);
		mList.setFastScrollAlwaysVisible(true);
		mList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		mList.setAdapter(mArrayAdapter);
		
		loadIds(getActivity());
		
		mLoading.setVisibility(View.VISIBLE);
		mList.setVisibility(View.GONE);
		
		new ContactsRetriever(getActivity(), this)
				.execute(ContactsRetriever.TYPE_PHONE | ContactsRetriever.TYPE_EMAIL);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		
		if (mTitle != null) {
			builder.setTitle(mTitle);
		}
		
		builder
				.setView(content)
				.setPositiveButton(R.string.ts_common_ok, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						notifyListener();
					}
				});
		
		return builder.create();
	}
	
	public void setListener(ContactPickerListener listener) {
		mListener = listener;
	}
	
	public void show(Activity activity) {
		if (!isVisible()) {
			show(activity.getFragmentManager(), TAG);
		}
	}
	
	@Override
	public void onContactsRetrieval(List<Contact> contacts) {
		mContacts.clear();
		mContacts.addAll(contacts);
		
		mNames.clear();
		for (int i = 0; i < 20; i++) {
			mNames.add(mContacts.get(i).name());
		}

		setSelectedById();
		
		mArrayAdapter.notifyDataSetChanged();
		
		mLoading.setVisibility(View.GONE);
		mList.setVisibility(View.VISIBLE);
		
		notifyListener();
	}
	
	private void notifyListener() {
		if (mListener != null) {
			List<Contact> selected = new ArrayList<Contact>();
			List<Long> ids = new ArrayList<Long>();
			
			SparseBooleanArray checked = mList.getCheckedItemPositions();
			
			if (checked != null) {
				for (int i = 0; i < checked.size(); i++) {
					if (checked.valueAt(i)) {
						Contact c = mContacts.get(checked.keyAt(i));
						selected.add(c);
						ids.add(c.id());
					}
				}
			}
			
			saveIds(getActivity(), ids);
			
			mListener.onContactsPick(selected);
		}
	}
	
	public void removeContact(final Context context, Contact c) {
		loadIds(context);
		Iterator<Long> iter = mCachedIds.iterator();
		
		while (iter.hasNext()) {
			Long id = iter.next();
			if (id.longValue() == c.id()) {
				iter.remove();
			}
		}
		saveIds(context, mCachedIds);
	}
	
	private void loadIds(final Context context) {
		try {
			FileInputStream fis = context.openFileInput(CACHE);
			ObjectInputStream ois = new ObjectInputStream(fis);
			mCachedIds = (List<Long>) ois.readObject();
			ois.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void saveIds(final Context context, final List<Long> ids) {
		try {
			FileOutputStream fos = context.openFileOutput(CACHE, Context.MODE_PRIVATE);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(ids);
			oos.flush();
			oos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void setSelectedById() {
		Contact c;
		
		for (int i = 0; i < mContacts.size(); i++) {
			
			c = mContacts.get(i);
			
			if (mCachedIds != null) {
				for (int j = 0; j < mCachedIds.size(); j++) {
					Long id = mCachedIds.get(j);
					if (c.id() == id.longValue()) {
						mList.setItemChecked(i, true);
						break;
					} else if (j == mCachedIds.size() - 1) {
						mList.setItemChecked(i, false);
					}
				}
			}
		}
	}
	
	public interface ContactPickerListener {
		void onContactsPick(List<Contact> contacts);
	}
}
