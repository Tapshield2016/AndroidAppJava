package com.tapshield.android.ui.adapter;

import java.util.List;

import android.R.mipmap;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.tapshield.android.R;
import com.tapshield.android.api.model.Agency;

public class AgencyListAdapter extends ArrayAdapter<Agency> {

	private LayoutInflater mInflater;
	
	private Context mContext;
	private int mResource;
	private List<Agency> mItems;
	
	public AgencyListAdapter(Context context, int resource, List<Agency> objects) {
		super(context, resource, objects);
		mContext = context;
		mResource = resource;
		mItems = objects;
	}
	
	public void setItems(List<Agency> newItems) {
		mItems = newItems;
		notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		return mItems.size();
	}
	
	@Override
	public Agency getItem(int position) {
		return mItems.get(position);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		
		if (view == null) {
			if (mInflater == null) {
				mInflater = LayoutInflater.from(mContext);
			}
			
			if (mInflater == null) {
				mInflater = (LayoutInflater)
						mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			}
			
			view = mInflater.inflate(mResource, null);
		}
		
		Agency a = getItem(position);
		TextView name = (TextView) view.findViewById(R.id.item_organizationselection_text);
		TextView distance = (TextView) view.findViewById(R.id.item_organizationselection_distance);
		
		if (name != null) {
			name.setText(a.name);
		}
		
		if (distance != null) {
			String distanceValue = a.distance < Float.MAX_VALUE ?
					Float.toString(a.distance).concat(" mi") :
							new String();
			distance.setText(distanceValue);
		}
		
		return view;
	}
}
