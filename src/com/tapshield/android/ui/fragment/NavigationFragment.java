package com.tapshield.android.ui.fragment;

import java.util.ArrayList;
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

public class NavigationFragment extends Fragment implements OnItemClickListener {

	private ImageView mProfile;
	private ListView mList;
	private List<String> mListItems;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_navigation, container, false);
		mList = (ListView) view.findViewById(R.id.fragment_navigation_list);
		mProfile = (ImageView) view.findViewById(R.id.fragment_navigation_image);
		return view;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		mProfile.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				onItemClick(null,  null, 100, view.getId());
			}
		});

		if (mListItems == null) {
			mListItems = new ArrayList<String>();
		}
		
		mListItems.clear();
		mListItems.add("Crime Data");
		mListItems.add("Safe Circle");
		mListItems.add("Timer");
		mListItems.add("Settings");
		mListItems.add("Tutorial");
		mListItems.add("About");
		mListItems.add("Suggestions");

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
				R.layout.item_navigation, mListItems);
		mList.setAdapter(adapter);
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
