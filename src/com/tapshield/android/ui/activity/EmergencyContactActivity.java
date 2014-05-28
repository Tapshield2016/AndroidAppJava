package com.tapshield.android.ui.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Spinner;

import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinUserManager;
import com.tapshield.android.api.model.UserProfile;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.utils.StringUtils;

public class EmergencyContactActivity extends BaseFragmentActivity {

	private JavelinUserManager mUserManager;
	
	private EditText mFirst;
	private EditText mLast;
	private EditText mPhone;
	private Spinner mRelationship;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_profile_emergencycontact);

		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		mUserManager = JavelinClient
				.getInstance(this, TapShieldApplication.JAVELIN_CONFIG)
				.getUserManager();
		
		mFirst = (EditText) findViewById(R.id.profile_contact_edit_firstname);
		mLast = (EditText) findViewById(R.id.profile_contact_edit_lastname);
		mPhone = (EditText) findViewById(R.id.profile_contact_edit_phone);
		mRelationship = (Spinner) findViewById(R.id.profile_contact_spinner_relationship);
		
		load();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			return true;
		}
		return false;
	}
	
	@Override
	public void onBackPressed() {
		save();
	}
	
	private void load() {
		UserProfile profile = mUserManager.getUser().profile;
		
		if (profile.hasEmergencyContactFirstName()) {
			mFirst.setText(profile.getEmergencyContactFirstName());
		}
		
		if (profile.hasEmergencyContactLastName()) {
			mLast.setText(profile.getEmergencyContactLastName());
		}
		
		if (profile.hasEmergencyContactPhoneNumber()) {
			mPhone.setText(profile.getEmergencyContactPhoneNumber());
		}
		
		if (profile.hasEmergencyContactRelationship()) {
			String[] relationships = getResources()
					.getStringArray(R.array.ts_profile_contact_relationships);
			int which = StringUtils.getIndexOf(profile.getEmergencyContactRelationship(),
					relationships);
			mRelationship.setSelection(which);
		}
	}
	
	private void save() {
		UserProfile profile = mUserManager.getUser().profile;
		
		String first = mFirst.getText().toString().trim();
		String last = mLast.getText().toString().trim();
		String phone = mPhone.getText().toString().trim();
		String relationship = mRelationship.getSelectedItem().toString().trim();
		
		if (!first.isEmpty()) {
			profile.setEmergencyContactFirstName(first);
		}
		
		if (!last.isEmpty()) {
			profile.setEmergencyContactLastName(last);
		}
		
		if (!phone.isEmpty()) {
			profile.setEmergencyContactPhoneNumber(phone);
		}
		
		if (mRelationship.getSelectedItemPosition() > 0) {
			profile.setEmergencyContactRelationship(relationship);
		}
		
		mUserManager.setUserProfile(profile);
		super.onBackPressed();
	}
}
