/*
    Copyright 2012, 2013, Yasuaki Honda (yasuaki.honda@gmail.com)
    This file is part of MaximaOnAndroid.

    MaximaOnAndroid is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 2 of the License, or
    (at your option) any later version.

    MaximaOnAndroid is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with MaximaOnAndroid.  If not, see <http://www.gnu.org/licenses/>.
 */

package jp.yhonda;

import android.os.Bundle;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

/** Shows a local page: a graph (maxout.html) or the About page. */
public class HTMLActivity extends AppCompatActivity {
	WebView webview = null;
	/* Back goes back in the page history first; enabled only while there is one. */
	private final OnBackPressedCallback backInPage = new OnBackPressedCallback(false) {
		@Override
		public void handleOnBackPressed() {
			webview.goBack();
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		SystemBars.setUp(this);
		getOnBackPressedDispatcher().addCallback(this, backInPage);
		setContentView(R.layout.htmlactivity);
		webview = (WebView) findViewById(R.id.webViewInHTMLActivity);
		webview.getSettings().setJavaScriptEnabled(true);
		// Graphs are loaded from file:///data/... (maxout.html); file access is off by default since targetSdk 30.
		webview.getSettings().setAllowFileAccess(true);
		webview.setWebViewClient(new WebViewClient() {
			@Override
			public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
				backInPage.setEnabled(view.canGoBack());
			}
		});
		webview.getSettings().setBuiltInZoomControls(true);
		webview.getSettings().setUseWideViewPort(true);
		webview.getSettings().setLoadWithOverviewMode(true);
		webview.setWebChromeClient(new WebChromeClient() {
			public boolean onConsoleMessage(ConsoleMessage cm) {
				Log.d("MyApplication",
						cm.message() + " -- From line " + cm.lineNumber()
								+ " of " + cm.sourceId());
				return true;
			}
		});

		if (!getIntent().getBooleanExtra("hwaccel", true)) {
			webview.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
		}

		String url = getIntent().getStringExtra("url");
		webview.setContentDescription(url);
		webview.loadUrl(url);
	}
}
