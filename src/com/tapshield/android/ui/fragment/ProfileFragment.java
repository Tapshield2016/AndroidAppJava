package com.tapshield.android.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import com.tapshield.android.R;
import com.tapshield.android.ui.activity.MainActivity;
import com.tapshield.android.utils.UiUtils;

public class ProfileFragment extends BaseFragment {

	private ImageButton mPicture;
	private EditText mFirstName;
	private EditText mLastName;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_profile, container, false);
		
		mPicture = (ImageButton) root.findViewById(R.id.fragment_profile_imagebutton);
		mFirstName = (EditText) root.findViewById(R.id.fragment_profile_edit_firstname);
		mLastName = (EditText) root.findViewById(R.id.fragment_profile_edit_lastname);
		
		return root;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		mPicture.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//open suggestion dialog
				//which will open options dialog
				//after cropping or picking, result will be back here
				//optimize image 
				//refresh
			}
		});
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.finish, menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.action_finish:
			saveUserInformation();
			UiUtils.startActivityNoStack(getActivity(), MainActivity.class);
			return true;
		}
		return false;
	}
	
	private void saveUserInformation() {
		//make it a requirement?
		String firstName = mFirstName.getText().toString().trim();
		String lastName = mLastName.getText().toString().trim();
		
		//save data into instance of JavelinUserManager via JavelinClient 
	}
}
