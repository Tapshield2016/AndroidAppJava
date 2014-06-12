package com.tapshield.android.ui.activity;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.tapshield.android.R;
import com.tapshield.android.api.JavelinSocialReportingManager;
import com.tapshield.android.utils.SocialReportsUtils;
import com.tapshield.android.utils.UiUtils;

public class ReportListActivity extends ListActivity implements OnItemClickListener {

	private ReportListAdapter mAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		mAdapter = new ReportListAdapter();
		setListAdapter(mAdapter);
		getListView().setOnItemClickListener(this);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			UiUtils.startActivityNoStack(this, MainActivity.class);
			return true;
		}
		return false;
	}
	
	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
		Intent report = new Intent(this, ReportActivity.class);
		report.putExtra(ReportActivity.EXTRA_TYPE_INDEX, position);
		startActivity(report);
	}
	
	class ReportListAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return JavelinSocialReportingManager.TYPE_LIST.length;
		}

		@Override
		public String getItem(int position) {
			return JavelinSocialReportingManager.TYPE_LIST[position];
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			
			if (view == null) {
				view = LayoutInflater
						.from(ReportListActivity.this)
						.inflate(R.layout.item_reportlist, null);
			}
			
			ImageView image = (ImageView) view.findViewById(R.id.item_reportlist_image);
			TextView text = (TextView) view.findViewById(R.id.item_reportlist_text);

			image.setImageResource(SocialReportsUtils.getDrawableOfType(getItem(position), false));
			text.setText(getItem(position));
			
			return view;
		}
	}
}
