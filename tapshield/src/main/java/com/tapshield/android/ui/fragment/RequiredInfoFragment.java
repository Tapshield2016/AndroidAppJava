package com.tapshield.android.ui.fragment;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinUserManager.OnUserSignUpListener;
import com.tapshield.android.api.model.User;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.utils.StringUtils;
import com.tapshield.android.utils.UiUtils;

public class RequiredInfoFragment extends BaseFragment implements OnUserSignUpListener {

	public static final String EXTRA_USER = "com.tapshield.android.extra.requiredinfofragment.user";
	
	private JavelinClient mJavelin;
	private EditText mEmail;
	private EditText mPassword;
	
	private ProgressDialog mDialogSignUp;
	
	private User mUser;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		
		mJavelin = JavelinClient.getInstance(getActivity(), TapShieldApplication.JAVELIN_CONFIG);
		
		mDialogSignUp = new ProgressDialog(getActivity());
		mDialogSignUp.setTitle(R.string.ts_fragment_requiredinfo_dialog_signup_title);
		mDialogSignUp.setMessage(getString(R.string.ts_fragment_requiredinfo_dialog_signup_message));
		mDialogSignUp.setIndeterminate(true);
		mDialogSignUp.setCancelable(false);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_requiredinfo, container, false);
		mEmail = (EditText) view.findViewById(R.id.fragment_requiredinfo_edit_email);
		mPassword = (EditText) view.findViewById(R.id.fragment_requiredinfo_edit_password);
		return view;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.next, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		switch (item.getItemId()) {
		case R.id.action_next:
			registerUser();
			return true;
		}
		
		return false;
	}
	
	private void registerUser() {
		
		String email = getTextOffEditText(mEmail);
		String password = getTextOffEditText(mPassword);
		
		if (!StringUtils.isEmailValid(email)) {
			mEmail.setError(getString(R.string.ts_fragment_requiredinfo_error_emailinvalid));
		} else if (password.length() < 4) {
			mPassword.setError(getString(R.string.ts_fragment_requiredinfo_error_passwordshort));
		} else {
			//meaning no error was found, attempt to sign up
			mDialogSignUp.show();
			
			//set user to be a reference to be passed forward after a successful signup
			mUser = new User();
			mUser.url = "dummy-url";
			mUser.email = email;
			mUser.username = mUser.email;
			mUser.setPassword(password);
			
			mJavelin.getUserManager().signUp(email, password, this);
		}
	}
	
	private void showErrorDialog(String message) {
		AlertDialog errorDialog = new AlertDialog.Builder(getActivity())
				.setTitle(R.string.ts_common_error)
				.setMessage(message)
				.setPositiveButton(R.string.ts_common_ok, null)
				.create();
		errorDialog.show();
	}
	
	private String getTextOffEditText(EditText editText) {
		return editText.getText().toString().trim();
	}
	
	@Override
	public void onUserSignUp(boolean successful, Throwable e) {
		mDialogSignUp.dismiss();
		
		if (successful) {
			UiUtils.toastShort(getActivity(), getString(R.string.ts_fragment_requiredinfo_signup_done));
			
			try {
				Bundle extras = new Bundle();
				extras.putString(EXTRA_USER, User.serialize(mUser));
				userRequestProceed(extras);
			} catch (Exception je) {
				onUserSignUp(false, je);
			}
		} else {
			showErrorDialog(e.getMessage());
		}
	}
}
