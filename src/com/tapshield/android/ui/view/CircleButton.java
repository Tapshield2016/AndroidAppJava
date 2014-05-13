package com.tapshield.android.ui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tapshield.android.R;

public class CircleButton extends RelativeLayout {

	private Button mButton;
	private TextView mLabel;
	
	public CircleButton(Context context) {
		this(context, null);
	}
	
	public CircleButton(Context context, AttributeSet attrs) {
		super(context, attrs);

		LayoutInflater.from(context).inflate(R.layout.view_circlebutton, this, true);
		mButton = (Button) findViewById(R.id.view_circlebutton_button);
		mLabel = (TextView) findViewById(R.id.view_circlebutton_text);
		ImageView image = (ImageView) findViewById(R.id.view_circlebutton_image);

		TypedArray a = context.getTheme().obtainStyledAttributes(
				attrs,
				R.styleable.CircleButton,
				0, 0);

		String text;
		int textSize;
		int textColor;
		int textColorStateList;
		int icon;
		int iconPadding;
		int iconPaddingLeft;
		int iconPaddingTop;
		int iconPaddingRight;
		int iconPaddingBottom;
		int background;
		
		try {
			text = a.getString(R.styleable.CircleButton_text);
			textColor = a.getColor(R.styleable.CircleButton_textColor, Color.WHITE);
			textColorStateList = a.getResourceId(R.styleable.CircleButton_textColorStateList, 0);
			icon = a.getResourceId(R.styleable.CircleButton_icon, 0);
			iconPadding = a.getDimensionPixelSize(R.styleable.CircleButton_iconPadding, 0);
			iconPaddingLeft = a.getDimensionPixelSize(R.styleable.CircleButton_iconPaddingLeft, 0);
			iconPaddingTop = a.getDimensionPixelSize(R.styleable.CircleButton_iconPaddingTop, 0);
			iconPaddingRight = a.getDimensionPixelSize(R.styleable.CircleButton_iconPaddingRight, 0);
			iconPaddingBottom = a.getDimensionPixelSize(R.styleable.CircleButton_iconPaddingBottom, 0);
			textSize = (int) a.getDimension(R.styleable.CircleButton_textSize, 12);
			background = a.getResourceId(R.styleable.CircleButton_background, 0);
		} finally {
			a.recycle();
		}
		
		mLabel.setText(text);
		mLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
		mLabel.setTextColor(textColor);
		mLabel.setTextColor(getResources().getColorStateList(textColorStateList));
		image.setImageResource(icon);
		mButton.setBackgroundResource(background);
		
		mLabel.setClickable(false);
		mLabel.setFocusable(false);
		mLabel.setFocusableInTouchMode(false);
		
		image.setClickable(false);
		image.setFocusable(false);
		image.setFocusableInTouchMode(false);
		image.setPadding(
				iconPaddingLeft > 0 ? iconPaddingLeft : iconPadding,
				iconPaddingTop > 0 ? iconPaddingTop : iconPadding,
				iconPaddingRight > 0 ? iconPaddingRight : iconPadding,
				iconPaddingBottom > 0 ? iconPaddingBottom : iconPadding);
		
		mButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				CircleButton.this.performClick();
			}
		});
	}
	
	@Override
	public void setEnabled(final boolean enabled) {
		
		Runnable action = new Runnable() {
			
			@Override
			public void run() {
				CircleButton.super.setEnabled(enabled);
				mButton.setEnabled(enabled);
				mLabel.setEnabled(enabled);
			}
		};
		
		post(action);
	}
}
