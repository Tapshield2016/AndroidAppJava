package com.tapshield.android.ui.adapter;

import java.util.Collections;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import android.content.Context;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.tapshield.android.api.model.MassAlert;

public class MassAlertAdapter extends BaseAdapter {

	private Context mContext;
	private int mResource;
	private int mMessageResource;
	private int mDateTimeResource;
	private String mDateTimeFormatPattern = "hh:mm:ss aa MMMM dd yyyy";
	private List<MassAlert> mItems;
	private LayoutInflater mLayoutInflater;
	
	public MassAlertAdapter(Context context, int layoutResource, int messageTextResourceId,
			int dateTimeTextResourceId, List<MassAlert> items) {
		mContext = context;
		mResource = layoutResource;
		mMessageResource = messageTextResourceId;
		mDateTimeResource = dateTimeTextResourceId;
		mItems = items;
		
		if (mItems == null) {
			mItems = Collections.emptyList();
		}
	}
	
	public void setDateTimePattern(String newPattern) {
		mDateTimeFormatPattern = newPattern;
	}
	
	public void setItems(List<MassAlert> items) {
		mItems = items;
		notifyDataSetChanged();
	}
	
	public void setItemsNoNotifyDataSetChanged(List<MassAlert> items) {
		mItems = items;
	}
	
	@Override
	public int getCount() {
		return mItems.size();
	}

	@Override
	public MassAlert getItem(int position) {
		return mItems.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;

		if (view == null) {
			if (mLayoutInflater == null) {
				mLayoutInflater = LayoutInflater.from(mContext);

				//if still null, retrieve via context
				if (mLayoutInflater == null) {
					mLayoutInflater = (LayoutInflater)
							mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				}
			}

			view = mLayoutInflater.inflate(mResource, null);
		}

		MassAlert alert = getItem(position);
		TextView message = (TextView) view.findViewById(mMessageResource);
		TextView time = (TextView) view.findViewById(mDateTimeResource);
		
		DateTime utcDate = new DateTime(alert.getTimestampInSeconds() * 1000, DateTimeZone.UTC);
		DateTime currentDate = utcDate.toDateTime(DateTimeZone.getDefault());
		
		message.setText(alert.message);
		time.setText(currentDate.toString(mDateTimeFormatPattern));
		
		message.setText(Html.fromHtml(message.getText().toString()));
		message.setMovementMethod(LinkMovementMethod.getInstance());
		
		return view;
	}
}
