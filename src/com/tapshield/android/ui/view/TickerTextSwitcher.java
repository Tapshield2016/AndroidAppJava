package com.tapshield.android.ui.view;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher.ViewFactory;

public class TickerTextSwitcher extends TextSwitcher implements ViewFactory {

	private List<String> mMessages;
	private int mIndex;
	private Runnable mUpdater;
	private ViewFactory mFactory;
	private long mDelay;
	private boolean mRunning;
	private boolean mOneTimeUpdate;
	
	public TickerTextSwitcher(Context context, AttributeSet attrs) {
		super(context, attrs);
		mMessages = new ArrayList<String>();
		mIndex = -1;
		mRunning = false;
		mOneTimeUpdate = false;
		mUpdater = new Runnable() {
			
			@Override
			public void run() {
				if (!isRunning() || mMessages.isEmpty()) {
					return;
				}
				Log.i("bbb", "update");
				//make sure to setText() if first time, if not, check for size of list
				if (mOneTimeUpdate || mMessages.size() > 1) {
					Log.i("bbb", "ticker index=" + mIndex);
					mIndex = mIndex >= mMessages.size() - 1 ? 0 : mIndex + 1;
					setText(mMessages.get(mIndex));
				}
				
				if (mOneTimeUpdate) {
					mOneTimeUpdate = false;
				}
				
				postDelayed(mUpdater, mDelay);
			}
		};
	}
	
	private void setRunning(boolean running) {
		mRunning = running;
	}
	
	private boolean isRunning() {
		return mRunning;
	}
	
	public void set(long delayMilliseconds) {
		set(null, delayMilliseconds);
	}
	
	public void set(ViewFactory factory, long delayMilliseconds) {
		if (isRunning()) {
			return;
		}
		
		setRunning(true);
		mDelay = delayMilliseconds;
		mFactory = factory;
		
		//if absent factory, use default internal factory
		if (mFactory == null) {
			//general dark background to match default implementation
			setBackgroundColor(Color.parseColor("#99000000"));
			mFactory = this;
		}
		
		setFactory(mFactory);
	}
	
	private void schedule() {
		post(mUpdater);
	}
	
	private void unschedule() {
		removeCallbacks(mUpdater);
	}
	
	public void stop() {
		if (!isRunning()) {
			return;
		}
		
		setRunning(false);
		
		removeCallbacks(mUpdater);
	}
	
	public void hide() {
		cancelOldAnimation();
		unschedule();
		setVisibility(View.INVISIBLE);
		startAnimation(getOutAnimation());
	}
	
	public void show() {
		cancelOldAnimation();
		setVisibility(View.VISIBLE);
		startAnimation(getInAnimation());
		//when invisible, the scheduled updates are stopped--restart
		schedule();
	}
	
	private void cancelOldAnimation() {
		Animation old = getAnimation();
		if (old != null) {
			old.cancel();
		}
	}

	public void addText(int textResourceId) {
		addText(getContext().getString(textResourceId));
	}
	
	public void addText(String text) {
		if (!mMessages.contains(text)) {
			mMessages.add(text);
			
			//if by adding, there is just one item, and is running, show has to be called with a
			// one-time update
			if (mMessages.size() == 1 && mRunning) {
				show();
				mOneTimeUpdate = true;
			}
		}
	}
	
	public void removeText(int textResourceId) {
		removeText(getContext().getString(textResourceId));
	}
	
	public void removeText(String text) {
		int at = mMessages.indexOf(text);
		if (at >= 0) {
			if (mMessages.remove(text)) {
				if (mIndex >= at) {
					mIndex--;
				}
				
				//request a one-time update since an item has been removed
				mOneTimeUpdate = true;

				removeCheck();
			}
		}
	}
	
	public void clearText() {
		if (mMessages.isEmpty()) {
			return;
		}
		
		mMessages.clear();
		removeCheck();
	}
	
	//hide if empty list of messages
	private void removeCheck() {
		if (mMessages.isEmpty()) {
			hide();
		}
	}

	@Override
	public View makeView() {
		TextView view = new TextView(getContext());
		view.setGravity(Gravity.CENTER);
		view.setTextColor(Color.WHITE);
		view.setPadding(10, 10, 10, 10);
		view.setTextSize(15);
		return view;
	}
}
