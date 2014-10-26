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
import com.tapshield.android.ui.fragment.RequiredInfoFragment;
import com.tapshield.android.ui.view.StepIndicator;
import com.tapshield.android.utils.PictureSetter;

public class RegistrationActivity extends BaseFragmentActivity
		implements OnUserActionRequestedListener {
	
	private static final int NUM_FRAGMENTS = 2;
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
		if (mIndex < 0) {
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
			f = new RequiredInfoFragment();
			f.setTitle(getString(R.string.ts_registration_actionbar_title_createaccount));
			break;
		case 1:
			f = new EmailConfirmationFragment();
			f.setTitle(getString(R.string.ts_registration_actionbar_title_emailverification));
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
		setFragmentByIndex(extras);
	}
	
	@Override
	public void onReturn() {
		if (mIndex >= 1) {
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
		
		if (mIndex >= 1) {
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
