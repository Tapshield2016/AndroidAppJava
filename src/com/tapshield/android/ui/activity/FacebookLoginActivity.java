package com.tapshield.android.ui.activity;

import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.facebook.Session;
import com.facebook.Session.NewPermissionsRequest;
import com.facebook.Session.OpenRequest;
import com.facebook.Session.StatusCallback;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.widget.LoginButton;
import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinUserManager;
import com.tapshield.android.api.JavelinUserManager.OnUserLogInListener;
import com.tapshield.android.api.model.User;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.utils.UiUtils;

import elorriaga.leon.android.imageflipanimator.ImageFlipAnimator;

public class FacebookLoginActivity extends Activity
		implements StatusCallback, OnUserLogInListener {

	private static final String TAG = "ts:facebook";
	private static final List<String> mPermissions = Arrays.asList("public_profile", "email");
	
	private JavelinUserManager mUserManager;

	private ImageView mImage;
	private LoginButton mSignIn;
	private UiLifecycleHelper mUiHelper;
	private boolean mRequestedLogOut;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login_facebook);
		getActionBar().hide();
		
		mUserManager = JavelinClient
				.getInstance(this, TapShieldApplication.JAVELIN_CONFIG)
				.getUserManager();
		
		mImage = (ImageView) findViewById(R.id.login_facebook_image);
		
		mSignIn = (LoginButton) findViewById(R.id.authButton);
		mSignIn.setReadPermissions(mPermissions);
		mSignIn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Session s = Session.getActiveSession();
				if (!s.isClosed() && !s.isOpened()) {
					OpenRequest openRequest = new OpenRequest(FacebookLoginActivity.this)
							.setPermissions(mPermissions)
							.setCallback(FacebookLoginActivity.this);

					Log.i(TAG, "permissions=" + mPermissions.toString());
					s.openForRead(openRequest);
					Log.i(TAG, "session open for read (1st) permissions=" + s.getPermissions());
				} else {
					Session.openActiveSession(FacebookLoginActivity.this, true, FacebookLoginActivity.this);
				}
			}
		});
		
		mUiHelper = new UiLifecycleHelper(this, this);
		mUiHelper.onCreate(savedInstanceState);
		
		new ImageFlipAnimator()
				.duration(getResources().getInteger(R.integer.ts_login_social_animation_flip))
				.imagesResources(
						R.drawable.ts_logo_shield_white_small,
						R.drawable.ts_social_facebook_logo_small)
				.alpha(0.6f, 1.0f, 0.6f)
				.start(mImage);
		
		mRequestedLogOut = false;
		mSignIn.performClick();
	}
	
	@Override
	public void onResume() {
	    super.onResume();
	    
	    // For scenarios where the main activity is launched and user
	    // session is not null, the session state change notification
	    // may not be triggered. Trigger it if it's open/closed.
	    Session session = Session.getActiveSession();
	    if (session != null &&
	           (session.isOpened() || session.isClosed()) ) {
	    	call(session, session.getState(), null);
	    }
	    
	    mUiHelper.onResume();
	}

	@Override
	public void onPause() {
	    super.onPause();
	    mUiHelper.onPause();
	}

	@Override
	public void onDestroy() {
	    super.onDestroy();
	    mUiHelper.onDestroy();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
	    super.onSaveInstanceState(outState);
	    mUiHelper.onSaveInstanceState(outState);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		mUiHelper.onActivityResult(requestCode, resultCode, data);
	}
	
	@Override
	public void call(Session session, SessionState state, Exception exception) {
		if (state.isOpened()) {
			Log.i(TAG, "Logged in...");
			List<String> permissions = session.getPermissions();
			Log.i(TAG, "session open for read (2nd) permissions=" + permissions + " state=" + session.getState());
			
			if (permissions.isEmpty()) {
				session.requestNewReadPermissions(new NewPermissionsRequest(this, mPermissions));
			} else {
				final String accessToken = session.getAccessToken();
		        Log.i(TAG, "Facebook access token=" + accessToken);
				mUserManager.logInWithFacebook(accessToken, this);
			}
	    } else if (state.isClosed()) {
	        Log.i(TAG, "Logged out...");
	        if (!mRequestedLogOut) {
	        	mSignIn.performClick();
	        }
	    }	
	}

	@Override
	public void onUserLogIn(boolean successful, User user, int errorCode, Throwable e) {
		
		if (successful) {
			UiUtils.welcomeUser(this);
			Session session = Session.getActiveSession();
			if (session != null && session.isOpened()) {
				mRequestedLogOut = true;
				session.closeAndClearTokenInformation();
			}
		} else {
			UiUtils.toastShort(this, "Error: " + e.getMessage());
		}
		
		Class<? extends Activity> clss = successful ? MainActivity.class : WelcomeActivity.class;
		UiUtils.startActivityNoStack(this, clss);
	}
}
