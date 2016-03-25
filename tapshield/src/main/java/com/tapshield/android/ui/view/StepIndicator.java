package com.tapshield.android.ui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.tapshield.android.R;

public class StepIndicator extends View {

	private static final int DEFAULT_COLOR_INCOMPLETE = Color.GRAY;
	private static final int DEFAULT_COLOR_COMPLETE = Color.GREEN;
	
	private int mNumSteps = 3;
	private int mCurrentStep = 1;
	private int mColorIncomplete;
	private int mColorComplete;
	private int mLineToCircleRatio = 5;
	private float mLineThicknessToCircleRatio = 0.3f;
	
	private Paint mPaintComplete;
	private Paint mPaintIncomplete;
	
	private float mStepWidth;
	private float mStepHeight;
	private float mStepRadius;
	private float mStepOffsetVertical;
	private float mStepOffsetHorizontal;
	private float mCenterY;
	private float mCenterXStatic;
	private float mCenterXVariableToIndex;
	
	public StepIndicator(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		TypedArray a = context.getTheme().obtainStyledAttributes(
				attrs,
				R.styleable.StepIndicator,
				0,
				0);
		
		try {
			mColorIncomplete = a.getColor(
					R.styleable.StepIndicator_colorIncomplete,
					DEFAULT_COLOR_INCOMPLETE);
			mColorComplete = a.getColor(
					R.styleable.StepIndicator_colorComplete,
					DEFAULT_COLOR_COMPLETE);
		} finally {
			a.recycle();
		}
		
		init();
	}
	
	private void init() {
		mPaintIncomplete = new Paint(Paint.ANTI_ALIAS_FLAG);
		mPaintIncomplete.setColor(mColorIncomplete);
		mPaintIncomplete.setStyle(Paint.Style.FILL);
		
		mPaintComplete = new Paint(Paint.ANTI_ALIAS_FLAG);
		mPaintComplete.setColor(mColorComplete);
		mPaintComplete.setStyle(Paint.Style.FILL);
	}

	public void setColorComplete(int colorComplete) {
		mColorComplete = colorComplete;
		init();
		invalidate();
	}
	
	public void setColorIncomplete(int colorIncomplete) {
		mColorIncomplete = colorIncomplete;
		init();
		invalidate();
	}
	
	public void setNumSteps(int numSteps) {
		mNumSteps = numSteps;
		requestLayout();
		invalidate();
	}
	
	public void setCurrentStep(int currentStep) {
		mCurrentStep = currentStep;
		invalidate();
	}
	
	/**
	 * Ratio that defines the width of the line and circle. Example: If 2 is given,
	 * it means the width of the step-connecting line will double (be 2 times)
	 * the diameter of the circle representing the step.
	 * <br>
	 * <strong>Note:</strong> the circles might be smaller than intended if the height of the view 
	 * is smaller than the suggested dimension.
	 * @param newLineToCircleRatio value to represent the line-to-circle ratio
	 */
	public void setLineToCircleRatio(int newLineToCircleRatio) {
		mLineToCircleRatio = newLineToCircleRatio;
		requestLayout();
		invalidate();
	}
	
	/**
	 * Ratio that defines the thickness of the step-connecting line based on the diameter of the
	 * circle. Example: If 0.5 is given, it means the width of the step-connecting line will be half
	 * of the diameter of the circle representing the step.
	 * <br>
	 * <strong>Note:</strong> the circles might be smaller than intended if the height of the view 
	 * is smaller than the suggested dimension.
	 * @param newLineThicknessToCircleRatio value to represent the lineThickness-to-circle ratio
	 */
	public void setLineThicknessToCircleRatio(int newLineThicknessToCircleRatio) {
		mLineThicknessToCircleRatio = newLineThicknessToCircleRatio;
		invalidate();
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		
		if (mNumSteps > 0) {
			float totalWidth = (float) (w - getPaddingLeft() - getPaddingRight());
			float totalHeight = (float) (h - getPaddingTop() - getPaddingBottom());
			
			//parts = num of steps + (num of space between all steps * line-to-circle ratio)
			int parts = mNumSteps + ((mNumSteps - 1) * mLineToCircleRatio);
			mStepWidth = totalWidth / parts;
			mStepHeight = totalHeight;
			float diameter = Math.min(mStepWidth, mStepHeight);
			mStepRadius = diameter / 2;
			
			if (diameter < mStepHeight) {
				mStepOffsetVertical = (float) ((mStepHeight - diameter) / 2);
			}
			
			if (diameter < mStepWidth) {
				mStepOffsetHorizontal = (float) ((mStepWidth - diameter) / 2);
			}
			
			mCenterY = getPaddingTop() + mStepOffsetVertical + mStepRadius;
			mCenterXStatic = getPaddingLeft() + mStepOffsetHorizontal + mStepRadius;
			//step width times the ratio + 1 (+1 since there is an extra
			//  'step width'/2 or radius in the start and end
			mCenterXVariableToIndex = mStepWidth * (mLineToCircleRatio + 1);
		}
		
		super.onSizeChanged(w, h, oldw, oldh);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		if (mNumSteps <= 0) {
			return;
		}
		
		float lineThickness = mLineThicknessToCircleRatio * 2 * mStepRadius;
		
		if (lineThickness <= 0) {
			lineThickness = 1;
		}
		
		//first draw any step-connecting line if not the first 0-indexes step (starting at 1)
		for (int i = 1; i < mNumSteps; i++) {
			canvas.drawRect(
					mCenterXStatic + (mCenterXVariableToIndex * (i - 1)),
					mCenterY - (lineThickness / 2),
					mCenterXStatic + (mCenterXVariableToIndex * i),
					mCenterY + (lineThickness / 2),
					i <= mCurrentStep ? mPaintComplete : mPaintIncomplete);
		}

		//now draw the circles on top of the lines
		for (int i = 0; i < mNumSteps; i++) {
			canvas.drawCircle(
					mCenterXStatic + (mCenterXVariableToIndex * i),
					mCenterY,
					mStepRadius,
					i <= mCurrentStep ? mPaintComplete : mPaintIncomplete);
		}
	}
}
