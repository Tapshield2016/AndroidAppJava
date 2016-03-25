package com.tapshield.android.ui.activity;

import android.app.ActionBar;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.model.Agency;
import com.tapshield.android.app.TapShieldApplication;
import com.tapshield.android.utils.UiUtils;

public class WebHelpActivity extends BaseFragmentActivity {

	private ProgressBar mLoading;
	private WebView mWebView;
	private WebViewClient mClient;
	private String mInfoUrl;
	
	@Override
	protected void onCreate(Bundle savedInstance) {
		super.onCreate(savedInstance);
		setContentView(R.layout.activity_webhelp);
		
		Agency agency = JavelinClient
				.getInstance(this, TapShieldApplication.JAVELIN_CONFIG)
				.getUserManager()
				.getUser()
				.agency;
		
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(agency.name);
		
		mLoading = (ProgressBar) findViewById(R.id.webhelp_progressbar);
		
		mClient = new WebViewClient() {
			
			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				super.onPageStarted(view, url, favicon);
				mLoading.setVisibility(View.VISIBLE);
			}
			
			@Override
			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);
				mLoading.setVisibility(View.GONE);
			}
		};
		
		mWebView = (WebView) findViewById(R.id.webhelp_webview);
		mWebView.setWebViewClient(mClient);
		
		mInfoUrl = agency.infoUrl;
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		if (mInfoUrl != null) {
			mWebView.loadUrl(mInfoUrl);
		} else {
			finish();
		}
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		
		switch (item.getItemId()) {
		case android.R.id.home:
			UiUtils.startActivityNoStack(this, MainActivity.class);
			return true;
		}
		
		return false;
	}
}
