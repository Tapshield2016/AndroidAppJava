package com.tapshield.android.utils;

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
import com.tapshield.android.model.CrimeClusterItem;

public class CrimeMapClusterRenderer extends DefaultClusterRenderer<CrimeClusterItem> {

	private Context mContext;
	private IconGenerator mClusterIconGenerator;
	private ImageView mClusterPinImageView;
	
	public CrimeMapClusterRenderer(Context context, GoogleMap map,
			ClusterManager<CrimeClusterItem> clusterManager) {
		super(context, map, clusterManager);
		mContext = context;
		mClusterIconGenerator = new IconGenerator(mContext);
		
		View cluster = ((LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
				.inflate(R.layout.map_cluster, null);
        mClusterPinImageView = (ImageView) cluster.findViewById(R.id.map_cluster_image);
        mClusterPinImageView.setImageResource(R.drawable.ts_pin_blank_red);
        mClusterIconGenerator.setContentView(cluster);
        mClusterIconGenerator.setBackground(null);
        mClusterIconGenerator.setTextAppearance(R.style.TapShieldMapClusterTextRed);
	}
	
	@Override
	protected void onBeforeClusterItemRendered(CrimeClusterItem item, MarkerOptions markerOptions) {
		
		MarkerOptions defaultOptions = SpotCrimeUtils.getMarkerOptionsOf(mContext,
				item.getCrime(), false);
		
		markerOptions
				.draggable(defaultOptions.isDraggable())
				.icon(defaultOptions.getIcon())
				.anchor(defaultOptions.getAnchorU(), defaultOptions.getAnchorV())
				.alpha(defaultOptions.getAlpha())
				.title(defaultOptions.getTitle())
				.snippet(defaultOptions.getSnippet());
	}
	
	@Override
	protected void onBeforeClusterRendered(Cluster<CrimeClusterItem> cluster,
			MarkerOptions markerOptions) {
		Bitmap icon = mClusterIconGenerator.makeIcon(String.valueOf(cluster.getSize()));
		markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon));
	}
	
	@Override
	protected boolean shouldRenderAsCluster(Cluster<CrimeClusterItem> cluster) {
		return cluster.getSize() > 1;
	}
}
