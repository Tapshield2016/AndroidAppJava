package com.tapshield.android.utils;

import java.io.File;

import org.apache.commons.io.FileUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;

import com.tapshield.android.R;
import com.tapshield.android.api.model.UserProfile;

import eu.janmuller.android.simplecropimage.CropImage;

public class PictureSetter {

	public static final String ACTION_PICTURE_SET = "com.tapshield.android.intent.ACTION_PICTURE_SETTER_SET";
	
	private static final int INTENT_REQUEST_PICTURE_PICK = 1;
    private static final int INTENT_REQUEST_PICTURE_TAKE = 2;
	private static final int INTENT_REQUEST_PICTURE_CROP = 3;
	
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
	
	private static void cropPictureFile(File file) {
		Intent cropper = new Intent(mContext, CropImage.class);
		cropper.putExtra(CropImage.IMAGE_PATH, file.getPath());
		cropper.putExtra(CropImage.SCALE, true);
		cropper.putExtra(CropImage.ASPECT_X, 1);
		cropper.putExtra(CropImage.ASPECT_Y, 1);
		
		mActivity.startActivityForResult(cropper, INTENT_REQUEST_PICTURE_CROP);
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
			
			Intent intentPhotopicker = new Intent(Intent.ACTION_GET_CONTENT);
			intentPhotopicker.setType("image/*");
			mActivity.startActivityForResult(Intent.createChooser(
					intentPhotopicker,
					mContext.getString(R.string.ts_fullprofile_picture_chooserintent_title)),
					INTENT_REQUEST_PICTURE_PICK);
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
				Uri uri = data.getData();
				
		        String[] projection = { MediaStore.Images.Media.DATA };
		        Cursor cursor = mContext.getContentResolver().query(uri, projection, null, null, null);
		        
		        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		        cursor.moveToFirst();
	            String path = cursor.getString(column_index);
	            cursor.close();
	            
	            File picked = new File(path);
	            File temp = UserProfile.getTemporaryPictureFile(mContext);
	            temp.delete();
	            try {
	            	FileUtils.copyFile(picked, temp);
	            } catch (Exception e) {
	            } finally {
	            	cropPictureFile(temp);
	            }
			}
			break;
		case INTENT_REQUEST_PICTURE_TAKE:
			File temp = UserProfile.getTemporaryPictureFile(mContext);
			cropPictureFile(temp);
			break;
		case INTENT_REQUEST_PICTURE_CROP:
			String path = data.getStringExtra(CropImage.IMAGE_PATH);
			
			if (path == null) {
				return;
			}
			
			final File cropped = new File(path);
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
