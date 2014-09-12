package com.tapshield.android.ui.activity;

import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.Session.NewPermissionsRequest;
import com.facebook.Session.OpenRequest;
import com.facebook.Session.StatusCallback;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.facebook.widget.LoginButton;
import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinUserManager;
import com.tapshield.android.api.JavelinUserManager.OnUserLogInListener;
import com.tapshield.android.api.JavelinUtils.AsyncImageDownloaderToFile;
import com.tapshield.android.api.model.User;
import com.tapshield.android.api.model.UserProfile;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.utils.UiUtils;

import elorriaga.leon.android.imageflipanimator.ImageFlipAnimator;

public class FacebookLoginActivity extends Activity
		implements StatusCallback, OnUserLogInListener {

	private static final String TAG = "ts:facebook";
	
	private JavelinUserManager mUserManager;

	private ImageView mImage;
	private LoginButton mSignIn;
	private UiLifecycleHelper mUiHelper;
	private boolean mJavelinLoggingIn = false;
	private boolean mRequestedLogOut;
	private boolean mSecondChance;
	private int mNewPermissionRetry;
	
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
		mSignIn.setReadPermissions(getPermissions());
		mSignIn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Session s = Session.getActiveSession();
				if (!s.isClosed() && !s.isOpened()) {
					OpenRequest openRequest = new OpenRequest(FacebookLoginActivity.this)
							.setPermissions(getPermissions())
							.setCallback(FacebookLoginActivity.this);

					Log.i(TAG, "permissions getter returning=" + getPermissions().toString());
					s.openForRead(openRequest);
					Log.i(TAG, "opening session for read permissions=" + s.getPermissions());
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
		
		mNewPermissionRetry = 0;
		mRequestedLogOut = false;
		mSecondChance = true;
		mSignIn.performClick();
		
		try {
			PackageInfo info = getPackageManager().getPackageInfo(getPackageName(),  PackageManager.GET_SIGNATURES);

			for (Signature signature : info.signatures)
			{
				MessageDigest md = MessageDigest.getInstance("SHA");
				md.update(signature.toByteArray());
				Log.d("ts:facebook", "hashkey=" + Base64.encodeToString(md.digest(), Base64.DEFAULT));
			}
		} catch (Exception e) {
			Log.d("ts:facebook", "Error getting keyhash: " + e.getMessage());
		}
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
	
	private String[] getPermissions() {
		return new String[]{"email"};
	}
	
	@Override
	public void call(Session session, SessionState state, Exception exception) {
		Log.i(TAG, "Session=" + session);
		if (state.isOpened()) {
			Log.i(TAG, "Logged in...");
			List<String> permissions = session.getPermissions();
			Log.i(TAG, "session open for read (2nd) permissions=" + permissions);
			
			if (!permissions.contains("email")) {
				mNewPermissionRetry++;
				
				if (mNewPermissionRetry >= 3) {
					onUserLogIn(false, null, 0, new Throwable("Retry Facebook sign in."));
					return;
				}
				
				try {
					session.requestNewReadPermissions(new NewPermissionsRequest(this, getPermissions()));
				} catch (Exception e) {}
			} else {
				final String accessToken = session.getAccessToken();
		        Log.i(TAG, "Facebook access token retrieved.");
		        if (!mJavelinLoggingIn) {
		        	mJavelinLoggingIn = true;
		        	mUserManager.logInWithFacebook(accessToken, this);
		        }
			}
	    } else if (state.isClosed()) {
	        Log.i(TAG, "Logged out..." + state.toString());

	        if (!mRequestedLogOut) {
	        	if (mSecondChance) {
	        		mSecondChance = false;
	        		session.closeAndClearTokenInformation();
	        		mSignIn.performClick();
	        	} else {
	        		onUserLogIn(false, null, 0, new Throwable("Check your login for Facebook."));
	        	}
	        }
	    }	
	}

	@Override
	public void onUserLogIn(boolean successful, User user, int errorCode, Throwable e) {
		if (successful) {
			attachProfileToUser(user);
		} else {
			error("Error: " + e.getMessage());
		}
	}
	
	private void signOut() {
		Session session = Session.getActiveSession();
		if (session != null && session.isOpened()) {
			mRequestedLogOut = true;
			session.closeAndClearTokenInformation();
		}
	}
	
	private void attachProfileToUser(final User user) {
		
		Request.newMeRequest(Session.getActiveSession(), new Request.GraphUserCallback() {
			
			@Override
			public void onCompleted(GraphUser graphUser, Response response) {

				if (graphUser == null) {
					Log.d(TAG, "Error fetching 'me' request. Response=" + response);
					done();
					return;
				}
				
				String birthday = graphUser.getBirthday();
				if (birthday != null && !birthday.isEmpty()) {
					user.profile.setDateOfBirth(birthday);
				}
				
				String gender = graphUser.getProperty("gender").toString();
				if (gender != null && !gender.isEmpty()) {
					
					gender = gender.toLowerCase(Locale.getDefault());
					
					if (gender.equals(UserProfile.GENDER_MALE.toLowerCase(Locale.getDefault()))) {
						user.profile.setGender(UserProfile.GENDER_MALE);
					} else if (gender.equals(UserProfile.GENDER_FEMALE.toLowerCase(Locale.getDefault()))) {
						user.profile.setGender(UserProfile.GENDER_FEMALE);
					}
				}
				
				String userId = graphUser.getId();
				if (userId != null && !userId.isEmpty()) {
					final int size = 300;
					final String pictureUrl = 
							String.format("https://graph.facebook.com/%s/picture?height=%d&width=%d",
									userId, size, size);
					new AsyncImageDownloaderToFile(FacebookLoginActivity.this, pictureUrl,
							UserProfile.getPictureFile(FacebookLoginActivity.this),
							UserProfile.ACTION_USER_PICTURE_UPDATED, false)
							.execute();
				}
				
				mUserManager.setUser(user);
				done();
			}
		}).executeAsync();
	}
	
	private void done() {
		UiUtils.welcomeUser(FacebookLoginActivity.this);
		UiUtils.startActivityNoStack(FacebookLoginActivity.this, MainActivity.class);
		signOut();
	}
	
	private void error(final String errorMessage) {
		if (errorMessage != null) {
			UiUtils.toastShort(this, errorMessage);
		}
		UiUtils.startActivityNoStack(this, WelcomeActivity.class);
	}
}
