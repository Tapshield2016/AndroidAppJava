package com.tapshield.android.ui.activity;

import java.util.List;

import android.app.ActionBar;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.tapshield.android.R;
import com.tapshield.android.api.JavelinChatManager;
import com.tapshield.android.api.JavelinChatManager.OnNewChatMessageListener;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.model.ChatMessage;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.location.LocationTracker;
import com.tapshield.android.manager.EmergencyManager;
import com.tapshield.android.ui.adapter.ChatMessageAdapter;
import com.tapshield.android.utils.EmergencyManagerUtils;
import com.tapshield.android.utils.UiUtils;

public class ChatActivity extends BaseFragmentActivity implements OnNewChatMessageListener {

	private ListView mList;
	private EditText mUserMessage;
	private ImageButton mSend;
	private View mAlertOverlay;
	
	//private List<ChatMessage> mMessages;
	private ChatMessageAdapter mAdapter;
	
	private EmergencyManager mEmergencyManager;
	private JavelinChatManager mChatManager;
	private LocationTracker mTracker;
	
	private BroadcastReceiver mSuccessfulAlertReceiver;
	private BroadcastReceiver mCompletionReceiver;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat);
		
		mEmergencyManager = EmergencyManager.getInstance(ChatActivity.this);
		mChatManager = JavelinClient.getInstance(ChatActivity.this,
				TapShieldApplication.JAVELIN_CONFIG).getChatManager();
		mTracker = LocationTracker.getInstance(ChatActivity.this);
		
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		
		mList = (ListView) findViewById(R.id.chat_list_messages);
		mUserMessage = (EditText) findViewById(R.id.chat_edit_message);
		mSend = (ImageButton) findViewById(R.id.chat_imagebutton_send);
		mAlertOverlay = findViewById(R.id.chat_view_alertoverlay);
		
		//mMessages = new ArrayList<ChatMessage>();
		mAdapter = new ChatMessageAdapter(ChatActivity.this, R.layout.item_chat_message_user,
				R.layout.item_chat_message_other);
		mList.setAdapter(mAdapter);
		
		mSend.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				sendPressed();
			}
		});

		mUserMessage.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				sendPressed();
				return true;
			}
		});
		
		mSuccessfulAlertReceiver = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				showOverlayOnAlert(true);
			}
		};
		
		mCompletionReceiver = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				UiUtils.startActivityNoStack(ChatActivity.this, MainActivity.class);
			}
		};
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		mChatManager.addOnNewChatMessageListener(this);
		mChatManager.notifySeeing();
		refreshUi();
		mTracker.start();
		showOverlayOnAlert(false);
		
		IntentFilter alertedFilter = new IntentFilter(EmergencyManager.ACTION_EMERGENCY_SUCCESS);
		registerReceiver(mSuccessfulAlertReceiver, alertedFilter);
		
		IntentFilter completionFilter=  new IntentFilter(EmergencyManager.ACTION_EMERGENCY_COMPLETE);
		registerReceiver(mCompletionReceiver, completionFilter);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		unregisterReceiver(mSuccessfulAlertReceiver);
		unregisterReceiver(mCompletionReceiver);
		
		mChatManager.notifyNotSeeing();
		mChatManager.removeOnNewChatMessageListener(this);
		boolean alertInProgress = JavelinClient.getInstance(ChatActivity.this,
				TapShieldApplication.JAVELIN_CONFIG).getAlertManager().isRunning();
		if (!alertInProgress) {
			LocationTracker.getInstance(ChatActivity.this).stop();
		}
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			return true;
		}
		
		return false;
	}
	
	private void sendPressed() {
		// if not started, do so
		if (!mEmergencyManager.isRunning()) {
			mEmergencyManager.startNow(EmergencyManager.TYPE_CHAT);
		} else if (mEmergencyManager.getRemaining() > 0) {
			/* otherwise, stop scheduled emergency IFF set in the future to start it now with
			 * the current type of alert.
			 */
			mEmergencyManager.cancel();
			mEmergencyManager.startNow(EmergencyManager.TYPE_START_REQUESTED);
		}
		sendUserMessage();
	}
	
	private void sendUserMessage() {
		final String userMessage = mUserMessage.getText().toString().trim();
		if (userMessage.isEmpty()) {
			return;
		}
		mUserMessage.setText(new String());
		mChatManager.send(userMessage);
	}
	
	private void refreshUi() {
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				mAdapter.notifyDataSetChanged();
				scrollToBottom();
			}
		});
	}
	
	private void scrollToBottom() {
		mList.setSelection(mAdapter.getCount() - 1);
	}
	
	private void showOverlayOnAlert(boolean animate) {
		//check for type chat since it will be considered a more passive emergency
		if (mEmergencyManager.isRunning()
				&& mEmergencyManager.getType() != EmergencyManager.TYPE_CHAT
				&& mAlertOverlay.getVisibility() != View.VISIBLE) {
			
			mAlertOverlay.setVisibility(View.VISIBLE);
			
			if (animate) {
				AlphaAnimation alpha = new AlphaAnimation(0.0f, 1.0f);
				alpha.setDuration(getResources().getInteger(R.integer.ts_chat_overlay_animation_milli));
				mAlertOverlay.startAnimation(alpha);
			}
		}
	}

	@Override
	public void onNewChatMessage(List<ChatMessage> allMessages) {
		mAdapter.setItemsNoNotifyDataSetChanged(allMessages);
		refreshUi();
	}
	
	@Override
	public void onBackPressed() {
		
		Class<? extends BaseFragmentActivity> to = AlertActivity.class;
		
		//check for type chat since it will be considered a more passive emergency 
		if (!mEmergencyManager.isRunning()
				|| EmergencyManagerUtils.isPassiveEmergencyActive(mEmergencyManager)) {
			to = MainActivity.class;
		}
		
		UiUtils.startActivityNoStack(this, to);
	}
}
