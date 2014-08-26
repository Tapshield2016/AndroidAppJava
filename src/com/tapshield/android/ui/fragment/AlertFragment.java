package com.tapshield.android.ui.fragment;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;

import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.model.Agency;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.manager.EmergencyManager;
import com.tapshield.android.manager.TwilioManager;
import com.tapshield.android.manager.TwilioManager.OnStatusChangeListener;
import com.tapshield.android.manager.TwilioManager.Status;
import com.tapshield.android.ui.activity.ChatActivity;
import com.tapshield.android.ui.activity.ReportListActivity;
import com.tapshield.android.ui.view.CircleButton;
import com.tapshield.android.utils.HardwareUtils;
import com.tapshield.android.utils.UiUtils;

public class AlertFragment extends Fragment implements OnClickListener, OnStatusChangeListener {

	private JavelinClient mJavelin;
	private EmergencyManager mEmergencyManager;
	private TwilioManager mTwilio;
	
	private View mCard;
	private TextView mCardNumber;
	private TextView mCardStatus;
	private Chronometer mCardChrono;
	private Button mCardSpeaker;
	
	private CircleButton mDetails;
	private CircleButton mCall;
	private CircleButton mChat;
	private Animation mCardAnimationIn;
	private Animation mCardAnimationOut;
	
