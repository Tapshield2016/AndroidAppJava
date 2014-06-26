package com.tapshield.android.ui.view;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class AnimatedVerticalColorProgress extends View
		implements AnimatorListener, AnimatorUpdateListener {

	private static final long FPS = 25;
	private static final long UPDATE_MILLI = 1000/FPS; 
	
	private Paint mPaint;
	private float mHeightPercent = 0;
	private Rect mRectangle;
	
	private Runnable mUpdater;
	private ValueAnimator mAnimator;
	private int[] mColors;
	private int mCurrentColor;
	private boolean mRunning = false;
	private boolean mCancelled = false;
	private boolean mAttemptedHardwareAcceleration = false;
	
	private Listener mListener;
	
	public AnimatedVerticalColorProgress(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	
	public AnimatedVerticalColorProgress(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}
	
	public void start(long duration, String... hexColors) {
		start(duration, 0, hexColors);
	}
	
	public void start(long duration, int[] colors) {
		start(duration, 0, colors);
	}
	
	public void start(long duration, long startAt, String... hexColors) {
		int[] colors = new int[hexColors.length];
		
		for (int i = 0; i < colors.length; i++) {
			colors[i] = Color.parseColor(hexColors[i]);
		}
		
		start(duration, startAt, colors);
	}
	
	public void start(long duration, long startAt, int[] colors) {
		mColors = colors;
		
		mUpdater = new Runnable() {
			
			@Override
			public void run() {
				update();
			}
		};
		
		mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		
		mAnimator = ObjectAnimator.ofInt(mColors);
		mAnimator.setDuration(duration);
		mAnimator.setEvaluator(new ArgbEvaluator());
		mAnimator.addListener(this);
		mAnimator.addUpdateListener(this);
		mAnimator.start();
		
		if (startAt > 0 && startAt <= mAnimator.getDuration()) {
			mAnimator.setCurrentPlayTime(startAt);
		}
	}
	
	public void cancel() {
		if (mAnimator != null) {
			mAnimator.cancel();
		}
	}
	
	public void end() {
		mAnimator.end();
		mHeightPercent = 100;
		update();
	}
	
	private boolean wasCancelled() {
		return mCancelled;
	}
	
	private void update() {
		int top = (int) ((100 - mHeightPercent) * getHeight() / 100); 
		mRectangle = new Rect(0, top, getWidth(), getHeight());
		
		invalidate();
		if (mRunning) {
			postDelayed(mUpdater, UPDATE_MILLI);
		}
	}

	@Override
	public void onAnimationUpdate(ValueAnimator value) {
		mCurrentColor = (Integer) value.getAnimatedValue();
		mPaint.setColor(mCurrentColor);
		mHeightPercent = ((float)value.getCurrentPlayTime() / (float)value.getDuration()) * 100f;
	}

	@Override
	public void onAnimationCancel(Animator arg0) {
		mCancelled = true;
		mRunning = false;
	}

	@Override
	public void onAnimationEnd(Animator arg0) {
		if (!wasCancelled()) {
			mRunning = false;
			invalidate();
			notifyListener();
		}
	}

	@Override
	public void onAnimationRepeat(Animator arg0) {}

	@Override
	public void onAnimationStart(Animator arg0) {
		mCancelled = false;
		mRunning = true;
		invalidate();
		update();
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		update();
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		if (!mAttemptedHardwareAcceleration && !canvas.isHardwareAccelerated()) {
			setLayerType(LAYER_TYPE_HARDWARE, mPaint);
			mAttemptedHardwareAcceleration = true;
		}
		canvas.drawRect(mRectangle, mPaint);
	}
	
	public void setListener(Listener l) {
		mListener = l;
	}
	
	public void removeListener(Listener l) {
		if (mListener.equals(l)) {
			mListener = null;
		}
	}
	
	private void notifyListener() {
		if (mListener != null) {
			mListener.onEnd();
		}
	}
	
	public static interface Listener {
		void onEnd();
	}
}
