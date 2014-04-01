package com.tapshield.android.ui.adapter;

import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.tapshield.android.R;
import com.tapshield.android.api.model.UserProfile;
import com.tapshield.android.utils.BitmapUtils;

public class NavigationListAdapter extends BaseAdapter {

	private Context mContext;
	private List<NavigationItem> mItems;
	private int mLayout;
	private int mIconImageViewId;
	private int mTitleTextViewId;
	
	public NavigationListAdapter(Context context, List<NavigationItem> items, int layoutResource,
			int iconImageViewId, int titleTextViewId) {
		mContext = context;
		mItems = items;
		mLayout = layoutResource;
		mIconImageViewId = iconImageViewId;
		mTitleTextViewId = titleTextViewId;
	}
	
	@Override
	public int getCount() {
		return mItems.size();
	}

	@Override
	public NavigationItem getItem(int position) {
		return mItems.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = LayoutInflater.from(mContext).inflate(mLayout, parent, false);
		}
		
		NavigationItem item = getItem(position);
		TextView text = (TextView) convertView.findViewById(mTitleTextViewId);
		ImageView image = (ImageView) convertView.findViewById(mIconImageViewId);
		
		text.setText(item.getTitle());
		
		//for first item, set circle-clipped profile picture if available
		if (position == 0) {
			if (UserProfile.hasPicture(mContext)) {
				Bitmap clippedBitmap = BitmapUtils.clipCircle(UserProfile.getPicture(mContext),
						BitmapUtils.CLIP_RADIUS_DEFAULT);
				image.setImageBitmap(clippedBitmap);
			} else {
				//set default
				//image.setImageResource(R.drawable.ts_navigation_profile_picture_default);
				image.setImageResource(R.drawable.ic_launcher);
			}
		} else {
			image.setImageResource(item.getIconResource());
		}
		
		return convertView;
	}
	
	public static class NavigationItem {
		private int icon = 0;
		private String title;
		
		public NavigationItem(int iconResource, int titleResource, Context context) {
			this(iconResource, context.getResources().getString(titleResource));
		}
		
		public NavigationItem(int iconResource, String title) {
			this.icon = iconResource;
			this.title = title;
		}
		
		public int getIconResource() {
			return icon;
		}
		
		public String getTitle() {
			return title;
		}
	}
}
