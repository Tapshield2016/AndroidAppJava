package com.tapshield.android.ui.adapter;

import java.util.Collections;
import java.util.List;

import org.joda.time.DateTime;

import android.content.Context;
import android.graphics.Bitmap;
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
import com.tapshield.android.utils.BitmapUtils;

public class ChatMessageAdapter extends BaseAdapter {

	private Context mContext;
	private int mResourceUser;
	private int mResourceOther;
	private List<ChatMessage> mItems;
	
	private Bitmap mUserBitmap;
	private String mUserId;
	
	private LayoutInflater mLayoutInflater;
	
	public ChatMessageAdapter(Context context, int resourceUser, int resourceOther) {
		this(context, resourceUser, resourceOther, null);
	}
	
	public ChatMessageAdapter(Context context, int resourceUser, int resourceOther, List<ChatMessage> items) {
		mContext = context;
		mResourceUser = resourceUser;
		mResourceOther = resourceOther;
		
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
	
	public void setItemsNoNotifyDataSetChanged(List<ChatMessage> items) {
		mItems = items;
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
		
		ChatMessage chatMessage = getItem(position);
		boolean userCreated = mUserId.equals(chatMessage.senderId);
		
		if (mLayoutInflater == null) {
			mLayoutInflater = LayoutInflater.from(mContext);
			
			//if still null, retrieve via context
			if (mLayoutInflater == null) {
				mLayoutInflater = (LayoutInflater)
						mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			}
		}
		
		int layoutToInflate = userCreated ? mResourceUser : mResourceOther;
		convertView = mLayoutInflater.inflate(layoutToInflate, null);

		TextView message = (TextView) convertView.findViewById(R.id.item_chat_message_text_message);
		TextView status = (TextView) convertView.findViewById(R.id.item_chat_message_text_status);
		ImageView icon = (ImageView) convertView.findViewById(R.id.item_chat_message_image);
		

		String statusValue = new String();
		if (chatMessage.transmitting) {
			statusValue = "sending...";
		} else {
			//using gmt-approx (utc) to convert to current timezone
			//NOTE: AWS (DDB) STORES TIMESTAMPS IN SECONDS. THUS, timestamp TIMES 1000 (s -> ms)
			long utc = chatMessage.timestamp * 1000;
			DateTime local = new DateTime(utc);

			//set "mm minutes ago..." if within an hour, otherwise, format as hh:mm (AM|PM)
			long now = System.currentTimeMillis();
			
			int diffMinutes = (int) ((now - local.getMillis()) / (1000 * 60));
			
			if (diffMinutes == 0) {
				statusValue = "just now";
			} else if (diffMinutes == 1) {
				statusValue = diffMinutes + " minute ago";
			} else if (diffMinutes > 1 && diffMinutes < 60) {
				statusValue = diffMinutes + " minutes ago";
			} else {
				statusValue = local.toString("hh:mm aa");
			}
		}
		
		message.setText(chatMessage.message);
		status.setText(statusValue);
		
		if (userCreated) {
			if (mUserBitmap != null) {
				Bitmap circleClipped = BitmapUtils.clipCircle(mUserBitmap, 0);
				icon.setImageBitmap(circleClipped);
			} else {
				icon.setImageResource(R.drawable.ic_launcher);
			}
		} else {
			icon.setImageResource(R.drawable.ic_launcher);
		}
		
		return convertView;
	}
}
