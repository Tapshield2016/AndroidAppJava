package com.tapshield.android.ui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.tapshield.android.R;

public class PageIndicator extends View {

	private static final int DEFAULT_COLOR = Color.parseColor("#FF444444");
	private static final float DEFAULT_STROKE_PERCENT = 30f;
	private static final float DEFAULT_MIN_STROKE_PERCENT = 5f;
	private static final float DEFAULT_MAX_STROKE_PERCENT = 100f;
	
	private int mNumPages = 0;
	private int mSelectedPage = 0;
	private int mColor;
	private float mStrokePercent;
	private Paint mPaintStroke;
	private Paint mPaintFill;
	
	private int mParts = 0;
	private float mPartWidth = 0;
	private float mPartHeight = 0;
	private float mRadius = 0;
	private float mOffsetHorizontal = 0;
	private float mOffsetVertical = 0;
	private float mCenterY = 0;
	private float mCenterXStatic = 0;
	private float mCenterXVariableToIndex = 0;
	
	public PageIndicator(Context context, AttributeSet attrs) {
		super(context, attrs);

		setFocusable(false);
		setFocusableInTouchMode(false);
		
		TypedArray a = context.getTheme().obtainStyledAttributes(
				attrs,
				R.styleable.PageIndicator,
				0,
				0);
		
		try {
			mColor = a.getColor(
					R.styleable.PageIndicator_color,
					DEFAULT_COLOR);
			mStrokePercent = a.getFloat(
					R.styleable.PageIndicator_strokePercentOfDiameter,
					DEFAULT_STROKE_PERCENT);
		} finally {
			a.recycle();
		}

		if (mStrokePercent > DEFAULT_MAX_STROKE_PERCENT) {
			mStrokePercent = DEFAULT_MAX_STROKE_PERCENT;
		}
		
		if (mStrokePercent < DEFAULT_MIN_STROKE_PERCENT) {
			mStrokePercent = DEFAULT_MIN_STROKE_PERCENT;
		}
		
		initialize();
	}
	
	private void initialize() {
		mPaintStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
		mPaintStroke.setColor(mColor);
		mPaintStroke.setStyle(Paint.Style.STROKE);
		
		mPaintFill = new Paint(Paint.ANTI_ALIAS_FLAG);
		mPaintFill.setColor(mColor);
		mPaintFill.setStyle(Paint.Style.FILL_AND_STROKE);
	}
	
	public void setColor(String hexColor) {
		setColor(Color.parseColor(hexColor));
	}
	
	public void setColor(int color) {
		mColor = color;
		initialize();
		invalidate();
		requestLayout();
	}
	
	public void setNumPages(int numPages) {
		if (mNumPages == numPages) {
			return;
		}
		
		mNumPages = numPages;
		invalidate();
		requestLayout();
	}
	
	public void setSelectedPage(int selectedPage) {
		if (mSelectedPage == selectedPage) {
			return;
		}

		mSelectedPage = selectedPage;
		
		if (mSelectedPage < 0 || mSelectedPage >= mNumPages) {
			throw new IndexOutOfBoundsException("Selected " + mSelectedPage + " out of " + mNumPages);
		}
		
		invalidate();
		requestLayout();
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		
		if (mNumPages > 0) {
			
			float totalWidth = (float) (w - getPaddingLeft() - getPaddingRight());
			float totalHeight = (float) (h - getPaddingTop() - getPaddingBottom());
			
			//parts, plus (parts -1) spaces in between
			mParts = (2 * mNumPages) - 1;
			mPartWidth = totalWidth / mParts;
			mPartHeight = totalHeight;
			float diameter = Math.min(mPartHeight, mPartWidth);
			mRadius = diameter/2;
			
			mPaintStroke.setStrokeWidth((float) (mStrokePercent * diameter / 100));
			
			if (diameter < mPartHeight) {
				mOffsetVertical = (float) ((mPartHeight - diameter)/2);
			}
			
			if (diameter < mPartWidth) {
				mOffsetHorizontal = (float) ((mPartWidth - diameter)/2);
			}
			
			mCenterY = getPaddingTop() + mOffsetVertical + mRadius;
			mCenterXStatic = getPaddingLeft() + mOffsetHorizontal + mRadius;
			mCenterXVariableToIndex = mPartWidth * 2;
		}
		
		super.onSizeChanged(w, h, oldw, oldh);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		for (int i=0; i < mNumPages; i++) {
				//indicator fill if selected
				if (mSelectedPage == i) {
					canvas.drawCircle(mCenterXStatic + (mCenterXVariableToIndex * i),
							mCenterY, mRadius, mPaintFill);
				}
				
				//indicator stroke
				canvas.drawCircle(mCenterXStatic + (mCenterXVariableToIndex * i),
						mCenterY, mRadius, mPaintStroke);
		}
	}
}
