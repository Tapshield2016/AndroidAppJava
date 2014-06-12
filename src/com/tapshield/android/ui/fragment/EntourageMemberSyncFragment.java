package com.tapshield.android.ui.fragment;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridView;

import com.tapshield.android.R;
import com.tapshield.android.ui.dialog.ContactPickerDialogFragment;

public class EntourageMemberSyncFragment extends BaseFragment {

	private GridView mGridView;
	private FrameLayout mDialogContainer;
	private ContactPickerDialogFragment mContactsDialog; //create custom fragment dialog
	private ProgressDialog mBusyDialog;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_entouragemembersync, container, false);
		
		mGridView = (GridView) root.findViewById(R.id.entouragemembersync_grid);
		mDialogContainer = (FrameLayout) root.findViewById(R.id.entouragemembersync_dialog_container);
		
		return root;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		/*
		syncFromLocal();
		*/
	}
	
	private void showContactList() {
		/*
		showBusy();
		loadContacts();
		*/
	}
	
	private void loadContacts() {
		/*
		if (loaded) { show dialog }
		else { async.execute , show dialog async.onpost }
		*/
	}
	
	private void addMember() {
		/*
		syncToLocal();
		syncRemote();
		*/
	}
	
	private void removeMember() {
		/*
		syncToLocal();
		syncRemote();
		*/
	}
	
	private void showBusyDialog(String message) {
		/*
		mBusyDialog.setTitle(message);
		mBusyDialog.show();
		*/
	}
	
	private void hideBusyDialog() {
		/*
		mBusyDialog.dismiss();
		*/
	}
	
	//entourage manager should hold Contact objects instead of custom middle-man object ('overhead')
	//adapter for contact view load bitmap async via contect fetcher on specific id
}
