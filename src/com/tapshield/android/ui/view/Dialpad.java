package com.tapshield.android.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tapshield.android.R;

public class Dialpad extends LinearLayout implements OnClickListener {

	private static final long ERROR_SHOW_MILLI = 2000;
	
	private FiniteInputIndicator mIndicator;
	private TextView mMessage;
	
	private String mMessageContent;
	private int mInputNumber;
	private String mInput;
	private int mInputCounter;
	private DialpadListener mListener;
	
	public Dialpad(Context context) {
		this(context, null);
	}
	
	public Dialpad(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		mInputNumber = 4;
		mInputCounter = 0;
		mInput = new String();
		
		LayoutInflater.from(context).inflate(R.layout.view_dialpad, this, true);

		mMessage = (TextView) findViewById(R.id.view_dialpad_text_message);
		mMessageContent = mMessage.getText().toString();
		mIndicator = (FiniteInputIndicator) findViewById(R.id.view_dialpad_finiteinputindicator);
		setInputNumber(mInputNumber);
		
		SquareButton square0 = (SquareButton) findViewById(R.id.view_dialpad_button_0);
		SquareButton square1 = (SquareButton) findViewById(R.id.view_dialpad_button_1);
		SquareButton square2 = (SquareButton) findViewById(R.id.view_dialpad_button_2);
		SquareButton square3 = (SquareButton) findViewById(R.id.view_dialpad_button_3);
		SquareButton square4 = (SquareButton) findViewById(R.id.view_dialpad_button_4);
		SquareButton square5 = (SquareButton) findViewById(R.id.view_dialpad_button_5);
		SquareButton square6 = (SquareButton) findViewById(R.id.view_dialpad_button_6);
		SquareButton square7 = (SquareButton) findViewById(R.id.view_dialpad_button_7);
		SquareButton square8 = (SquareButton) findViewById(R.id.view_dialpad_button_8);
		SquareButton square9 = (SquareButton) findViewById(R.id.view_dialpad_button_9);
		SquareButton squareClear = (SquareButton) findViewById(R.id.view_dialpad_button_clear);
		SquareButton squareDelete = (SquareButton) findViewById(R.id.view_dialpad_button_delete);
		
		square0.setOnClickListener(this);
		square1.setOnClickListener(this);
		square2.setOnClickListener(this);
		square3.setOnClickListener(this);
		square4.setOnClickListener(this);
		square5.setOnClickListener(this);
		square6.setOnClickListener(this);
		square7.setOnClickListener(this);
		square8.setOnClickListener(this);
		square9.setOnClickListener(this);
		squareClear.setOnClickListener(this);
		squareDelete.setOnClickListener(this);
	}
	
	public void setInputNumber(int inputNumber) {
		mInputNumber = inputNumber;
		mIndicator.setSize(mInputNumber);
	}
	
	private void add(int number) {
		if (mInputCounter >= mInputNumber) {
			return;
		}
		
		String input = Integer.toString(number);
		mInput = mInput.concat(input);
		mInputCounter++;

		updateIndicator();
		
		mListener.onInputChange(mInput);
		
		if (mInputCounter >= mInputNumber) {
			notifyListener();
		}
	}
	
	private void back() {
		Log.i("dialpad", "counter=" + mInputCounter);
		if (mInputCounter <= 0) {
			return;
		} else if (mInputCounter == 1) {
			clear();
		} else {
			mInput = mInput.substring(0, mInputCounter - 1);
			mInputCounter--;
			updateIndicator();
		}
		
		mListener.onInputChange(mInput);
	}
	
	public void clear() {
		mInput = new String();
		mInputCounter = 0;
		updateIndicator();
		
		mListener.onInputChange(mInput);
	}
	
	private void updateIndicator() {
		mIndicator.setCounter(mInputCounter);
	}
	
	private void notifyListener() {
		if (mListener == null) {
			return;
		}
		
		mListener.onInputComplete(mInput);
	}
	
	public void setDialpadListener(DialpadListener l) {
		mListener = l;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.view_dialpad_button_0:
			add(0);
			break;
		case R.id.view_dialpad_button_1:
			add(1);
			break;
		case R.id.view_dialpad_button_2:
			add(2);
			break;
		case R.id.view_dialpad_button_3:
			add(3);
			break;
		case R.id.view_dialpad_button_4:
			add(4);
			break;
		case R.id.view_dialpad_button_5:
			add(5);
			break;
		case R.id.view_dialpad_button_6:
			add(6);
			break;
		case R.id.view_dialpad_button_7:
			add(7);
			break;
		case R.id.view_dialpad_button_8:
			add(8);
			break;
		case R.id.view_dialpad_button_9:
			add(9);
			break;
		case R.id.view_dialpad_button_clear:
			clear();
			break;
		case R.id.view_dialpad_button_delete:
			back();
			break;
		}
	}
	
	public void setError(int errorMessageStringResource) {
		setError(getResources().getString(errorMessageStringResource));
	}
	
	public void setError(String errorMessage) {
		clear();
		mMessage.setText(errorMessage);
		mMessage.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				mMessage.setText(mMessageContent);
			}
		}, ERROR_SHOW_MILLI);
	}

	public static interface DialpadListener {
		void onInputComplete(String input);
		void onInputChange(String newInput);
	}
}
