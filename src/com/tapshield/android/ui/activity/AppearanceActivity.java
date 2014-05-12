package com.tapshield.android.ui.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinUserManager;
import com.tapshield.android.api.model.UserProfile;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.ui.dialog.HeightPickerDialogFragment;
import com.tapshield.android.ui.dialog.HeightPickerDialogFragment.HeightPickerListener;
import com.tapshield.android.ui.dialog.WeightPickerDialogFragment;
import com.tapshield.android.ui.dialog.WeightPickerDialogFragment.WeightPickerListener;
import com.tapshield.android.utils.StringUtils;

public class AppearanceActivity extends Activity
		implements WeightPickerListener, HeightPickerListener {

	private JavelinUserManager mUserManager;
	
	private Spinner mRace;
	private Spinner mHaircolor;
	private EditText mWeight;
	private EditText mHeight;
	
	private WeightPickerDialogFragment mWeightPicker;
	private HeightPickerDialogFragment mHeightPicker;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_profile_appearance);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		mUserManager = JavelinClient
				.getInstance(this, TapShieldApplication.JAVELIN_CONFIG)
				.getUserManager();
		
		mRace = (Spinner) findViewById(R.id.profile_appearance_spinner_race);
		mHaircolor = (Spinner) findViewById(R.id.profile_appearance_spinner_haircolor);
		mWeight = (EditText) findViewById(R.id.profile_appearance_edit_weight);
		mHeight = (EditText) findViewById(R.id.profile_appearance_edit_height);
		
		OnItemSelectedListener itemSelectedListener = new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				//assuming a textview is obtained
				int color = getResources().getColor(
						position == 0 ? R.color.ts_gray_dark : R.color.ts_brand_dark);
				((TextView) view).setTextColor(color);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {}
		};
		
		mRace.setOnItemSelectedListener(itemSelectedListener);
		mHaircolor.setOnItemSelectedListener(itemSelectedListener);
		
		mWeight.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					mWeightPicker.show(AppearanceActivity.this);
				}
			}
		});
		
		mWeight.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mWeightPicker.show(AppearanceActivity.this);
			}
		});
		
		mHeight.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					heightStringToPicker();
				}
			}
		});
		
		mHeight.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				heightStringToPicker();
			}
		});
		
		mWeightPicker = new WeightPickerDialogFragment()
				.setTitle(R.string.ts_profile_appearance_weight_dialog)
				.setListener(this);
		
		mHeightPicker = new HeightPickerDialogFragment()
				.setListener(this);
		
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
	
	@Override
	public void onWeightSet(int pounds) {
		mWeight.setText(pounds + " lbs");
	}
	
	@Override
	public void onHeightSet(int feet, int inches) {
		mHeight.setText(feet + "' " + inches + "\"");
	}
	
	private void load() {
		UserProfile profile = mUserManager.getUser().profile;
		
		int which = 0;
		String[] array;
		
		if (profile.hasRace()) {
			array = getResources().getStringArray(R.array.ts_profile_appearance_races);
			which = StringUtils.getIndexOf(profile.getRace(), array);
			mRace.setSelection(which);
		}
		
		if (profile.hasHairColor()) {
			array = getResources().getStringArray(R.array.ts_profile_appearance_haircolors);
			which = StringUtils.getIndexOf(profile.getHairColor(), array);
			mHaircolor.setSelection(which);
		}
		
		if (profile.hasWeight()) {
			int pounds = profile.getWeight();
			mWeightPicker.setValue(pounds);
			mWeight.setText(profile.getWeight() + " lbs");
		}
		
		if (profile.hasHeight()) {
			mHeight.setText(profile.getHeight());
		}
	}
	
	private void save() {
		UserProfile profile = mUserManager.getUser().profile;
		String race = mRace.getSelectedItem().toString().trim();
		String haircolor = mHaircolor.getSelectedItem().toString().trim();

		if (mRace.getSelectedItemPosition() > 0) {
			profile.setRace(race);
		}
		
		if (mHaircolor.getSelectedItemPosition() > 0) {
			profile.setHairColor(haircolor);
		}
		
		if (!mWeight.getText().toString().isEmpty()) {
			profile.setWeight(mWeightPicker.getValue());
		}
		
		if (!mHeight.getText().toString().isEmpty()) {
			profile.setHeight(mHeightPicker.getFeet() + "' " + mHeightPicker.getInches() + "\"");
		}
		
		mUserManager.setUserProfile(profile);
		super.onBackPressed();
	}
	
	private void heightStringToPicker() {
		String height = mHeight.getText().toString().trim();
		if (!height.isEmpty()) {
			final String regexFeetMark = "'";
			final String regexNonDigit = "\\D*";
			String[] parts = height.split(regexFeetMark);
			parts[0] = parts[0].replaceAll(regexNonDigit, "");
			parts[1] = parts[1].replaceAll(regexNonDigit, "");
			int feet = Integer.parseInt(parts[0]);
			int inches = Integer.parseInt(parts[1]);
			mHeightPicker.setFeet(feet);
			mHeightPicker.setInches(inches);
		}
		mHeightPicker.show(this);
	}
}
