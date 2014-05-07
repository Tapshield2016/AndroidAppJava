package com.tapshield.android.ui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
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
	private int mColorBar;
	private int mColorHandle;
	private int mColorProgress;
	private int mSizeBar;
	private int mSizeHandle;
	private int mSizeProgress;
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
	private float mHandleX;
	private float mHandleY;
	private float mBackgroundArc = 0;
	private RectF mBackgroundRect;
	private float mStepAngle = 0;
	
	public CircleSeekBar(Context context) {
		this(context, null);
	}
	
	public CircleSeekBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		TypedArray a = context.getTheme().obtainStyledAttributes(
				attrs,
				R.styleable.CircleSeekBar,
				0, 0);
		
		try {
			mColorBar = a.getColor(R.styleable.CircleSeekBar_colorBar, Color.DKGRAY);
			mColorHandle = a.getColor(R.styleable.CircleSeekBar_colorHandle, Color.WHITE);
			mColorProgress = a.getColor(R.styleable.CircleSeekBar_colorProgress, Color.DKGRAY);
			
			mSizeBar = a.getDimensionPixelSize(R.styleable.CircleSeekBar_sizeBar, 0);
			mSizeHandle = a.getDimensionPixelSize(R.styleable.CircleSeekBar_sizeHandle, 0);
			mSizeProgress = a.getDimensionPixelSize(R.styleable.CircleSeekBar_sizeProgress, 0);
		} finally {
			a.recycle();
		}
		
		configPaint();
		configFps();
		measureStepAngle(getMax());
	}
	
	private void configPaint() {
		mPaintBar = new Paint(Paint.ANTI_ALIAS_FLAG);
		mPaintBar.setStyle(Paint.Style.STROKE);
		mPaintBar.setColor(mColorBar);
		mPaintBar.setStrokeWidth(mSizeBar);
		
		mPaintHandle = new Paint(Paint.ANTI_ALIAS_FLAG);
		mPaintHandle.setColor(mColorHandle);
		
		mPaintBackgroundArc = new Paint(Paint.ANTI_ALIAS_FLAG);
		mPaintBackgroundArc.setStyle(Paint.Style.STROKE);
		mPaintBackgroundArc.setColor(mColorProgress);
		mPaintBackgroundArc.setStrokeWidth(mSizeProgress);
	}
	
	public void setFps(int newFps) {
		mFps = newFps;
		configFps();
	}
	
	private void configFps() {
		mInvalidateFrequency = (long) 1000 / (long) mFps;
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
		
		mBackgroundRect = new RectF(
				mOffsetHorizontal,
				mOffsetVertical,
				mOffsetHorizontal + mDimension,
				mOffsetVertical + mDimension);
		
		setProgress(getProgress());
		super.onSizeChanged(w, h, oldw, oldh);
	}

	private boolean invalidateFpsLimited() {
		long now = SystemClock.elapsedRealtime();

		if (now >= mInvalidatedAt + mInvalidateFrequency) {
			mInvalidatedAt = now;
			invalidate();
			return true;
		}
		return false;
	}
	
	@Override
	protected synchronized void onDraw(Canvas canvas) {
		canvas.drawCircle(mCx, mCy, mRadius, mPaintBar);
		canvas.drawArc(mBackgroundRect, -90, mBackgroundArc, false, mPaintBackgroundArc);
		canvas.drawCircle(mHandleX, mHandleY, mSizeHandle, mPaintHandle);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		
		mTouchX = event.getX();
		mTouchY = event.getY();
		
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mAllowDrag = mTouchX <= mHandleX + mSizeHandle && mTouchX >= mHandleX - mSizeHandle
					&& mTouchY <= mHandleY + mSizeHandle && mTouchY >= mHandleY - mSizeHandle;
			
			if (mAllowDrag) {
				snap();
			}
			break;
		case MotionEvent.ACTION_MOVE:
			if (mAllowDrag && invalidateFpsLimited()) {
				snap();
			}
			break;
		case MotionEvent.ACTION_UP:
			if (mAllowDrag) {
				mAllowDrag = false;
				snap();
			}
			break;
		}
		
		return true;
	}
	
	@Override
	public synchronized void setProgress(int progress) {
		snapHandleToProgress(progress);
		super.setProgress(progress);
	}
	
	@Override
	public synchronized void setMax(int max) {
		measureStepAngle(max);
		super.setMax(max);
	}
	
	private void measureStepAngle(int max) {
		mStepAngle = 360 / max;
	}
	
	private void snap() {
		double dx = mTouchX - mCx;
		double dy = mCy - mTouchY;
		double angleRadians = Math.atan2(dy, dx);
		double angle = angleRadians * 180 / Math.PI;
		float angleFromY = getAngleFromYAxisClockwise(angle);
		
		int snappedStep = getSnappedStepWithAngle(angleFromY);
		int step = getProgress();
		if (snappedStep != step) {
			setProgress(snappedStep);
		}
	}
	
	private void snapHandleToProgress(int step) {
		float angleFromY = (mStepAngle / 2) + (mStepAngle * step);

		//reverse process of getAngleFromYAxisClockwise()
		float anglePreRadians = 0;
		if (angleFromY > 270) {
			anglePreRadians = 360 - angleFromY + 90;
		} else if (angleFromY > 90) {
			anglePreRadians = (-1) * (angleFromY - 90);
		} else {
			anglePreRadians = 90 - angleFromY;
		}
		
		double angleRadians = (double) anglePreRadians * Math.PI / 180d;
		mHandleX = mCx + (mRadius * (float) Math.cos(angleRadians));
		mHandleY = mCy - (mRadius * (float) Math.sin(angleRadians));
		mBackgroundArc = angleFromY;
	}
	
	private float getAngleFromYAxisClockwise(double angleFromYAxis) {
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
		
		if (angleFromYAxis < 0) {
			return 90 + (float) Math.abs(angleFromYAxis);
		} else if (angleFromYAxis > 90) {
			return 360 - (float) (angleFromYAxis - 90);
		} else {
			return 90 - (float) angleFromYAxis;
		}
	}
	
	private int getSnappedStepWithAngle(double angleFromYAxis) {
		return (int) (angleFromYAxis / mStepAngle);
	}
}
