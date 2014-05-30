package com.tapshield.android.ui.activity;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinUserManager;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.ui.fragment.BaseFragment;
import com.tapshield.android.ui.fragment.BaseFragment.OnUserActionRequestedListener;
import com.tapshield.android.ui.fragment.EmailConfirmationFragment;
import com.tapshield.android.ui.fragment.OrganizationSelectionFragment;
import com.tapshield.android.ui.fragment.PhoneConfirmationFragment;
import com.tapshield.android.ui.fragment.ProfileFragment;
import com.tapshield.android.ui.fragment.RequiredInfoFragment;
import com.tapshield.android.ui.view.StepIndicator;
import com.tapshield.android.utils.PictureSetter;

public class RegistrationActivity extends BaseFragmentActivity
		implements OnUserActionRequestedListener {
	
	public static final String EXTRA_SKIP_ORG = "com.tapshield.android.extra.skip_org";
	public static final String EXTRA_SET_STEP = "com.tapshield.android.extra.set_step";
	
	public static final int STEP_PHONEVERIFICATION = 3;
	public static final int STEP_TERMSCONDITIONS = 4;
	
	private static final int NUM_FRAGMENTS = 5;
	private static final int mFragmentContainer = R.id.registration_container;

	private TextView mStepTitle;
	private StepIndicator mStepIndicator;
	private FragmentManager mFragmentManager;
	
	private int mIndex = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_registration);
		mFragmentManager = getSupportFragmentManager();
		
		View actionBarCustomView = getLayoutInflater().inflate(R.layout.actionbar_steps, null);
		actionBarCustomView.setLayoutParams(new ActionBar.LayoutParams(
				ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT));
		
		mStepTitle = (TextView) actionBarCustomView.findViewById(R.id.actionbar_steps_text);
		mStepIndicator = (StepIndicator)
				actionBarCustomView.findViewById(R.id.actionbar_steps_stepindicator);
		
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setDisplayShowCustomEnabled(true);
		actionBar.setCustomView(actionBarCustomView);

		Intent i = getIntent();
		if (i != null) {
			Bundle e = i.getExtras();
			if (e != null && e.containsKey(EXTRA_SKIP_ORG)) {
				boolean skipOrg = e.getBoolean(EXTRA_SKIP_ORG, false);
				if (skipOrg) {
					mIndex = 1;
				}
			}
			
			if (e != null && e.containsKey(EXTRA_SET_STEP)) {
				int index = e.getInt(EXTRA_SET_STEP, -1);
				if (index >= 1) {
					mIndex = index;
				}
			}
		}
		
		setFragmentByIndex();
		mStepIndicator.setNumSteps(NUM_FRAGMENTS);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			promptToCancel();
			return true;
		}
		return false;
	}
	
	private AlertDialog getCancelDialog(final int messageResource) {
		return new AlertDialog.Builder(this)
				.setMessage(getString(messageResource))
				.setCancelable(true)
				.setPositiveButton(R.string.ts_common_yes, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						JavelinClient.getInstance(RegistrationActivity.this,
								TapShieldApplication.JAVELIN_CONFIG)
								.getUserManager()
								.logOut(new JavelinUserManager.OnUserLogOutListener() {
									@Override
									public void onUserLogOut(boolean successful, Throwable e) {}
								});
						finish();
					}
				})
				.setNegativeButton(R.string.ts_common_no, null)
				.create();
	}
	
	private void setFragmentByIndex() {
		setFragmentByIndex(null);
	}
	
	private void setFragmentByIndex(Bundle extras) {
		boolean finish = mIndex < 0;
		
		if (finish) {
			finish();
			return;
		}

		//set flag to either add or replace the fragments based on what the container has
		BaseFragment fragment = (BaseFragment) mFragmentManager.findFragmentById(mFragmentContainer);
		boolean add = fragment == null;
		
		fragment = instantiateFragmentByIndex();
		
		if (fragment == null) {
			finish();
			return;
		}
		
		if (extras != null) {
			fragment.setArguments(extras);
		}
		
		//add/replace instantiated fragment
		FragmentTransaction transaction = mFragmentManager.beginTransaction();
		if (add) {
			transaction.add(mFragmentContainer, fragment);
		} else {
			transaction.replace(mFragmentContainer, fragment);
		}
		transaction.commit();
		
		mStepTitle.setText(fragment.getTitle());
		mStepIndicator.setCurrentStep(mIndex);
	}
	
	private BaseFragment instantiateFragmentByIndex() {
		BaseFragment f = null;
		switch (mIndex) {
		case 0:
			f = new OrganizationSelectionFragment();
			f.setTitle(getString(R.string.ts_registration_actionbar_title_pickorg));
			break;
		case 1:
			f = new RequiredInfoFragment();
			f.setTitle(getString(R.string.ts_registration_actionbar_title_createaccount));
			break;
		case 2:
			f = new EmailConfirmationFragment();
			f.setTitle(getString(R.string.ts_registration_actionbar_title_emailverification));
			break;
		case 3:
			f = new PhoneConfirmationFragment();
			f.setTitle(getString(R.string.ts_registration_actionbar_title_phoneconfirmation));
			break;
		case 4:
			f = new ProfileFragment();
			f.setTitle(getString(R.string.ts_registration_actionbar_title_profile));
			break;
		}
		return f;
	}

	/*
	interface implemented to either:
	1. proceed (next step in the registration process), or
	2. return (previous step)
	*/

	@Override
	public void onProceed(Bundle extras) {
		mIndex++;
		
		//if phone confirmation should be shown but no agency/organization is related to the user
		// then skip to profile fragment for partial information
		if (mIndex == 3) {
			boolean skipPhoneConfirmation = !JavelinClient
					.getInstance(this, TapShieldApplication.JAVELIN_CONFIG)
					.getUserManager()
					.getUser()
					.belongsToAgency();
			
			if (skipPhoneConfirmation) {
				mIndex++;
			}
		}
		
		setFragmentByIndex(extras);
	}
	
	@Override
	public void onReturn() {
		if (mIndex >= 2) {
			promptToCancel();
			return;
		}
		
		mIndex--;
		setFragmentByIndex();
	}
	
	@Override
	public void onBackPressed() {
		onReturn();
	}
	
	private void promptToCancel() {
		
		int messageResource = R.string.ts_registration_dialog_cancel_simple_message;
		
		if (mIndex >= 2) {
			messageResource = R.string.ts_registration_dialog_cancel_aftercreation_message;
		}
		
		getCancelDialog(messageResource).show();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		PictureSetter.onActivityResult(requestCode, resultCode, data);
		super.onActivityResult(requestCode, resultCode, data);
	}
}
