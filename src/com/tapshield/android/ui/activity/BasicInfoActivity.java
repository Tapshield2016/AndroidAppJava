package com.tapshield.android.ui.activity;

import org.joda.time.DateTime;
import org.joda.time.MutableDateTime;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinUserManager;
import com.tapshield.android.api.JavelinUserManager.OnUserRequiredInformationUpdateListener;
import com.tapshield.android.api.model.User;
import com.tapshield.android.api.model.UserProfile;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.utils.StringUtils;
import com.tapshield.android.utils.UiUtils;

public class BasicInfoActivity extends Activity
		implements OnDateSetListener, OnUserRequiredInformationUpdateListener {

	private static final String FORMAT_DOB = "MM/dd/yyyy";
	
	private JavelinUserManager mUserManager;
	
	private EditText mFirst;
	private EditText mLast;
	private EditText mDob;
	private Spinner mGender;
	
	private DatePickerDialog mDobPicker;
	private DateTime mDatePicked;
	
	private ProgressDialog mSaving;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_profile_basic);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		mUserManager = JavelinClient
				.getInstance(this, TapShieldApplication.JAVELIN_CONFIG)
				.getUserManager();
		
		mFirst = (EditText) findViewById(R.id.profile_basic_edit_firstname);
		mLast = (EditText) findViewById(R.id.profile_basic_edit_lastname);
		mDob = (EditText) findViewById(R.id.profile_basic_edit_dob);
		mGender = (Spinner) findViewById(R.id.profile_basic_spinner_gender);
		
		mDatePicked = new DateTime().minusYears(18);
		//picker gets zero-index month, so subtract 1 off month getter
		mDobPicker = new DatePickerDialog(this, this,
				mDatePicked.getYear(), mDatePicked.getMonthOfYear() - 1, mDatePicked.getDayOfMonth());
		
		mDob.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					mDobPicker.show();
				}
			}
		});
		
		mDob.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mDobPicker.show();
			}
		});
		
		mGender.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				//assuming a textview is obtained
				int color = getResources().getColor(
						position == 0 ? R.color.ts_gray_dark : R.color.ts_brand_dark);
				((TextView) view).setTextColor(color);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {}
		});
		
		mSaving = new ProgressDialog(this);
		mSaving.setMessage(getString(R.string.ts_profile_dialog_saving));
		mSaving.setIndeterminate(true);
		mSaving.setCancelable(false);
		
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
	public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
		MutableDateTime mutableDateTime = new MutableDateTime();
		mutableDateTime.setYear(year);
		mutableDateTime.setMonthOfYear(monthOfYear + 1); //zero-indexed month, add 1 to set right
		mutableDateTime.setDayOfMonth(dayOfMonth);
		
		mDatePicked = mutableDateTime.toDateTime();
		formatDob();
	}
	
	private void formatDob() {
		mDob.setText(mDatePicked.toString(FORMAT_DOB));
	}
	
	private void load() {
		User user = mUserManager.getUser();
		UserProfile profile = user.profile;
		
		if (user.firstName != null && !user.firstName.isEmpty()) {
			mFirst.setText(user.firstName);
		}
		
		if (user.lastName != null && !user.lastName.isEmpty()) {
			mLast.setText(user.lastName);
		}
		
		if (profile.hasDateOfBirth()) {
			mDob.setText(profile.getDateOfBirth());
		}
		
		if (profile.hasGender()) {
			String[] genders = getResources().getStringArray(R.array.ts_profile_basic_genders);
			int which = StringUtils.getIndexOf(profile.getGender(), genders);
			mGender.setSelection(which);
		}
	}
	
	private void save() {
		mSaving.show();
		
		User user = mUserManager.getUser();
		UserProfile profile = user.profile;
		
		String first = mFirst.getText().toString().trim();
		String last = mLast.getText().toString().trim();
		String dob = mDob.getText().toString().trim();
		String gender = mGender.getSelectedItem().toString().trim();
		
		if (!first.isEmpty()) {
			
			user.firstName = first;
		}
		
		if (!last.isEmpty()) {
			user.lastName = last;
		}
		
		if (!dob.isEmpty()) {
			profile.setDateOfBirth(dob);
		}
		
		if (mGender.getSelectedItemPosition() > 0) {
			profile.setGender(gender);
		}
		
		mUserManager.setUser(user);
		mUserManager.setUserProfile(profile);
		mUserManager.updateRequiredInformation(this);
	}

	@Override
	public void onUserRequiredInformationUpdate(boolean successful, Throwable e) {
		mSaving.dismiss();
		if (successful) {
			super.onBackPressed();
		} else {
			UiUtils.toastShort(this, getString(R.string.ts_profile_toast_error));
		}
	}
}
