package com.tapshield.android.ui.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.tapshield.android.R;
import com.tapshield.android.api.googleplaces.GooglePlaces;
import com.tapshield.android.app.TapShieldApplication;

import elor.leon.android.argazki.Argazki;

public class EntouragePagerPlaceFragment extends BaseFragment {

	public static final String EXTRA_PHOTO = "com.tapshield.android.extras.entourage-pager-place-photo";
	public static final String EXTRA_NAME = "com.tapshield.android.extras.entourage-pager-place-name";
	public static final String EXTRA_DESCRIPTION = "com.tapshield.android.extras.entourage-pager-place-desc";
	
	private ImageView mImage;
	private TextView mName;
	private TextView mDescription;
	
	private GooglePlaces mPlacesApi;
	
	public EntouragePagerPlaceFragment() {
		mPlacesApi = new GooglePlaces().config(TapShieldApplication.GOOGLEPLACES_CONFIG);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_entourage_destination_pager_place,
				container, false);
		
		mImage = (ImageView) root.findViewById(R.id.entourage_destination_pager_place_image);
		mName = (TextView) root.findViewById(R.id.entourage_destination_pager_place_text_name);
		mDescription = (TextView) root.findViewById(R.id.entourage_destination_pager_place_text_description);
		
		return root;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		Bundle extras = getArguments();
		
		if (extras != null) {
			String photoRef = extras.getString(EXTRA_PHOTO, null);
			String name = extras.getString(EXTRA_NAME, null);
			String description = extras.getString(EXTRA_DESCRIPTION, null);

			if (name != null) {
				mName.setText(name);
			}
			
			if (description != null) {
				mDescription.setText(description);
			}
			
			Log.i("ts-entourage", "photoRef=" + photoRef);
			
			if (photoRef != null) {
				String photoUrl = mPlacesApi.photoUrlOf(photoRef, 200, 200);
				Argazki.at(getActivity()).from(photoUrl).to(mImage);
				Log.i("ts-entourage", "photoUrl=" + photoUrl);
			}
		}
	}
}
