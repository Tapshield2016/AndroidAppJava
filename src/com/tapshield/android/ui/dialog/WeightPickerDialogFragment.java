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

public class WeightPickerDialogFragment extends DialogFragment implements OnClickListener {

	private int mTitleRes;
	private int mMax = 400;
	private int mMin = 50;
	private int mValue = (int) Math.ceil(mMax/2);
	private int mTempValue = mValue;
	
	private Button mPlus100;
	private Button mPlus10;
	private Button mPlus1;
	private Button mMinus100;
	private Button mMinus10;
	private Button mMinus1;
	private TextView mLabel;
	private WeightPickerListener mListener;
	
	public WeightPickerDialogFragment() {}

	public WeightPickerDialogFragment setTitle(int titleRes) {
		mTitleRes = titleRes;
		return this;
	}
	
	public WeightPickerDialogFragment setMax(int max) {
		mMax = max;
		return this;
	}
	
	public WeightPickerDialogFragment setMin(int min) {
		mMin = min;
		return this;
	}
	
	public WeightPickerDialogFragment setValue(int value) {
		mValue = mTempValue = value;
		limitValue();
		return this;
	}
	
	public WeightPickerDialogFragment setListener(final WeightPickerListener l) {
		mListener = l;
		return this;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		View content = getActivity().getLayoutInflater().inflate(R.layout.dialog_weightpicker, null);
		
		mPlus100 = (Button) content.findViewById(R.id.weightpicker_button_top1);
		mPlus10 = (Button) content.findViewById(R.id.weightpicker_button_top2);
		mPlus1 = (Button) content.findViewById(R.id.weightpicker_button_top3);
		mMinus100 = (Button) content.findViewById(R.id.weightpicker_button_bottom1);
		mMinus10 = (Button) content.findViewById(R.id.weightpicker_button_bottom2);
		mMinus1 = (Button) content.findViewById(R.id.weightpicker_button_bottom3);
		
		mLabel = (TextView) content.findViewById(R.id.weightpicker_text);
		updateLabel();
		
		mPlus100.setOnClickListener(this);
		mPlus10.setOnClickListener(this);
		mPlus1.setOnClickListener(this);
		mMinus100.setOnClickListener(this);
		mMinus10.setOnClickListener(this);
		mMinus1.setOnClickListener(this);
		
		return new AlertDialog.Builder(getActivity())
				.setTitle(mTitleRes)
				.setView(content)
				.setPositiveButton(R.string.ts_dialog_weightpicker_button_set,
						new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						notifyListener();
					}
				})
				.setNegativeButton(R.string.ts_dialog_weightpicker_button_cancel, null)
				.create();
	}
	
	@Override
	public void onDismiss(DialogInterface dialog) {
		mTempValue = getValue();
		updateLabel();
		super.onDismiss(dialog);
	}
	
	public void show(Activity activity) {
		show(activity.getFragmentManager(), null);
	}
	
	public int getValue() {
		return mValue;
	}
	
	public int getMax() {
		return mMax;
	}
	
	public int getMin() {
		return mMin;
	}
	
	private void notifyListener() {
		if (mListener != null && mTempValue != mValue) {
			mValue = mTempValue;
			mListener.onWeightSet(getValue());
		}
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.weightpicker_button_top1:
			mTempValue = mTempValue + 100;
			break;
		case R.id.weightpicker_button_top2:
			mTempValue = mTempValue + 10;
			break;
		case R.id.weightpicker_button_top3:
			mTempValue = mTempValue + 1;
			break;
		case R.id.weightpicker_button_bottom1:
			mTempValue = mTempValue - 100;
			break;
		case R.id.weightpicker_button_bottom2:
			mTempValue = mTempValue - 10;
			break;
		case R.id.weightpicker_button_bottom3:
			mTempValue = mTempValue - 1;
			break;
		}
		
		limitValue();
		updateLabel();
	}
	
	private void limitValue() {
		if (mTempValue < mMin) {
			mTempValue = mMin;
		} else if (mTempValue > mMax) {
			mTempValue = mMax;
		}
	}
	
	private void updateLabel() {
		mLabel.setText(Integer.toString(mTempValue));
	}
	
	public interface WeightPickerListener {
		void onWeightSet(int pounds);
	}
}
