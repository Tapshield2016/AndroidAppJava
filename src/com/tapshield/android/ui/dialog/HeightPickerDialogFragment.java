package com.tapshield.android.ui.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.tapshield.android.R;

public class HeightPickerDialogFragment extends DialogFragment implements OnClickListener {

	private int mMaxFeet = 6;
	private int mMinFeet = 1;
	private int mMaxInches = 11;
	private int mMinInches = 0;
	private int mValueFeet = (int) Math.ceil(mMaxFeet/2);
	private int mValueInches = (int) Math.ceil(mMaxInches/2);
	private int mTempValueFeet = mValueFeet;
	private int mTempValueInches = mValueInches;
	
	private Button mPlusFeet;
	private Button mPlusInches;
	private Button mMinusFeet;
	private Button mMinusInches;
	private TextView mFeet;
	private TextView mInches;
	
	private HeightPickerListener mListener;
	
	public HeightPickerDialogFragment() {}
	
	public HeightPickerDialogFragment setFeet(int feet) {
		mTempValueFeet = mValueFeet = feet;
		limitValues();
		return this;
	}
	
	public HeightPickerDialogFragment setInches(int inches) {
		mTempValueInches = mValueInches = inches;
		limitValues();
		return this;
	}
	
	public HeightPickerDialogFragment setListener(final HeightPickerListener l) {
		mListener = l;
		return this;
	}
	
	public int getFeet() {
		return mValueFeet;
	}
	
	public int getInches() {
		return mValueInches;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		View content = getActivity().getLayoutInflater().inflate(R.layout.dialog_heightpicker, null);
		
		mPlusFeet = (Button) content.findViewById(R.id.heightpicker_button_feet_plus);
		mMinusFeet = (Button) content.findViewById(R.id.heightpicker_button_feet_minus);
		mPlusInches = (Button) content.findViewById(R.id.heightpicker_button_inches_plus);
		mMinusInches = (Button) content.findViewById(R.id.heightpicker_button_inches_minus);
		
		mFeet = (TextView) content.findViewById(R.id.heightpicker_text_feet);
		mInches = (TextView) content.findViewById(R.id.heightpicker_text_inches);
		updateLabels();
		
		mPlusFeet.setOnClickListener(this);
		mMinusFeet.setOnClickListener(this);
		mPlusInches.setOnClickListener(this);
		mMinusInches.setOnClickListener(this);
		
		return new AlertDialog.Builder(getActivity())
				.setTitle(R.string.ts_dialog_heightpicker_title)
				.setView(content)
				.setPositiveButton(R.string.ts_dialog_heightpicker_button_set,
						new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								notifyListener();
							}
						})
				.setNegativeButton(R.string.ts_dialog_heightpicker_button_cancel, null)
				.create();
	}
	
	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.heightpicker_button_feet_plus:
			mTempValueFeet++;
			break;
		case R.id.heightpicker_button_feet_minus:
			mTempValueFeet--;
			break;
		case R.id.heightpicker_button_inches_plus:
			mTempValueInches++;
			break;
		case R.id.heightpicker_button_inches_minus:
			mTempValueInches--;
			break;
		}
		
		limitValues();
		updateLabels();
	}
	
	@Override
	public void onDismiss(DialogInterface dialog) {
		mTempValueFeet = mValueFeet;
		mTempValueInches = mValueInches;
		updateLabels();
		super.onDismiss(dialog);
	}
	
	public void show(Activity activity) {
		show(activity.getFragmentManager(), null);
	}
	
	private void limitValues() {
		if (mTempValueInches < mMinInches) {
			mTempValueInches = mMaxInches; //set max since feet will decrease
			mTempValueFeet--;
		} else if (mTempValueInches > mMaxInches) {
			mTempValueInches = 0;
			mTempValueFeet++;
		}
		
		if (mTempValueFeet > mMaxFeet) {
			mTempValueFeet = mMaxFeet;
		} else if (mTempValueFeet < mMinFeet) {
			mTempValueFeet = mMinFeet;
		}
	}
	
	private void updateLabels() {
		mFeet.setText(Integer.toString(mTempValueFeet));
		mInches.setText(Integer.toString(mTempValueInches));
	}
	
	private void notifyListener() {
		if (mListener != null) {
			if (mTempValueFeet != mValueFeet || mTempValueInches != mValueInches) {
				mValueFeet = mTempValueFeet;
				mValueInches = mTempValueInches;
				mListener.onHeightSet(getFeet(), getInches());
			}
		}
	}
	
	public interface HeightPickerListener {
		void onHeightSet(int feet, int inches);
	}
}
