package com.tapshield.android.ui.activity;

import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
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

public class GooglePlusLoginActivity extends Activity
		implements ConnectionCallbacks, OnConnectionFailedListener, OnUserLogInListener {

	private static final String TAG = "ts:googleplus";
	private static final int REQUEST_SIGN_IN = 10032;
	
	private JavelinUserManager mUserManager;
	
	private ImageView mImage;
	private GoogleApiClient mClient;
	private boolean mIntentInProgress = false;
	private boolean mSignInClicked;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login_googleplus);
		getActionBar().hide();
		
		mUserManager = JavelinClient
				.getInstance(this, TapShieldApplication.JAVELIN_CONFIG)
				.getUserManager();
		
		mImage = (ImageView) findViewById(R.id.login_googleplus_image);
		
		mClient = new GoogleApiClient.Builder(this)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.addApi(Plus.API)
				.addScope(Plus.SCOPE_PLUS_LOGIN)
				.build();
		
		new ImageFlipAnimator()
				.duration(getResources().getInteger(R.integer.ts_login_social_animation_flip))
				.imagesResources(
						R.drawable.ts_logo_shield_white_small,
						R.drawable.ts_social_googleplus_logo_small)
				.alpha(0.6f, 1.0f, 0.6f)
				.start(mImage);

		mSignInClicked = true; //creating this activity means it was user-requested
		signIn();
	}

	@Override
	protected void onStart() {
		super.onStart();
		mClient.connect();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		if (mClient.isConnected()) {
			mClient.disconnect();
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_SIGN_IN) {

			if (resultCode != RESULT_OK) {
				mSignInClicked = false;
				onUserLogIn(false, null, 0, new Throwable("Cancelled."));
			}
			
			mIntentInProgress = false;
			
			if (!mClient.isConnecting()) {
				mClient.connect();
			}
		}
	}
	
	private void signIn() {
		if (!mClient.isConnected()) {
			mClient.connect();
		}
	}
	
	private void signOut() {
		if (mClient.isConnected()) {
			Plus.AccountApi.clearDefaultAccount(mClient);
			mClient.disconnect();
		}
	}
	
	private class TokenRetriever extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... args) {
			String accountName = args[0];
			String scope = args[1];
			
			String token = null;
			
			try {
				token = GoogleAuthUtil.getToken(
						GooglePlusLoginActivity.this,
						accountName,
						scope);
			} catch (UserRecoverableAuthException e) {
				Log.e(TAG, "error retrieving token", e);
				
				startActivityForResult(e.getIntent(), REQUEST_SIGN_IN);
				
				token = null;
			} catch (IOException e) {
				Log.e(TAG, "error retrieving token", e);
				token = null;
			} catch (GoogleAuthException e) {
				Log.e(TAG, "error retrieving token", e);
				token = null;
			}
			
			return token;
		}
		
		@Override
		protected void onPostExecute(String token) {
			if (token != null) {
				Log.i(TAG, "G+ token retrieved=" + token);
				mUserManager.logInWithGooglePlus(token, GooglePlusLoginActivity.this);
			}
		}
	}
	
	@Override
	public void onConnected(Bundle b) {
		Log.i(TAG, "Connected");
		mSignInClicked = false;
		new TokenRetriever().execute(
				Plus.AccountApi.getAccountName(mClient),
				"oauth2:" + Scopes.PLUS_LOGIN + " " + Scopes.PROFILE + " email");
	}

	@Override
	public void onConnectionSuspended(int cause) {
		Log.i(TAG, "Connection suspended");
		mClient.connect();
	}

	@Override
	public void onConnectionFailed(ConnectionResult result) {
		Log.i(TAG, "Connection failed (" + result.getErrorCode() + ") " + mIntentInProgress);
		
		if (!mIntentInProgress && result.hasResolution() && mSignInClicked) {
			try {
				mIntentInProgress = true;
				startIntentSenderForResult(result.getResolution().getIntentSender(),
						REQUEST_SIGN_IN, null, 0, 0, 0);
			} catch (SendIntentException e) {
				// The intent was canceled before it was sent.  Return to the default
				// state and attempt to connect to get an updated ConnectionResult.
				mIntentInProgress = false;
				mClient.connect();
			}
		}
	}

	@Override
	public void onUserLogIn(boolean successful, User user, int errorCode, Throwable e) {
		
		if (successful) {
			attachProfileToUser(user);
			signOut();
			UiUtils.welcomeUser(this);
		} else {
			UiUtils.toastShort(this, "Error: " + e.getMessage());
		}
		
		Class<? extends Activity> clss = successful ? MainActivity.class : WelcomeActivity.class;
		UiUtils.startActivityNoStack(this, clss);
	}
	
	private void attachProfileToUser(User user) {
		if (Plus.PeopleApi.getCurrentPerson(mClient) != null) {
			Person currentPerson = Plus.PeopleApi.getCurrentPerson(mClient);

			//given format: YYYY-MM-DD
			if (currentPerson.hasBirthday()) {
				String birthday = currentPerson.getBirthday();
				String[] birthdayParts = birthday.split("-");
				String backendBirthday =
						birthdayParts[1] + "/" + birthdayParts[2] + "/" + birthdayParts[0];
				user.profile.setDateOfBirth(backendBirthday);
			}
			
			if (currentPerson.hasGender()) {
				int gender = currentPerson.getGender();
				if (gender == Person.Gender.MALE) {
					user.profile.setGender(UserProfile.GENDER_MALE);
				} else if (gender == Person.Gender.FEMALE) {
					user.profile.setGender(UserProfile.GENDER_FEMALE);
				}
			}
			
			if (currentPerson.hasImage() && currentPerson.getImage().hasUrl()) {
				String pictureUrl = currentPerson.getImage().getUrl();
				final String sizeVar = "sz";
				final String sizeRegex = sizeVar + "=\\d+";
				final int newSize = 300;
				
				pictureUrl = pictureUrl.replaceAll(sizeRegex, sizeVar + "=" + newSize);
				
				new AsyncImageDownloaderToFile(GooglePlusLoginActivity.this, pictureUrl,
						UserProfile.getPictureFile(GooglePlusLoginActivity.this),
						UserProfile.ACTION_USER_PICTURE_UPDATED, false)
						.execute();
			}
			
			mUserManager.setUser(user);
		}
	}
}
