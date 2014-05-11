package com.tapshield.android.ui.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
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
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinUserManager;
import com.tapshield.android.api.model.User;
import com.tapshield.android.api.model.UserProfile;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.ui.activity.MainActivity;
import com.tapshield.android.utils.PictureSetter;
import com.tapshield.android.utils.UiUtils;

public class ProfileFragment extends BaseFragment {

	private ImageButton mPicture;
	private EditText mFirstName;
	private EditText mLastName;
	
	private BroadcastReceiver mPictureSetReceiver;
	
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
				PictureSetter.offerOptions(getActivity(), getActivity());
			}
		});
		
		mPictureSetReceiver = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				loadPicture();
			}
		};
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		loadPicture();
		
		IntentFilter filter = new IntentFilter(PictureSetter.ACTION_PICTURE_SET);
		getActivity().registerReceiver(mPictureSetReceiver, filter);
	}
	
	@Override
	public void onPause() {
		getActivity().unregisterReceiver(mPictureSetReceiver);
		
		super.onPause();
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
	
	private void loadPicture() {
		if (!UserProfile.hasPicture(getActivity())) {
			return;
		}
		
		Bitmap picture = UserProfile.getPicture(getActivity());

		if (picture != null) {
			mPicture.setImageBitmap(picture);
		} else {
			mPicture.setImageResource(R.drawable.ic_launcher);
		}
	}
	
	private void saveUserInformation() {
		String firstName = mFirstName.getText().toString().trim();
		String lastName = mLastName.getText().toString().trim();
		
		if (!firstName.isEmpty() || !lastName.isEmpty()) {
			JavelinUserManager userManager = JavelinClient
					.getInstance(getActivity(), TapShieldApplication.JAVELIN_CONFIG)
					.getUserManager();
			
			User user = userManager.getUser();
			
			if (!firstName.isEmpty()) {
				user.firstName = firstName;
			}
			
			if (!lastName.isEmpty()) {
				user.lastName = lastName;
			}
			
			userManager.setUser(user);
		}
	}
}
