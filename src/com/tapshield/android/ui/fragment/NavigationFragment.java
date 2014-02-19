package com.tapshield.android.ui.fragment;

import java.util.ArrayList;
import java.util.List;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.tapshield.android.R;

public class NavigationFragment extends Fragment {

	private ListView mList;
	private List<String> mListItems = new ArrayList<String>();
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_navigation, container, false);
		mList = (ListView) view.findViewById(R.id.fragment_navigation_list);
		return view;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
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
	}
	
}
