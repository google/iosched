/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.apps.iosched.ui;

import com.google.android.apps.iosched.R;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * A {@link WebView}-based fragment that shows Google Realtime Search results for a given query,
 * provided as the {@link TagStreamFragment#EXTRA_QUERY} extra in the fragment arguments. If no
 * search query is provided, the conference hashtag is used as the default query.
 */
public class TagStreamFragment extends Fragment {

    private static final String TAG = "TagStreamFragment";

    public static final String EXTRA_QUERY = "com.google.android.iosched.extra.QUERY";

    public static final String CONFERENCE_HASHTAG = "#io2011";

    private String mSearchString;
    private WebView mWebView;
    private View mLoadingSpinner;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = BaseActivity.fragmentArgumentsToIntent(getArguments());
        mSearchString = intent.getStringExtra(EXTRA_QUERY);
        if (TextUtils.isEmpty(mSearchString)) {
            mSearchString = CONFERENCE_HASHTAG;
        }
        if (!mSearchString.startsWith("#")) {
            mSearchString = "#" + mSearchString;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_webview_with_spinner, null);

        // For some reason, if we omit this, NoSaveStateFrameLayout thinks we are
        // FILL_PARENT / WRAP_CONTENT, making the progress bar stick to the top of the activity.
        root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.FILL_PARENT));

        mLoadingSpinner = root.findViewById(R.id.loading_spinner);
        mWebView = (WebView) root.findViewById(R.id.webview);
        mWebView.setWebViewClient(mWebViewClient);

        mWebView.post(new Runnable() {
            public void run() {
                mWebView.getSettings().setJavaScriptEnabled(true);
                mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
                try {
                    mWebView.loadUrl(
                            "http://www.google.com/search?tbs="
                            + "mbl%3A1&hl=en&source=hp&biw=1170&bih=668&q="
                            + URLEncoder.encode(mSearchString, "UTF-8")
                            + "&btnG=Search");
                } catch (UnsupportedEncodingException e) {
                    Log.e(TAG, "Could not construct the realtime search URL", e);
                }
            }
        });

        return root;
    }

    public void refresh() {
        mWebView.reload();
    }

    private WebViewClient mWebViewClient = new WebViewClient() {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            mLoadingSpinner.setVisibility(View.VISIBLE);
            mWebView.setVisibility(View.INVISIBLE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            mLoadingSpinner.setVisibility(View.GONE);
            mWebView.setVisibility(View.VISIBLE);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.startsWith("javascript")) {
                return false;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            return true;
        }
    };
}
