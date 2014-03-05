package com.tapshield.android.ui.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinUtils;
import com.tapshield.android.api.model.ChatMessage;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.ui.adapter.ChatMessageAdapter;

public class ChatActivity extends Activity {

	private ListView mList;
	private EditText mUserMessage;
	private ImageButton mSend;
	
	private List<ChatMessage> mMessages;
	private ChatMessageAdapter mAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat);
		
		mList = (ListView) findViewById(R.id.chat_list_messages);
		mUserMessage = (EditText) findViewById(R.id.chat_edit_message);
		mSend = (ImageButton) findViewById(R.id.chat_imagebutton_send);
		
		mMessages = new ArrayList<ChatMessage>();
		mAdapter = new ChatMessageAdapter(ChatActivity.this, R.layout.item_chat_message);
		mList.setAdapter(mAdapter);
	}
}
