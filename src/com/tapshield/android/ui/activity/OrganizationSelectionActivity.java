package com.tapshield.android.ui.activity;

import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SearchView.OnCloseListener;
import android.widget.SearchView.OnQueryTextListener;

import com.tapshield.android.R;
import com.tapshield.android.api.model.Agency;
import com.tapshield.android.utils.UiUtils;

public class OrganizationSelectionActivity extends Activity {

	private ListView mList;
	private List<Agency> mNearbyAgencies;
	private List<Agency> mAllAgencies;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_organizationselection);
		
		mList = (ListView) findViewById(R.id.organizationselection_list);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.organizationselection, menu);
		
		SearchView searchView = (SearchView) menu.findItem(R.id.action_organizationselection_search);
		
		searchView.setOnCloseListener(new OnCloseListener() {
			
			@Override
			public boolean onClose() {
				UiUtils.toastShort(OrganizationSelectionActivity.this,"closed search view");
				return false;
			}
		});
		
		searchView.setOnQueryTextListener(new OnQueryTextListener() {
			
			@Override
			public boolean onQueryTextSubmit(String query) {
				return false;
			}
			
			@Override
			public boolean onQueryTextChange(String newText) {
				UiUtils.toastShort(OrganizationSelectionActivity.this, "search for " + newText);
				return false;
			}
		});
		return true;
	}
}
