package com.tapshield.android.ui.activity;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.manager.EmergencyManager;
import com.tapshield.android.utils.UiUtils;

public class EmergencyActivity extends FragmentActivity {

	private EmergencyManager mEmergencyManager;
	
	private ProgressBar mProgressBar;
	private EditText mDisarm;
	private Button mCall;
	
	private TextWatcher mDisarmWatcher;
	private ValueAnimator mProgressAnimator;
	
	@Override
	protected void onCreate(Bundle b) {
		super.onCreate(b);
		setContentView(R.layout.activity_emergency);
		
		mEmergencyManager = EmergencyManager.getInstance(this);
		mProgressBar = (ProgressBar) findViewById(R.id.emergency_progressbar);
		mDisarm = (EditText) findViewById(R.id.emergency_edit_disarm);
		mCall = (Button) findViewById(R.id.emergency_button_twilio);
		
		mProgressBar.setMax(100);
		
		mDisarmWatcher = new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
				if (cs != null && cs.length() == 4) {
					JavelinClient javelin = JavelinClient.getInstance(EmergencyActivity.this,
							TapShieldApplication.JAVELIN_CONFIG);
					String disarmCode = javelin.getUserManager().getUser().getDisarmCode();
					if (disarmCode.equals(cs.toString())) {
						mEmergencyManager.cancel();
						UiUtils.startActivityNoStack(EmergencyActivity.this, MainActivity.class);
					} else {
						mDisarm.setText(new String());
						mDisarm.setError("wrong code");
					}
				}
			}
			
			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
			
			@Override
			public void afterTextChanged(Editable arg0) {}
		};
		
		mDisarm.addTextChangedListener(mDisarmWatcher);
		
		mCall.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				boolean started = mEmergencyManager.isTransmitting();
				
				if (!started) {
					mEmergencyManager.cancel();
					mEmergencyManager.startNow(EmergencyManager.TYPE_START_REQUESTED);
					mProgressAnimator.cancel();
				} else {
					mEmergencyManager.requestRedial();
				}
			}
		});
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		boolean scheduledOrStarted = mEmergencyManager.isRunning();
		
		if (scheduledOrStarted) {
			long remaining = mEmergencyManager.getRemaining();
			boolean started = remaining <= 0;
			
			if (started) {
				mProgressBar.setProgress(100);
			} else {
				long total = mEmergencyManager.getDuration();
				long elapsed = mEmergencyManager.getElapsed();
				
				final int animStart = (int) (elapsed / total);
				
				//progressbar range from 0 to 100
				mProgressAnimator = ValueAnimator.ofInt(animStart, 100);
				mProgressAnimator.setDuration(remaining);
				
				mProgressAnimator.addListener(new AnimatorListener() {
					
					@Override
					public void onAnimationStart(Animator animation) {
						mProgressBar.setProgress(animStart);
					}
					
					@Override
					public void onAnimationRepeat(Animator animation) {}
					
					@Override
					public void onAnimationEnd(Animator animation) {
						mProgressBar.setProgress(100);
					}
					
					@Override
					public void onAnimationCancel(Animator animation) {
						boolean started = mEmergencyManager.isRunning();
						//if cancelled, get whether it is running or not to set progress to 0 or 100
						int progress = started ? 100 : 0;
						mProgressBar.setProgress(progress);
					}
				});
				
				mProgressAnimator.addUpdateListener(new AnimatorUpdateListener() {
					
					@Override
					public void onAnimationUpdate(ValueAnimator valueAnim) {
						mProgressBar.setProgress((Integer) valueAnim.getAnimatedValue());
					}
				});
				
				mProgressAnimator.start();
			}
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		mProgressAnimator.cancel();
	}
	
	@Override
	public void onBackPressed() {}
}
