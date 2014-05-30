package com.tapshield.android.ui.activity;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.tapshield.android.R;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.app.TapShieldApplication;

public class WebHelpActivity extends BaseFragmentActivity {

	private ProgressBar mLoading;
	private WebView mWebView;
	private WebViewClient mClient;
	
	@Override
	protected void onCreate(Bundle savedInstance) {
		super.onCreate(savedInstance);
		setContentView(R.layout.activity_webhelp);
		
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
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		String infoUrl = JavelinClient
				.getInstance(this, TapShieldApplication.JAVELIN_CONFIG)
				.getUserManager()
				.getUser()
				.agency
				.infoUrl;
		mWebView.loadUrl(infoUrl);
	}
}
