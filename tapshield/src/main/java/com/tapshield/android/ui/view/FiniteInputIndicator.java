package com.tapshield.android.ui.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import com.tapshield.android.R;

public class FiniteInputIndicator extends View {

	private int mItemWidth = 0;
	private int mItemHeight = 0;
	private int mItemSpacing = 0;
	private int mItemOffsetVertical = 0;
	private int mItemOffsetHorizontal = 0;
	
	private int mSize = 0;
	private int mCounter = 0;
	
	private Drawable mDrawable;
	
	public FiniteInputIndicator(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	
	public FiniteInputIndicator(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		prepare(context);
	}
	
	private void prepare(final Context context) {
		Resources r = context.getResources();
		mDrawable = r.getDrawable(R.drawable.ts_button_rect);
	}
	
	public void setSize(int size) {
		mSize = size;
		requestLayout();
		invalidate();
	}
	
	public int getSize() {
		return mSize;
	}
	
	public void setCounter(int counter) {
		mCounter = counter;
		invalidate();
	}
	
	public int getCounter() {
		return mCounter;
	}
	
	public void increase() {
		mCounter++;
		invalidate();
	}
	
	public void decrease() {
		mCounter--;
		invalidate();
	}
	
	private boolean isEmpty() {
		return getSize() <= 0;
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		if (!isEmpty()) {
			int tw = w - getPaddingLeft() - getPaddingRight();
			int th = h - getPaddingTop() - getPaddingBottom();
			
			//n items with n-1 spaces in between
			mItemSpacing = mItemWidth = tw / (2 * mSize - 1);
			mItemHeight = th;

			//square item dimensions and set right offset
			if (mItemWidth < mItemHeight) {
				mItemOffsetVertical = (mItemHeight - mItemWidth)/2;
				mItemHeight = mItemWidth;
			} else if (mItemWidth > mItemHeight) {
				mItemOffsetHorizontal = (mItemWidth - mItemHeight)/2;
				mItemWidth = mItemHeight;
			}

		}
		super.onSizeChanged(w, h, oldw, oldh);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		if (isEmpty()) {
			return;
		}
		
		for (int i = 0; i < getSize(); i++) {
			int x = getPaddingLeft() + mItemOffsetHorizontal + (i * mItemWidth) + (i * mItemSpacing);
			int y = 0 + mItemOffsetVertical;
			
			//set state based on position i
			mDrawable.setState((i + 1 <= getCounter()) ? PRESSED_STATE_SET : EMPTY_STATE_SET);
			mDrawable.setBounds(x, y, x + mItemWidth, y + mItemHeight);
			mDrawable.draw(canvas);
		}
	}
}
