package com.tapshield.android.ui.activity;

import java.io.File;

import org.apache.commons.io.FileUtils;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.tapshield.android.R;
import com.tapshield.android.api.model.UserProfile;
import com.tapshield.android.utils.UiUtils;

import eu.janmuller.android.simplecropimage.CropImage;

public class FullProfileActivity extends Activity implements OnClickListener {

	private static final int INTENT_REQUEST_PICTURE_PICK = 1;
    private static final int INTENT_REQUEST_PICTURE_TAKE = 2;
	private static final int INTENT_REQUEST_PICTURE_CROP = 3;
	
	private ImageButton mPicture;
	private TextView mName;
	private Button mBasic;
	private Button mContact;
	private Button mAppearance;
	private Button mMedical;
	private Button mEmergencyContact;
	
	private Uri mPictureUri;
	private AlertDialog mPictureSource;
	private ProgressDialog mPictureOptimizing;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_fullprofile);
		
		mPicture = (ImageButton) findViewById(R.id.fullprofile_imagebutton_picture);
		mName = (TextView) findViewById(R.id.fullprofile_text_name);
		mBasic = (Button) findViewById(R.id.fullprofile_button_basic);
		mContact = (Button) findViewById(R.id.fullprofile_button_contact);
		mAppearance = (Button) findViewById(R.id.fullprofile_button_appearance);
		mMedical = (Button) findViewById(R.id.fullprofile_button_medical);
		mEmergencyContact = (Button) findViewById(R.id.fullprofile_button_emergency);
		
		mPicture.setOnClickListener(this);
		mBasic.setOnClickListener(this);
		mContact.setOnClickListener(this);
		mAppearance.setOnClickListener(this);
		mMedical.setOnClickListener(this);
		mEmergencyContact.setOnClickListener(this);
		
		mPictureSource = getPictureSourceDialog();
		mPictureOptimizing = getPictureOptimizingDialog();
		
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowHomeEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		String name = "John Doe";
		mName.setText(name);
		
		loadPicture();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			break;
		}
		return false;
	}
	
	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.fullprofile_imagebutton_picture:
			mPictureSource.show();
			break;
		case R.id.fullprofile_button_basic:
			break;
		case R.id.fullprofile_button_contact:
			break;
		case R.id.fullprofile_button_appearance:
			break;
		case R.id.fullprofile_button_medical:
			break;
		case R.id.fullprofile_button_emergency:
			break;
		}
	}
	
	private AlertDialog getPictureSourceDialog() {
		return new AlertDialog.Builder(this)
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
	
	private ProgressDialog getPictureOptimizingDialog() {
		ProgressDialog d = new ProgressDialog(this);
		d.setTitle(R.string.ts_fullprofile_dialog_pictureoptimizing_title);
		d.setMessage(getString(R.string.ts_fullprofile_dialog_pictureoptimizing_message));
		d.setIndeterminate(true);
		d.setCancelable(false);
		return d;
	}
	
	private void loadPicture() {
		if (!UserProfile.hasPicture(this)) {
			return;
		}
		
		Bitmap picture = UserProfile.getPicture(this);

		if (picture != null) {
			mPicture.setImageBitmap(picture);
		} else {
			mPicture.setImageResource(R.drawable.ic_launcher);
		}
	}
	
	private void takePicture() {
		if (mPictureUri == null) {
			File photoFile = UserProfile.getTemporaryPictureFile(this);
			if (photoFile == null) {
				UiUtils.toastLong(this, getString(R.string.ts_fullprofile_error_sdcard_absent));
				return;
			}
			mPictureUri = Uri.fromFile(photoFile);
		}
		
		// create Intent to take a picture and return control to the calling application
	    Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
	    takePicture.putExtra(MediaStore.EXTRA_OUTPUT, mPictureUri);
	    startActivityForResult(takePicture, INTENT_REQUEST_PICTURE_TAKE);		
	}
	
	private void pickPicture() {
		// start activity with intent to select and crop an image
		/* steps:
		 * -this method will get the file and if boolean argument is true, create it if non-existent
		 * -success: the temporary file will be renamed to a final one
		 * -failure/cancel: the temporary file will be deleted, keeping the final (new or previous)
		 * (for success or failure: #onActivityResult() )
		 */
		
		File file = UserProfile.getTemporaryPictureFile(this);
		
		if (file != null) {
			
			Intent intentPhotopicker = new Intent(Intent.ACTION_GET_CONTENT);
			intentPhotopicker.setType("image/*");
			startActivityForResult(Intent.createChooser(
					intentPhotopicker,
					getString(R.string.ts_fullprofile_picture_chooserintent_title)),
					INTENT_REQUEST_PICTURE_PICK);
		} else {
			UiUtils.toastLong(this, getString(R.string.ts_fullprofile_error_sdcard_absent));
		}
	}
	
	private void cropPictureFile(File file) {
		Intent cropper = new Intent(this, CropImage.class);
		cropper.putExtra(CropImage.IMAGE_PATH, file.getPath());
		cropper.putExtra(CropImage.SCALE, true);
		cropper.putExtra(CropImage.ASPECT_X, 1);
		cropper.putExtra(CropImage.ASPECT_Y, 1);
		
		startActivityForResult(cropper, INTENT_REQUEST_PICTURE_CROP);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		if (resultCode != RESULT_OK) {
			return;
		}
		
		switch (requestCode) {
		case INTENT_REQUEST_PICTURE_PICK:
			if (data != null) {
				Uri uri = data.getData();
				
		        String[] projection = { MediaStore.Images.Media.DATA };
		        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
		        
		        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		        cursor.moveToFirst();
	            String path = cursor.getString(column_index);
	            cursor.close();
	            
	            File picked = new File(path);
	            File temp = UserProfile.getTemporaryPictureFile(this);
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
			File temp = UserProfile.getTemporaryPictureFile(this);
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
					mPictureOptimizing.show();
				}
				
				@Override
				protected Void doInBackground(Void... params) {
					UserProfile.setPicture(FullProfileActivity.this, cropped);
					return null;
				}
				
				protected void onPostExecute(Void result) {					
					mPictureOptimizing.dismiss();
					loadPicture();
				}
				
			}.execute();
			break;
		}
		
		super.onActivityResult(requestCode, resultCode, data);
	}
}
