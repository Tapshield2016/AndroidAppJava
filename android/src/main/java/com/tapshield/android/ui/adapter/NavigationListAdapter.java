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

import com.tapshield.android.api.model.UserProfile;
import com.tapshield.android.ui.fragment.NavigationFragment;
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
		image.setImageResource(item.getIconResource());
		
		if (item.getId() == NavigationFragment.NAV_ID_PROFILE && UserProfile.hasPicture(mContext)) {
			Bitmap clippedBitmap = BitmapUtils.clipCircle(UserProfile.getPicture(mContext),
					BitmapUtils.CLIP_RADIUS_DEFAULT);
			image.setImageBitmap(clippedBitmap);
		}
		
		return convertView;
	}
	
	public static class NavigationItem {
		private int id = -1;
		private int icon = 0;
		private String title;
		
		public NavigationItem(int id, int iconResource, int titleResource, Context context) {
			this(id, iconResource, context.getResources().getString(titleResource));
		}
		
		public NavigationItem(int id, int iconResource, String title) {
			this.id = id;
			this.icon = iconResource;
			this.title = title;
		}
		
		public int getId() {
			return id;
		}
		
		public int getIconResource() {
			return icon;
		}
		
		public String getTitle() {
			return title;
		}
	}
}
