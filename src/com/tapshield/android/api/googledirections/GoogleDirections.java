package com.tapshield.android.api.googledirections;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import android.os.AsyncTask;

import com.google.gson.Gson;
import com.tapshield.android.api.googledirections.model.GoogleDirectionsResponse;

public class GoogleDirections {

	public static final void request(final GoogleDirectionsRequest request,
			final GoogleDirectionsListener l) {
		
		new AsyncTask<Void, Void, GoogleDirectionsResponse>() {

			@Override
			protected GoogleDirectionsResponse doInBackground(Void... arg0) {
				
				GoogleDirectionsResponse response = null;
				HttpsURLConnection connection = null;
				try {
					
					URL hostname = new URL(request.url());
					connection = (HttpsURLConnection) hostname.openConnection();
					
					if (connection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
						
						Gson gson = new Gson();
						Reader reader = new InputStreamReader(connection.getInputStream());
						response = gson.fromJson(reader, GoogleDirectionsResponse.class);
					} else {
						response = null;
					}
				} catch (Exception e) {
					response = null;
				} finally {
					connection.disconnect();
				}
				
				return response;
			}
			
			@Override
			protected void onPostExecute(GoogleDirectionsResponse result) {
				super.onPostExecute(result);
				l.onDirectionsRetrieval(result != null, result);
			}
		}.execute();
	}
	
	public interface GoogleDirectionsListener {
		void onDirectionsRetrieval(boolean ok, GoogleDirectionsResponse response);
	}
}