	private AlertDialog mTwilioFailureDialog;
	private BroadcastReceiver mDispatcherNotifiedReceiver;
	private BroadcastReceiver mCompletionReceiver;
	private BroadcastReceiver mTwilioFailureReceiver;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_alert, container, false);
		mCard = root.findViewById(R.id.fragment_alert_card);
		mCardNumber = (TextView) root.findViewById(R.id.fragment_alert_text_phone);
		mCardChrono = (Chronometer) root.findViewById(R.id.fragment_alert_chrono);
		mCardStatus = (TextView) root.findViewById(R.id.fragment_alert_text_status);
		mCardSpeaker = (Button) root.findViewById(R.id.fragment_alert_button_speaker);
		
		mDetails = (CircleButton) root.findViewById(R.id.fragment_alert_circlebutton_details);
		mCall = (CircleButton) root.findViewById(R.id.fragment_alert_circlebutton_call);
		mChat = (CircleButton) root.findViewById(R.id.fragment_alert_circlebutton_chat);
		return root;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mJavelin = JavelinClient.getInstance(getActivity(), TapShieldApplication.JAVELIN_CONFIG);
		mEmergencyManager = EmergencyManager.getInstance(getActivity());
		mTwilio = TwilioManager.getInstance(getActivity());

		mCardAnimationIn = AnimationUtils.loadAnimation(getActivity(), R.anim.card_cuadratic_slide_in);
		mCardAnimationIn.setAnimationListener(new Animation.AnimationListener() {
			
			@Override
			public void onAnimationStart(Animation animation) {
				mCard.setVisibility(View.VISIBLE);
			}
			
			@Override
			public void onAnimationRepeat(Animation animation) {}
			
			@Override
			public void onAnimationEnd(Animation animation) {}
		});
		
		mCardAnimationOut = AnimationUtils.loadAnimation(getActivity(), R.anim.card_cuadratic_slide_out);
		mCardAnimationOut.setAnimationListener(new Animation.AnimationListener() {
			
			@Override
			public void onAnimationStart(Animation animation) {
				mCard.setVisibility(View.GONE);
			}
			
			@Override
			public void onAnimationRepeat(Animation animation) {}
			
			@Override
			public void onAnimationEnd(Animation animation) {}
		});

		mDetails.setOnClickListener(this);
		mCall.setOnClickListener(this);
		mChat.setOnClickListener(this);
		mCardSpeaker.setOnClickListener(this);

		mDispatcherNotifiedReceiver = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				toggleCard(true);
				mCardStatus.setText(R.string.ts_alert_authorities);
			}
		};
		
		mCompletionReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				setCompletionMessage();
			}
		};

		mTwilioFailureReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				promptUserForTwilioFailure();
			}
		};

		mTwilioFailureDialog = getTwilioFailureDialog();
		
		boolean enabled = HardwareUtils.isSpeakerphoneOn(getActivity());
		setCardSpeakerState(enabled);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		if (mEmergencyManager.isNotified()) {
			mCardStatus.setText(R.string.ts_alert_authorities);
		}
		
		if (mEmergencyManager.isCompleted()) {
			setCompletionMessage();
		}
		
		mTwilio.addOnStatusChangeListener(this);
		promptUserForTwilioFailure();
		
		/*
		 * REMEMBER UNCOMMENT UNREGISTRATION OF THIS SAME RECEIVER at onPause()
		IntentFilter twilioFailureFilter =
				new IntentFilter(EmergencyManager.ACTION_TWILIO_FAILED_EXCESSIVELY);
		getActivity().registerReceiver(mTwilioFailureReceiver, twilioFailureFilter);
		*/
		
		
		IntentFilter dispatcherNotifiedFilter = new IntentFilter(EmergencyManager.ACTION_EMERGENCY_SUCCESS);
		getActivity().registerReceiver(mDispatcherNotifiedReceiver, dispatcherNotifiedFilter);
		
		IntentFilter completionFilter = new IntentFilter(EmergencyManager.ACTION_EMERGENCY_COMPLETE);
		getActivity().registerReceiver(mCompletionReceiver, completionFilter);
	}
	
	@Override
	public void onPause() {
		if (mDispatcherNotifiedReceiver != null) {
			getActivity().unregisterReceiver(mDispatcherNotifiedReceiver);
		}
		
		
		getActivity().unregisterReceiver(mCompletionReceiver);
		//getActivity().unregisterReceiver(mTwilioFailureReceiver);
		
		mTwilio.removeOnStatusChangeListener(this);
		
		super.onPause();
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.fragment_alert_circlebutton_details:
			Intent reporting = new Intent(getActivity(), ReportListActivity.class);
			startActivity(reporting);
			break;
		case R.id.fragment_alert_circlebutton_call:
			boolean scheduled = mEmergencyManager.isRunning() && !mEmergencyManager.isTransmitting();
			
			if (scheduled) {
				mEmergencyManager.cancel();
				mEmergencyManager.startNow(EmergencyManager.TYPE_START_REQUESTED);
			} else {
				mEmergencyManager.requestRedial();
			}
			break;
		case R.id.fragment_alert_circlebutton_chat:
			Intent chat = new Intent(getActivity(), ChatActivity.class);
			startActivity(chat);
			break;
		case R.id.fragment_alert_button_speaker:
			boolean speakers = HardwareUtils.toggleSpeakerphone(getActivity());
			setCardSpeakerState(speakers);
			break;
		}
	}
	
	private void setCardSpeakerState(boolean enabled) {
		int color = getResources().getColor(
				enabled ? R.color.ts_brand_light : R.color.ts_gray_dark);
		int icon = enabled ? R.drawable.ts_speakers_enabled : R.drawable.ts_speakers_disabled;
		mCardSpeaker.setTextColor(color);
		mCardSpeaker.setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0);
	}
	
	private void setCompletionMessage() {
		Agency a = mJavelin.getUserManager().getUser().agency;
		mCardStatus.setText(a.completeMessage);
		mCardStatus.setTextColor(getResources().getColor(R.color.ts_brand_light));
		mCardStatus.setVisibility(View.VISIBLE);
	}
	
	private void promptUserForTwilioFailure() {
		//if twilio flag failed before activity creation and user has not been prompted,
		//    show the dialog and notify emergency manager
		/*
		if (mEmergencyManager.isTwilioFailingExcessively() && !mEmergencyManager.isTwilioFailureUserPrompted()) {
			mTwilioFailureDialog.show();
			mEmergencyManager.setTwilioFailureUserPrompted();
		}
		*/
	}
	
	private AlertDialog getTwilioFailureDialog() {

		DialogInterface.OnClickListener normalCallListener = new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				String primaryNumber = mJavelin.getUserManager().getUser().agency.primaryNumber;
				UiUtils.MakePhoneCall(getActivity(), primaryNumber);
			}
		};

		DialogInterface.OnClickListener retryListener = new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				mEmergencyManager.requestRedial();
			}
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
			.setCancelable(false)
			.setIcon(R.drawable.ic_launcher)
			.setTitle(R.string.ts_dialog_alert_twilio_failure_title)
			.setMessage(R.string.ts_dialog_alert_twilio_failure_message)
			.setPositiveButton(R.string.ts_dialog_alert_twilio_failure_button_normalcall,
					normalCallListener)
					.setNeutralButton(R.string.ts_dialog_alert_twilio_failure_button_retry,
							retryListener)
							.setNegativeButton(R.string.ts_common_cancel, null);
		return builder.create();
	}

	@Override
	public void onStatusChange(Status status) {
		boolean idle = status.equals(TwilioManager.Status.IDLE);
		boolean connecting = status.equals(TwilioManager.Status.CONNECTING);
		boolean busy = status.equals(TwilioManager.Status.BUSY);
		boolean error = status.equals(TwilioManager.Status.DISABLED);
		
		if (idle) {
			mCall.setEnabled(true);
		} else {
			mCall.setEnabled(false);
		}
		
		
		getActivity().runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				mCardChrono.stop();
			}
		});
		
		if (idle) {
			
			getActivity().runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					mCardChrono.setText(R.string.ts_alert_twilio_ended);
				}
			});
		} else if (connecting) {
			
			getActivity().runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					mCardChrono.setText(R.string.ts_alert_twilio_connecting);
				}
			});
			
			toggleCard(true);
		} else if (busy) {
			
			getActivity().runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					mCardChrono.setBase(mTwilio.getCallStartedAt());
					mCardChrono.start();
				}
			});
		} else if (error) {
			
			getActivity().runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					mCardChrono.setText(R.string.ts_alert_twilio_error);
				}
			});
		}
	}
	
	private void toggleCard() {
		toggleCard(mCard.getVisibility() == View.VISIBLE ? false : true);
	}
	
	private void toggleCard(final boolean toVisible) {
		int vis = toVisible ? View.VISIBLE : View.GONE;
		
		if (vis == mCard.getVisibility()) {
			return;
		}
		
		boolean populate = mCardNumber.getText().toString().isEmpty();
		
		if (toVisible && populate) {

			Agency a = mJavelin.getUserManager().getUser().agency;
			
			//TextView name = (TextView) findViewById(R.id.emergency_text_agency_name);
			//set format as (###) ###-####
			StringBuilder phoneNumberFormatter = new StringBuilder(a.primaryNumber)
					.insert(0, "(")
					.insert(4, ") ")
					.insert(9, "-");
			
			//name.setText(a.name);
			mCardNumber.setText(phoneNumberFormatter.toString());
		}
		
		getActivity().runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				mCard.startAnimation(toVisible ? mCardAnimationIn : mCardAnimationOut);
			}
		});
	}
}
