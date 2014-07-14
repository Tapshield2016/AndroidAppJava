package com.tapshield.android.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

public class AsyncImageDownloaderToFile extends AsyncTask<Void, Void, Boolean> {

	private Context mContext;
	private String mUrl;
	private File mFile;
	private String mAction;
	private boolean mRetry;
	
	/**
	 * @param url Url to download the image from.
	 * @param to File to save the image after download.
	 * @param action Action to be broadcasted after successful download.
	 * @param retryOnFail Boolean value to have it retry after a failed attempt to download
	 * a remotely-available image.
	 */
	public AsyncImageDownloaderToFile(Context context, String url, File to, String action,
			boolean retryOnFail) {
		mContext = context;
		mUrl = url;
		mFile = to;
		mAction = action;
		mRetry = retryOnFail;
	}
	
	@Override
	protected Boolean doInBackground(Void... params) {
		
		Bitmap bitmap = null;
		
		try {
			URL url = new URL(mUrl);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.connect();
			int responseCode = connection.getResponseCode();
			if (responseCode != HttpURLConnection.HTTP_OK) {
				Log.e("tapshield","Error fetching image (" + mUrl + ")" +
						"Response code NOT OK (" + responseCode + ")");
				return false;
			}
			
			InputStream is = connection.getInputStream();
			bitmap = BitmapFactory.decodeStream(is);
			is.close();
			connection.disconnect();
		} catch (Exception e1) {
			Log.e("tapshield", "Error fetching image (" + mUrl + ")", e1);
			return false;
		}
		
		try {
			if (bitmap != null) {
				
				if (mFile.exists()) {
					mFile.delete();
				}
				
				BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(mFile));
				bitmap.compress(CompressFormat.PNG, 80, bos);
				bos.flush();
				bos.close();
			}
		} catch (Exception e2) {
			Log.e("tapshield", "Error storing image (" + mUrl + ")", e2);
			return false;
		}
		
		return true;
	}
	
	@Override
	protected void onPostExecute(Boolean result) {
		if (result) {
			Intent broadcast = new Intent(mAction);
			mContext.sendBroadcast(broadcast);
		}
		super.onPostExecute(result);
	}
}
