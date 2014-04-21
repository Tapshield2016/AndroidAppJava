package com.tapshield.android.api.googledirections;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;

import com.tapshield.android.api.JavelinComms;
import com.tapshield.android.api.JavelinComms.JavelinCommsCallback;
import com.tapshield.android.api.JavelinComms.JavelinCommsRequestResponse;
import com.tapshield.android.api.googledirections.model.Route;

public class GoogleDirections {

	private static final String JSON_ROUTES = "routes";
	
	public static final void request(final GoogleDirectionsRequest request,
			final GoogleDirectionsListener l) {
		
		JavelinCommsCallback internalCallback = new JavelinCommsCallback() {
			
			@Override
			public void onEnd(JavelinCommsRequestResponse response) {
				List<Route> result = null;
				String error = null;
				
				if (response.successful) {
					try {
						result = new ArrayList<Route>();
						
						JSONArray routes = response.jsonResponse.getJSONArray(JSON_ROUTES);
						
						for (int r = 0; r < routes.length(); r++) {
							Route route = Route.fromJson(routes.getJSONObject(r));
							result.add(route);
						}
					} catch (Exception e) {
						result = null;
						error = e.getMessage();
					}
				} else {
					error = response.exception.toString();
				}
				
				l.onDirectionsRetrieval(response.successful, result, error);
			}
		};
		
		JavelinComms.httpGet(request.url(), null, null, null, internalCallback);
	}
	
	public interface GoogleDirectionsListener {
		void onDirectionsRetrieval(boolean ok, List<Route> routes, String errorIfNotOk);
	}
}
