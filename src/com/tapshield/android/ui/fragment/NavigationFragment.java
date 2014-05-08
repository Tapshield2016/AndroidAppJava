package com.tapshield.android.ui.fragment;

import java.util.Arrays;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.tapshield.android.R;
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
	public NavigationItem[] ITEMS = new NavigationItem[] {
			new NavigationItem(NAV_ID_PROFILE, R.drawable.ts_icon_nav_profile, "Profile"),
			new NavigationItem(NAV_ID_HOME, R.drawable.ts_icon_nav_home, "Home"),
			new NavigationItem(NAV_ID_NOTIFICATION, R.drawable.ts_icon_nav_notifications, "Notifications")
			};

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_navigation, container, false);
		mList = (ListView) view.findViewById(R.id.fragment_navigation_list);
		return view;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		NavigationListAdapter adapter = new NavigationListAdapter(
				getActivity(),
				Arrays.asList(ITEMS),
				R.layout.item_navigation,
				R.id.item_navigation_image,
				R.id.item_navigation_text);
		
		mList.setAdapter(adapter);
		mList.setDivider(null);
		mList.setDividerHeight(0);
		mList.setOnItemClickListener(this);
	}
	
	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
		OnNavigationItemClickListener listener = (OnNavigationItemClickListener) getActivity();
		listener.onNavigationItemClick(ITEMS[position]);
	}
	
	public interface OnNavigationItemClickListener {
		void onNavigationItemClick(NavigationItem navigationItem);
	}
}
