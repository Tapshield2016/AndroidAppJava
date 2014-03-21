package com.tapshield.android.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;

import com.tapshield.android.R;
import com.tapshield.android.ui.activity.RegistrationActivity;

public class LoginFragment extends BaseFragment implements OnClickListener, OnMenuItemClickListener {

	private Button mLogin;
	private Button mSignUp;
	
	private boolean mLoginPressed;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_login, container, false);
		
		mLogin = (Button) root.findViewById(R.id.fragment_login_button_login);
		mSignUp = (Button) root.findViewById(R.id.fragment_login_button_signup);
		
		return root;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		mLogin.setOnClickListener(this);
		mSignUp.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		//CHECK ICONCONTEXTMENU CLASS TO SUPPORT ICONS
		
		mLoginPressed = v.getId() == R.id.fragment_login_button_login;
		
		PopupMenu menu = new PopupMenu(getActivity(), v);
		menu.inflate(R.menu.login_signup);
		menu.setOnMenuItemClickListener(this);
		menu.show();
	}

	@Override
	public boolean onMenuItemClick(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_email:
			if (mLoginPressed) {
				//show login layout and hide this one
			} else {
				Intent registration = new Intent(getActivity(), RegistrationActivity.class);
				startActivity(registration);
			}
			break;
		case R.id.menu_facebook:
			break;
		case R.id.menu_twitter:
			break;
		case R.id.menu_googleplus:
			break;
		case R.id.menu_linkedin:
			break;
		default:
			return false;
		}
		return true;
	}
}
