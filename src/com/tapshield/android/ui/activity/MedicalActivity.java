package com.tapshield.android.ui.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.EditText;

import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinUserManager;
import com.tapshield.android.api.model.User;
import com.tapshield.android.api.model.UserProfile;
import com.tapshield.android.app.TapShieldApplication;

public class MedicalActivity extends Activity {

	private JavelinUserManager mUserManager;
	private EditText mAllergies;
	private EditText mMedication;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_profile_medical);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		mUserManager = JavelinClient
				.getInstance(this, TapShieldApplication.JAVELIN_CONFIG)
				.getUserManager();
		
		mAllergies = (EditText) findViewById(R.id.profile_medical_edit_allergies);
		mMedication = (EditText) findViewById(R.id.profile_medical_edit_medication);
		
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
		User user = mUserManager.getUser();
		UserProfile profile = user.profile;
		
		if (profile.hasAllergies()) {
			mAllergies.setText(profile.getAllergies());
		}
		
		if (profile.hasMedications()) {
			mMedication.setText(profile.getMedications());
		}
	}
	
	private void save() {
		UserProfile profile = mUserManager.getUser().profile;
		String allergies = mAllergies.getText().toString().trim();
		String medication = mMedication.getText().toString().trim();

		if (!allergies.isEmpty()) {
			profile.setAllergies(allergies);
		}
		
		if (!medication.isEmpty()) {
			profile.setMedications(medication);
		}
		
		mUserManager.setUserProfile(profile);
		super.onBackPressed();
	}
}
