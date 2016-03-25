package com.tapshield.android.utils;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;

import com.soundcloud.android.crop.Crop;
import com.tapshield.android.R;
import com.tapshield.android.api.model.UserProfile;

public class PictureSetter {

	public static final String ACTION_PICTURE_SET = "com.tapshield.android.intent.ACTION_PICTURE_SETTER_SET";
	
	//making use of request codes of cropping library with pick utility
	private static final int INTENT_REQUEST_PICTURE_TAKE = 1202;
	private static final int INTENT_REQUEST_PICTURE_PICK = Crop.REQUEST_PICK;
	private static final int INTENT_REQUEST_PICTURE_CROP = Crop.REQUEST_CROP;
	
	private static Activity mActivity;
	private static Context mContext;
	private static Uri mPictureUri;
	private static AlertDialog mOptionsDialog;
	private static ProgressDialog mOptimizationDialog;
	
	public static void offerOptions(Activity activity, Context context) {
		mActivity = activity;
		mContext = context;
		mOptionsDialog = getOptionsDialog(mContext);
		mOptionsDialog.show();
	}
	
	private static void takePicture() {
		if (mPictureUri == null) {
			File photoFile = UserProfile.getTemporaryPictureFile(mContext);
			if (photoFile == null) {
				UiUtils.toastLong(mContext, mContext.getString(R.string.ts_fullprofile_error_sdcard_absent));
				return;
			}
			mPictureUri = Uri.fromFile(photoFile);
		}
		
		// create Intent to take a picture and return control to the calling application
	    Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
	    takePicture.putExtra(MediaStore.EXTRA_OUTPUT, mPictureUri);
	    mActivity.startActivityForResult(takePicture, INTENT_REQUEST_PICTURE_TAKE);		
	}
	
	private static void cropPictureWithUri(Uri source, Uri destination) {
		new Crop(source).output(destination).asSquare().start(mActivity);
	}
	
	private static void pickPicture() {
		// start activity with intent to select and crop an image
		/* steps:
		 * -this method will get the file and if boolean argument is true, create it if non-existent
		 * -success: the temporary file will be renamed to a final one
		 * -failure/cancel: the temporary file will be deleted, keeping the final (new or previous)
		 * (for success or failure: #onActivityResult() )
		 */
		
		File file = UserProfile.getTemporaryPictureFile(mContext);
		
		if (file != null) {
			Crop.pickImage(mActivity);
		} else {
			UiUtils.toastLong(mContext, mContext.getString(R.string.ts_fullprofile_error_sdcard_absent));
		}
	}
	
	public final static void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != Activity.RESULT_OK) {
			return;
		}
		
		switch (requestCode) {
		case INTENT_REQUEST_PICTURE_PICK:
			if (data != null) {
				Uri uriPicked = data.getData();

				File temp = UserProfile.getTemporaryPictureFile(mContext);
	            temp.delete();
	            
	            Uri uriCropped = Uri.fromFile(temp);
	            
	            cropPictureWithUri(uriPicked, uriCropped);
			}
			break;
		case INTENT_REQUEST_PICTURE_TAKE:
			File taken = UserProfile.getTemporaryPictureFile(mContext);
			cropPictureWithUri(Uri.fromFile(taken), Uri.fromFile(taken));
			break;
		case INTENT_REQUEST_PICTURE_CROP:
			final File cropped = UserProfile.getTemporaryPictureFile(mContext);
			new AsyncTask<Void, Void, Void>() {

				@Override
				protected void onPreExecute() {
					mOptimizationDialog = getOptimizationDialog(mContext);
					mOptimizationDialog.show();
				}
				
				@Override
				protected Void doInBackground(Void... params) {
					UserProfile.setPicture(mContext, cropped);
					return null;
				}
				
				protected void onPostExecute(Void result) {					
					mOptimizationDialog.dismiss();
					Intent broadcast = new Intent(ACTION_PICTURE_SET);
					mContext.sendBroadcast(broadcast);
				}
				
			}.execute();
			break;
		}
	}
	
	private static AlertDialog getOptionsDialog(final Context context) {
		return new AlertDialog.Builder(context)
				.setTitle(R.string.ts_fullprofile_dialog_picturesource_title)
				.setMessage(R.string.ts_fullprofile_dialog_picturesource_message)
				.setPositiveButton(R.string.ts_fullprofile_dialog_picturesource_button_camera,
						new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						takePicture();
					}
				})
				.setNeutralButton(R.string.ts_fullprofile_dialog_picturesource_button_gallery,
						new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						pickPicture();
					}
				})
				.setNegativeButton(R.string.ts_common_cancel, null)
				.create();
	}
	
	private static ProgressDialog getOptimizationDialog(final Context context) {
		ProgressDialog d = new ProgressDialog(context);
		d.setTitle(R.string.ts_fullprofile_dialog_pictureoptimizing_title);
		d.setMessage(context.getString(R.string.ts_fullprofile_dialog_pictureoptimizing_message));
		d.setIndeterminate(true);
		d.setCancelable(false);
		return d;
	}
}
