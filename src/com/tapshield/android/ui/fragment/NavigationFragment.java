package com.tapshield.android.ui.fragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import com.tapshield.android.R;
import com.tapshield.android.ui.adapter.NavigationListAdapter;
import com.tapshield.android.ui.adapter.NavigationListAdapter.NavigationItem;

public class NavigationFragment extends Fragment implements OnItemClickListener {

	private ListView mList;
	private NavigationItem[] ITEMS = new NavigationItem[] {
			new NavigationItem(0, "Profile"),
			new NavigationItem(R.drawable.ic_launcher, "Home"),
			new NavigationItem(android.R.drawable.ic_menu_info_details, "Notifications"),
			new NavigationItem(android.R.drawable.ic_menu_preferences, "Settings")
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
		mList.setSelected(true);
		mList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
	}
	
	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
		OnNavigationItemClickListener listener = (OnNavigationItemClickListener) getActivity();
		listener.onNavigationItemClick(position);
	}
	
	public interface OnNavigationItemClickListener {
		void onNavigationItemClick(int position);
	}
}
