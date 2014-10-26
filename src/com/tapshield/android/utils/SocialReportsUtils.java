package com.tapshield.android.utils;

import java.util.Locale;

import org.joda.time.DateTime;

import android.content.Context;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.tapshield.android.R;
import com.tapshield.android.api.JavelinSocialReportingManager;
import com.tapshield.android.api.model.SocialCrime;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.ui.adapter.CrimeInfoWindowAdapter;


public class SocialReportsUtils {

	//check javelinsocialreportingmanager for excluded types
	public static final int[] SOCIAL_REPORTS_DRAWABLES_ID_ICONS = {
		R.drawable.ts_report_abuse_blue,
		R.drawable.ts_report_assault_blue,
		R.drawable.ts_report_caraccident_blue,
		R.drawable.ts_report_disturbance_blue,
		R.drawable.ts_report_drugs1_blue,
		R.drawable.ts_report_harassment_blue,
		R.drawable.ts_report_mentalhealth_blue,
		R.drawable.ts_report_other_blue,
		R.drawable.ts_report_suggestion_blue,
		R.drawable.ts_report_suspicious_blue,
		R.drawable.ts_report_theft_blue,
		R.drawable.ts_report_vandalism_blue
	};
	
	public static final int[] SOCIAL_REPORTS_DRAWABLES_ID_PINS = {
		R.drawable.ts_pin_abuse_blue,
		R.drawable.ts_pin_assault_blue,
		R.drawable.ts_pin_caraccident_blue,
		R.drawable.ts_pin_disturbance_blue,
		R.drawable.ts_pin_drugs1_blue,
		R.drawable.ts_pin_harassment_blue,
		R.drawable.ts_pin_mentalhealth_blue,
		R.drawable.ts_pin_other_blue,
		R.drawable.ts_pin_suggestion_blue,
		R.drawable.ts_pin_suspicious_blue,
		R.drawable.ts_pin_theft_blue,
		R.drawable.ts_pin_vandalism_blue
	};
	
	public final static int getDrawableOfType(String type, final boolean isMarker) {
		int resource = isMarker ? R.drawable.ts_pin_other_blue : R.drawable.ts_report_other_blue;
		
		final String[] list = JavelinSocialReportingManager.TYPE_LIST;
		final int len = list.length;
		
		for (int i = 0; i < len; i++) {
			if (list[i].toLowerCase(Locale.getDefault()).trim().equals(type.toLowerCase(Locale.getDefault()).trim())) {
				resource = isMarker ? SOCIAL_REPORTS_DRAWABLES_ID_PINS[i]
						: SOCIAL_REPORTS_DRAWABLES_ID_ICONS[i];
				break;
			}
		}
		
		return resource;
	}
	
	public final static MarkerOptions getMarkerOptionsOf(Context context, SocialCrime socialCrime,
			boolean attachPosition) {
		final String type = socialCrime.getTypeName();
		final int markerDrawableResource = SocialReportsUtils.getDrawableOfType(type, true);
		final String timeLabel = DateTimeUtils.getTimeLabelFor(socialCrime.getDate());

		//set snippet with mandatory time label and source
		final String source = context.getString(R.string.ts_misc_credits_socialcrimes);
		final String snippet = socialCrime.isViewed()
				+ CrimeInfoWindowAdapter.SEPARATOR + timeLabel
				+ CrimeInfoWindowAdapter.SEPARATOR + source;
		
		final float alpha = MapUtils.getOpacityOffTimeframeAt(
				socialCrime.getDate().getMillis(),
				new DateTime()
						.minusHours(TapShieldApplication.CRIMES_PERIOD_HOURS)
						.getMillis(),
				TapShieldApplication.CRIMES_MARKER_OPACITY_MINIMUM);
		
		MarkerOptions options = new MarkerOptions()
				.draggable(false)
				.icon(BitmapDescriptorFactory.fromResource(markerDrawableResource))
				.anchor(0.5f, 1.0f)
				.alpha(alpha)
				.title(type)
				.snippet(snippet);
		
		if (attachPosition) {
			options.position(new LatLng(socialCrime.getLatitude(), socialCrime.getLongitude()));
		}
		
		return options;
	}
}
