package com.tapshield.android.utils;

import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.content.IntentSender.SendIntentException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.plus.Plus;

/**
 * Steps:
 * <br>
 * 1. Get instance via static <strong>get()</strong> method.
 * <br>
 * 2. Call <strong>requestTokenWithScope()</strong>.
 * <br>
 * 3. Override <strong>onActivityResult()</strong> of calling activity and inside of it call
 * GooglePlusAuthHelper's <strong>notifyResult()</strong> passing arguments of onActivityResult()
 * so the help will make use of it if it was Google Play Service's request.
 * <br>
 * 4. Implement and pass as argument in step #2 the <strong>PlusTokensListener</strong>. 
 */
public class GooglePlusAuthHelper implements ConnectionCallbacks, OnConnectionFailedListener {

	public static final Scope SCOPE_PROFILE = Plus.SCOPE_PLUS_PROFILE;
	public static final Scope SCOPE_LOGIN = Plus.SCOPE_PLUS_LOGIN;
	
	public static final int EVENT_IDLE_DONE = 0;
	public static final int EVENT_BUSY = 1;
	public static final int EVENT_FAILED = 2;
	public static final int EVENT_FATAL = 3;
	
	private static final int REQUEST_CODE = 1000;
	
	private static GooglePlusAuthHelper mInstance;
	private Context mContext;
	private Activity mActivity;
	private GoogleApiClient mGoogleApiClient;
	private PlusTokensListener mListener;
	private String mCliendId;
	private String mTokenRequestScope;
	private boolean mIntentSent = false;
	
	private GooglePlusAuthHelper(Context context) {
		mContext = context.getApplicationContext();
	}
	
	/**
	 * <strong>Note:</strong> Read description of the class to understand workflow
	 * 
	 * @param context Activity or application context
	 * @return Instance of GooglePlusAuthHelper
	 */
	public static GooglePlusAuthHelper get(Context context) {
		if (mInstance == null) {
			mInstance = new GooglePlusAuthHelper(context);
		}
		return mInstance;
	}
	
	public void requestTokensWithScope(Activity resultActivity, String serverClientId, Scope scope, PlusTokensListener l) {
		if (scope == null || l == null || resultActivity == null || serverClientId == null) {
			try {
				throw new Exception("Null argument(s)");
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
		}

		notifyListener(EVENT_BUSY, false, null, null);
		
		mActivity = resultActivity;
		mCliendId = serverClientId;
		
		mTokenRequestScope = scope == Plus.SCOPE_PLUS_PROFILE ?
				Scopes.PROFILE : Scopes.PLUS_LOGIN;
		mListener = l;
		mGoogleApiClient = new GoogleApiClient.Builder(mContext)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.addApi(Plus.API, null)
				.addScope(scope)
				.build();
		
		mGoogleApiClient.connect();
	}
	
	public void stop() {
		if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
			mGoogleApiClient.disconnect();
		}
	}
	
	public void notifyResult(int requestCode, int resultCode) {
		mIntentSent = false;
		
		if (requestCode == REQUEST_CODE) {
			if (resultCode == Activity.RESULT_OK) {
				if (!mGoogleApiClient.isConnecting()) {
					mGoogleApiClient.connect();
				}
			} else {
				notifyListener(EVENT_IDLE_DONE, false, null, null);
			}
		}
	}

	@Override
	public void onConnectionFailed(ConnectionResult result) {
		if (result.getErrorCode() == ConnectionResult.INTERNAL_ERROR) {
			notifyListener(EVENT_FAILED, false, "internal error", "internal error");
			return;
		}
		
		if (result.hasResolution() && !mIntentSent) {
			try {
				result.startResolutionForResult(mActivity, REQUEST_CODE);
				mIntentSent = true;
			} catch (SendIntentException e) {
				notifyListener(EVENT_FAILED, false, null, null);
				mIntentSent = false;
				mGoogleApiClient.connect();
			} finally {
				notifyListener(EVENT_BUSY, false, null, null);
			}
		}
	}

	@Override
	public void onConnected(Bundle arg0) {
		new TokenFetchAsync().execute();
	}

	@Override
	public void onConnectionSuspended(int cause) {
		Log.d("gplus", "suspended");
		mGoogleApiClient.connect();
	}
	
	private void notifyListener(int event, boolean ok, String accessToken, String refreshToken) {
		if (mListener != null) {
			mListener.onEvent(event, ok, accessToken, refreshToken);
		}
	}
	
	public interface PlusTokensListener {
		void onEvent(int event, boolean ok, String accessToken, String refreshToken);
	}

	private class TokenFetchAsync extends AsyncTask<Void, String, Integer> {

		private String token = null;
		
		@Override
		protected Integer doInBackground(Void... arg0) {
			int event = EVENT_IDLE_DONE;
			String scopes = "oauth2:server:client_id:" + mCliendId + ":api_scope:" + mTokenRequestScope;
			String localToken = null;
			Log.i("gplus", "data for token"
					+ " *activity=" + mActivity.getClass().toString()
					+ " *accountName=" + Plus.AccountApi.getAccountName(mGoogleApiClient)
					+ " *scopes=" + scopes);

			try {
				localToken = GoogleAuthUtil.getToken(mContext,
						Plus.AccountApi.getAccountName(mGoogleApiClient),
						scopes);
				publishProgress(localToken);
			} catch (IOException transientEx) {
				Log.e("gplus", transientEx.toString());
				// network or server error, the call is expected to succeed if you try again later.
				// Don't attempt to call again immediately - the request is likely to
				// fail, you'll hit quotas or back-off.
				event = EVENT_FAILED;
			} catch (UserRecoverableAuthException e) {
				Log.e("gplus", e.toString());
				// Recover
				mActivity.startActivityForResult(e.getIntent(), REQUEST_CODE);
				event = EVENT_BUSY;
			} catch (GoogleAuthException authEx) {
				Log.e("gplus", authEx.toString());
				// Failure. The call is not expected to ever succeed so it should not be
				// retried.
				event = EVENT_FATAL;
			} catch (Exception e) {
				Log.e("gplus", e.toString());
				throw new RuntimeException(e);
			}
			return event;
		}
		
		@Override
		protected void onProgressUpdate(String... values) {
			token = values[0];
		}
		
		@Override
		protected void onPostExecute(Integer event) {
			notifyListener(event, token != null, token, token);
		}
	}
}
