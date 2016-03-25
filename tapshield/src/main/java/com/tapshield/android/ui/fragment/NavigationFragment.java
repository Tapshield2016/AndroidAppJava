package com.tapshield.android.ui.fragment;

import java.util.ArrayList;
import java.util.List;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinUserManager;
import com.tapshield.android.api.model.UserProfile;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.ui.adapter.NavigationListAdapter;
import com.tapshield.android.ui.adapter.NavigationListAdapter.NavigationItem;

public class NavigationFragment extends Fragment implements OnItemClickListener {

	public static final int NAV_ID_PROFILE = 0;
	public static final int NAV_ID_HOME = 1;
	public static final int NAV_ID_NOTIFICATION = 2;
	public static final int NAV_ID_SETTINGS = 3;
	public static final int NAV_ID_HELP = 4;
	public static final int NAV_ID_ABOUT = 5;
	
	private ListView mList;
	private NavigationListAdapter mAdapter;
	private List<NavigationItem> mItems;
	private BroadcastReceiver mProfilePictureReceiver;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_navigation, container, false);
		mList = (ListView) view.findViewById(R.id.fragment_navigation_list);
		return view;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mItems = new ArrayList<NavigationItem>();
		mAdapter = new NavigationListAdapter(
				getActivity(),
				mItems,
				R.layout.item_navigation,
				R.id.item_navigation_image,
				R.id.item_navigation_text);
		
		mList.setAdapter(mAdapter);
		mList.setDivider(null);
		mList.setDividerHeight(0);
		mList.setOnItemClickListener(this);
		
		mProfilePictureReceiver = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				setNavigationItems();
			}
		};
	}
	
	@Override
	public void onResume() {
		super.onResume();
		setNavigationItems();
		IntentFilter profilePictureUpdated = new IntentFilter(UserProfile.ACTION_USER_PICTURE_UPDATED);
		getActivity().registerReceiver(mProfilePictureReceiver, profilePictureUpdated);
	}
	
	@Override
	public void onPause() {
		getActivity().unregisterReceiver(mProfilePictureReceiver);
		super.onPause();
	}
	
	private void setNavigationItems() {
		
		mItems.clear();
		
		mItems.add(new NavigationItem(
				NAV_ID_PROFILE, R.drawable.ts_icon_nav_profile, "Profile"));
		mItems.add(new NavigationItem(
				NAV_ID_HOME, R.drawable.ts_icon_nav_home, "Home"));
		mItems.add(new NavigationItem(
				NAV_ID_NOTIFICATION, R.drawable.ts_icon_nav_notifications, "Notifications"));
		mItems.add(new NavigationItem(
				NAV_ID_SETTINGS, R.drawable.ts_icon_nav_settings, "Settings"));
		
		JavelinUserManager userManager = JavelinClient
				.getInstance(getActivity(), TapShieldApplication.JAVELIN_CONFIG)
				.getUserManager();
		
		if (userManager.isPresent()
				&& userManager.getUser().agency != null
				&& userManager.getUser().agency.infoUrl != null) {
			mItems.add(new NavigationItem(
					NAV_ID_HELP, R.drawable.ts_icon_nav_help,
					userManager.getUser().agency.name));
		}
		
		mAdapter.notifyDataSetChanged();
	}
	
	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
		OnNavigationItemClickListener listener = (OnNavigationItemClickListener) getActivity();
		listener.onNavigationItemClick(mItems.get(position));
	}
	
	public interface OnNavigationItemClickListener {
		void onNavigationItemClick(NavigationItem navigationItem);
	}
}
