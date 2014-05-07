package com.tapshield.android.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.SeekBar;

import com.tapshield.android.R;

public class CircleSeekBar extends SeekBar {

	private int mFps = 60;
	private long mInvalidateFrequency;
	private long mInvalidatedAt = 0;
	private Paint mPaintBar;
	private Paint mPaintHandle;
	private Paint mPaintBackgroundArc;
	
	private float mCx;
	private float mCy;
	private int mDimension = 0;
	private float mRadius;
	private float mOffsetHorizontal = 0;
	private float mOffsetVertical = 0;

	private boolean mAllowDrag = false;
	private float mTouchX;
	private float mTouchY;
	private float mHandleRadius = 80;
	private float mHandleX;
	private float mHandleY;
	private float mBackgroundArc = 0;
	private RectF mBackgroundRect;
	
	public CircleSeekBar(Context context) {
		this(context, null);
	}
	
	public CircleSeekBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		configPaint();
		configFps();
	}
	
	private void configPaint() {
		mPaintBar = new Paint(Paint.ANTI_ALIAS_FLAG);
		mPaintBar.setStyle(Paint.Style.STROKE);
		mPaintBar.setColor(getContext().getResources().getColor(R.color.ts_brand_dark));
		mPaintBar.setStrokeWidth(45);
		
		mPaintHandle = new Paint(Paint.ANTI_ALIAS_FLAG);
		mPaintHandle.setColor(getContext().getResources().getColor(R.color.ts_brand_light));
		
		mPaintBackgroundArc = new Paint(Paint.ANTI_ALIAS_FLAG);
		mPaintBackgroundArc.setColor(getContext().getResources().getColor(R.color.ts_alert_text_color));
		mPaintBackgroundArc.setStyle(Paint.Style.STROKE);
		mPaintBackgroundArc.setStrokeWidth(45);
	}
	
	public void setFps(int newFps) {
		mFps = newFps;
		configFps();
	}
	
	private void configFps() {
		mInvalidateFrequency = (long) 1000 / (long) mFps;
	}
	
	private void invalidateLimited() {
		long now = SystemClock.elapsedRealtime();

		if (now >= mInvalidatedAt + mInvalidateFrequency) {
			mInvalidatedAt = now;
			invalidate();
		}
	}
	
	@Override
	public void invalidate() {
		updateValues();
		super.invalidate();
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		
		int totalW = w - getPaddingLeft() - getPaddingRight();
		int totalH = h - getPaddingTop() - getPaddingBottom();
		
		mDimension = Math.min(totalW, totalH);
		
		mOffsetHorizontal = (w - mDimension) / 2;
		mOffsetVertical = (h - mDimension) / 2;

		mCx = mOffsetHorizontal + (mDimension / 2);
		mCy = mOffsetVertical + (mDimension / 2);
		
		mRadius = mDimension / 2;
	
		mHandleX = mCx;
		mHandleY = mCy;
		
		mBackgroundRect = new RectF(
				mOffsetHorizontal,
				mOffsetVertical,
				mOffsetHorizontal + mDimension,
				mOffsetVertical + mDimension);
		
		super.onSizeChanged(w, h, oldw, oldh);
	}
	
	@Override
	protected synchronized void onDraw(Canvas canvas) {
		canvas.drawCircle(mCx, mCy, mRadius, mPaintBar);
		canvas.drawArc(mBackgroundRect, -90, mBackgroundArc, false, mPaintBackgroundArc);
		canvas.drawCircle(mHandleX, mHandleY, mHandleRadius, mPaintHandle);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		
		mTouchX = event.getX();
		mTouchY = event.getY();
		
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mAllowDrag = mTouchX <= mHandleX + mHandleRadius && mTouchX >= mHandleX - mHandleRadius
					&& mTouchY <= mHandleY + mHandleRadius && mTouchY >= mHandleY - mHandleRadius;
			
			if (mAllowDrag) {
				invalidate();
			}
			break;
		case MotionEvent.ACTION_MOVE:
			if (mAllowDrag) {
				invalidateLimited();
			}
			break;
		case MotionEvent.ACTION_UP:
			if (mAllowDrag) {
				mAllowDrag = false;
				invalidate();
			}
			break;
		}
		
		return true;
	}
	
	private void updateValues() {
		double dx = mTouchX - mCx;
		double dy = mCy - mTouchY;
		double angleRadians = Math.atan2(dy, dx);
		double angle = angleRadians * 180 / Math.PI;
		
		mHandleX = mCx + (mRadius * (float) Math.cos(angleRadians));
		mHandleY = mCy - (mRadius * (float) Math.sin(angleRadians));
		
		/*
		 * the angle variable holds the value [0, 180] positive or negative
		 * defined by the x axis in quadrant 1, going up, positive, going down, negative.
		 * quadrant 1: [0, 90]
		 * quadrant 2: (90, 180]
		 * quadrant 4: [-90, 0]
		 * quadrant 3: [-180, 90)
		 * 
		 * 3 cases revised
		 * 1. when angle is negative (lower quadrants)
		 * 2. first quadrant has to have the inverted angle
		 * 3. second quadrant, 360 (full) - offset angle by -90)
		 * 
		 */
		
		Log.i("aaa", "angle=" + angle);
		
		if (angle < 0) {
			mBackgroundArc = 90 + (float) Math.abs(angle);
		} else if (angle > 90) {
			mBackgroundArc = 360 - (float) (angle - 90);
		} else {
			mBackgroundArc = 90 - (float) angle;
		}
	}
}
