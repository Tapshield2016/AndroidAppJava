package com.tapshield.android.ui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tapshield.android.R;

public class CircleButton extends RelativeLayout {

	public CircleButton(Context context) {
		this(context, null);
	}
	
	public CircleButton(Context context, AttributeSet attrs) {
		super(context, attrs);

		LayoutInflater.from(context).inflate(R.layout.view_circlebutton, this, true);
		Button button = (Button) findViewById(R.id.view_circlebutton_button);
		TextView label = (TextView) findViewById(R.id.view_circlebutton_text);
		ImageView image = (ImageView) findViewById(R.id.view_circlebutton_image);

		TypedArray a = context.getTheme().obtainStyledAttributes(
				attrs,
				R.styleable.CircleButton,
				0, 0);

		String text;
		int textSize;
		int textColor;
		int icon;
		int background;
		
		try {
			text = a.getString(R.styleable.CircleButton_text);
			textColor = a.getColor(R.styleable.CircleButton_textColor, Color.WHITE);
			icon = a.getResourceId(R.styleable.CircleButton_icon, 0);
			textSize = (int) a.getDimension(R.styleable.CircleButton_textSize, 12);
			background = a.getResourceId(R.styleable.CircleButton_background, 0);
		} finally {
			a.recycle();
		}
		
		label.setText(text);
		label.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
		label.setTextColor(textColor);
		image.setImageResource(icon);
		button.setBackgroundResource(background);
		
		label.setClickable(false);
		label.setFocusable(false);
		label.setFocusableInTouchMode(false);
		
		image.setClickable(false);
		image.setFocusable(false);
		image.setFocusableInTouchMode(false);
		
		button.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				CircleButton.this.performClick();
			}
		});
	}
}
