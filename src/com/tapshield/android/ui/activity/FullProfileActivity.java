package com.tapshield.android.ui.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinUserManager;
import com.tapshield.android.api.model.User;
import com.tapshield.android.api.model.UserProfile;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.utils.PictureSetter;

public class FullProfileActivity extends Activity implements OnClickListener {

	private ImageButton mPicture;
	private TextView mName;
	private Button mBasic;
	private Button mContact;
	private Button mAppearance;
	private Button mMedical;
	private Button mEmergencyContact;
	
	private BroadcastReceiver mPictureSetReceiver;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_fullprofile);
		
		mPicture = (ImageButton) findViewById(R.id.fullprofile_imagebutton_picture);
		mName = (TextView) findViewById(R.id.fullprofile_text_name);
		mBasic = (Button) findViewById(R.id.fullprofile_button_basic);
		mContact = (Button) findViewById(R.id.fullprofile_button_contact);
		mAppearance = (Button) findViewById(R.id.fullprofile_button_appearance);
		mMedical = (Button) findViewById(R.id.fullprofile_button_medical);
		mEmergencyContact = (Button) findViewById(R.id.fullprofile_button_emergency);
		
		mPicture.setOnClickListener(this);
		mBasic.setOnClickListener(this);
		mContact.setOnClickListener(this);
		mAppearance.setOnClickListener(this);
		mMedical.setOnClickListener(this);
		mEmergencyContact.setOnClickListener(this);
		
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowHomeEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);
		
		mPictureSetReceiver = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				loadPicture();
			}
		};
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		JavelinUserManager userManager = JavelinClient
				.getInstance(this, TapShieldApplication.JAVELIN_CONFIG)
				.getUserManager();
		
		if (userManager.isPresent()) {

			User user = userManager.getUser();
			StringBuilder nameBuilder = new StringBuilder();
			
			if (user.firstName != null) {
				nameBuilder.append(user.firstName);
			}
			
			if (user.lastName != null) {
				nameBuilder.append(" ");
				nameBuilder.append(user.lastName);
			}
			
			mName.setText(nameBuilder.toString());
		}
		
		loadPicture();
		
		IntentFilter filter = new IntentFilter(PictureSetter.ACTION_PICTURE_SET);
		registerReceiver(mPictureSetReceiver, filter);
	}
	
	@Override
	protected void onPause() {
		unregisterReceiver(mPictureSetReceiver);
		
		super.onPause();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			break;
		}
		return false;
	}
	
	@Override
	public void onClick(View view) {
		Intent activity = null;
		
		switch (view.getId()) {
		case R.id.fullprofile_imagebutton_picture:
			PictureSetter.offerOptions(this, this);
			break;
		case R.id.fullprofile_button_basic:
			activity = new Intent(this, BasicInfoActivity.class);
			break;
		case R.id.fullprofile_button_contact:
			break;
		case R.id.fullprofile_button_appearance:
			break;
		case R.id.fullprofile_button_medical:
			break;
		case R.id.fullprofile_button_emergency:
			break;
		}
		
		if (activity != null) {
			startActivity(activity);
		}
	}
	
	private void loadPicture() {
		if (!UserProfile.hasPicture(this)) {
			return;
		}
		
		Bitmap picture = UserProfile.getPicture(this);

		if (picture != null) {
			mPicture.setImageBitmap(picture);
		} else {
			mPicture.setImageResource(R.drawable.ic_launcher);
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		PictureSetter.onActivityResult(requestCode, resultCode, data);
		super.onActivityResult(requestCode, resultCode, data);
	}
}
