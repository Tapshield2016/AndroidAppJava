package com.tapshield.android.utils;

import org.joda.time.DateTime;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;
import com.tapshield.android.R;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.model.SocialCrimeClusterItem;
import com.tapshield.android.ui.adapter.CrimeInfoWindowAdapter;

public class SocialCrimeMapClusterRenderer extends DefaultClusterRenderer<SocialCrimeClusterItem> {

	private Context mContext;
	private IconGenerator mClusterIconGenerator;
	private ImageView mClusterPinImageView;
	
	public SocialCrimeMapClusterRenderer(Context context, GoogleMap map,
			ClusterManager<SocialCrimeClusterItem> clusterManager) {
		super(context, map, clusterManager);
		mContext = context;
		mClusterIconGenerator = new IconGenerator(mContext);
		
		View cluster = ((LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
				.inflate(R.layout.map_cluster, null);
        mClusterPinImageView = (ImageView) cluster.findViewById(R.id.map_cluster_image);
        mClusterPinImageView.setImageResource(R.drawable.ts_pin_blank_blue);
        mClusterIconGenerator.setContentView(cluster);
        mClusterIconGenerator.setBackground(null);
        mClusterIconGenerator.setTextAppearance(R.style.TapShieldMapClusterTextBlue);
	}
	
	@Override
	protected void onBeforeClusterItemRendered(SocialCrimeClusterItem item,
			MarkerOptions markerOptions) {
		final DateTime crimeDateTime = item.getSocialCrime().getDate();
		final String type = item.getSocialCrime().getTypeName();
		final int markerDrawableResource = SocialReportsUtils.getDrawableOfType(type, true);
		final String timeDifference = DateTimeUtils.getTimeLabelFor(crimeDateTime);

		//set snippet with mandatory time label and source
		final String source = mContext.getString(R.string.ts_misc_credits_socialcrimes);
		final String snippet = timeDifference + CrimeInfoWindowAdapter.SEPARATOR + source;
		
		markerOptions
				.draggable(false)
				.icon(BitmapDescriptorFactory.fromResource(markerDrawableResource))
				.anchor(0.5f, 1.0f)
				.alpha(
						MapUtils.getOpacityOffTimeframeAt(
								crimeDateTime.getMillis(),
								new DateTime().minusHours(TapShieldApplication.CRIMES_PERIOD_HOURS).getMillis(),
								TapShieldApplication.CRIMES_MARKER_OPACITY_MINIMUM))
				.title(type)
				.snippet(snippet);
	}
	
	@Override
	protected void onBeforeClusterRendered(Cluster<SocialCrimeClusterItem> cluster,
			MarkerOptions markerOptions) {
		Bitmap icon = mClusterIconGenerator.makeIcon(String.valueOf(cluster.getSize()));
		markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon));
	}
	
	@Override
	protected boolean shouldRenderAsCluster(Cluster<SocialCrimeClusterItem> cluster) {
		return cluster.getSize() > 1;
	}
}
