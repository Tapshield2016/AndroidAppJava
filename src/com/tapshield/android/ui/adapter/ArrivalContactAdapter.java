package com.tapshield.android.ui.adapter;

import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.tapshield.android.R;
import com.tapshield.android.utils.ContactsRetriever.Contact;

public class ArrivalContactAdapter extends BaseAdapter {

	private Context mContext;
	private List<Contact> mItems;
	private int mLayoutResource;
	
	public ArrivalContactAdapter(Context context, List<Contact> items, int layoutResource) {
		mContext = context;
		mItems = items;
		mLayoutResource = layoutResource;
	}
	
	@Override
	public int getCount() {
		return mItems.size() + 1; //add one more for the 'add' (index 0) button
	}

	@Override
	public Contact getItem(int position) {
		return mItems.get(position - 1); //return position - 1 due to extra 'add' button at 0
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View view, ViewGroup root) {
		
		if (view == null) {
			view = LayoutInflater
					.from(mContext)
					.inflate(mLayoutResource, root, false);
		}

		ImageView image = (ImageView) view.findViewById(R.id.item_arrivalcontact_image);
		TextView text = (TextView) view.findViewById(R.id.item_arrivalcontact_text);
		
		//handle 'add' button at 0 or just regular contacts
		if (position == 0) {
			text.setText("+");
		} else {
			Contact c = getItem(position);
			image.setImageBitmap(c.photo());
			text.setText(c.name());
		}
		
		return view;
	}

}
