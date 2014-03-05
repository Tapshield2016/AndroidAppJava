package com.tapshield.android.ui.adapter;

import java.util.Collections;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinUtils;
import com.tapshield.android.api.model.ChatMessage;
import com.tapshield.android.api.model.UserProfile;
import com.tapshield.android.app.TapShieldApplication;

public class ChatMessageAdapter extends BaseAdapter {

	private static final long AN_HOUR_MILLI = 1 * 60 * 60 * 1000;
	
	private Context mContext;
	private int mResource;
	private List<ChatMessage> mItems;
	
	private Bitmap mUserBitmap;
	private String mUserId;
	
	private LayoutInflater mLayoutInflater;
	
	public ChatMessageAdapter(Context context, int resource) {
		this(context, resource, null);
	}
	
	public ChatMessageAdapter(Context context, int resource, List<ChatMessage> items) {
		mContext = context;
		mResource = resource;
		
		if (items != null) {
			mItems = items;
		} else {
			mItems = Collections.emptyList();
		}
		
		prepare();
	}
	
	public void setItems(List<ChatMessage> items) {
		mItems = items;
		notifyDataSetChanged();
	}
	
	private void prepare() {
		JavelinClient javelin = JavelinClient.getInstance(mContext,
				TapShieldApplication.JAVELIN_CONFIG);
		//amazon dynamodb holds the numerical id, thus, extract and stringify it off the url
		mUserId = Integer.toString(JavelinUtils.extractLastIntOfString(
				javelin.getUserManager().getUser().url));
		
		if (UserProfile.hasPicture(mContext)) {
			mUserBitmap = UserProfile.getPicture(mContext);
		}
	}

	@Override
	public int getCount() {
		return mItems.size();
	}
	
	@Override
	public ChatMessage getItem(int position) {
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

		//objects
		ChatMessage chatMessage = getItem(position);
		TextView message = (TextView) view.findViewById(R.id.item_chat_message_text_message);
		TextView status = (TextView) view.findViewById(R.id.item_chat_message_text_status);
		ImageView iconLeft = (ImageView) view.findViewById(R.id.item_chat_message_image_left);
		ImageView iconRight = (ImageView) view.findViewById(R.id.item_chat_message_image_right);
		boolean userCreated = mUserId.equals(chatMessage.senderId);

		//values
		int backgroundColor = Color.parseColor(userCreated ? "#7BAB92" : "#F1F1F1");
		int messageTextColor = userCreated ? Color.WHITE : Color.DKGRAY;
		int statusTextColor = Color.parseColor(userCreated ? "#dddddd" : "#777777");
		
		String statusValue = new String();
		if (chatMessage.transmitting) {
			statusValue = "sending...";
		} else {

			//set "mm minutes ago..." if within an hour, otherwise, format as hh:mm (AM|PM)
			long now = System.currentTimeMillis();
			boolean withinHour = now < chatMessage.timestamp + AN_HOUR_MILLI;
			
			if (withinHour) {
				//convert different into minutes
				int minutes = (int) ((now - chatMessage.timestamp) / (1000 * 60));
				//value down to 0? say 'just now' instead
				statusValue = minutes == 0 ? "just now" : minutes + " minutes ago";
			} else {
				DateTime dateTime = new DateTime(chatMessage.timestamp);
				DateTimeFormatter formatter = DateTimeFormat.forPattern("hh:mm aa");
				statusValue = formatter.print(dateTime);
			}
		}
		
		//setters
		message.setText(chatMessage.message);
		message.setTextColor(messageTextColor);
		message.setBackgroundColor(backgroundColor);
		
		status.setText(statusValue);
		status.setTextColor(statusTextColor);
		status.setBackgroundColor(backgroundColor);
		
		iconLeft.setVisibility(userCreated ? View.INVISIBLE : View.VISIBLE);
		iconRight.setVisibility(userCreated ? View.VISIBLE : View.INVISIBLE);

		if (userCreated) {
			if (mUserBitmap != null) {
				iconRight.setImageBitmap(mUserBitmap);
			} else {
				iconRight.setImageResource(R.drawable.ic_launcher);
			}
		} else {
			iconLeft.setImageResource(R.drawable.ic_launcher);
		}
		
		return view;
	}
}
