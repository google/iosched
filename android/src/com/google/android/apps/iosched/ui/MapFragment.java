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
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.provider.ScheduleContract.Rooms;
import com.google.android.apps.iosched.util.AnalyticsUtils;
import com.google.android.apps.iosched.util.ParserUtils;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

/**
 * Shows a {@link WebView} with a map of the conference venue.
 */
public class MapFragment extends Fragment {
    private static final String TAG = "MapFragment";

    /**
     * When specified, will automatically point the map to the requested room.
     */
    public static final String EXTRA_ROOM = "com.google.android.iosched.extra.ROOM";

    private static final String MAP_JSI_NAME = "MAP_CONTAINER";
    private static final String MAP_URL = "http://www.google.com/events/io/2011/embed.html";
    private static boolean CLEAR_CACHE_ON_LOAD = false;

    private WebView mWebView;
    private View mLoadingSpinner;
    private boolean mMapInitialized = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        AnalyticsUtils.getInstance(getActivity()).trackPageView("/Map");
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
        mWebView.setWebChromeClient(mWebChromeClient);
        mWebView.setWebViewClient(mWebViewClient);

        mWebView.post(new Runnable() {
            public void run() {
                // Initialize web view
                if (CLEAR_CACHE_ON_LOAD) {
                    mWebView.clearCache(true);
                }

                mWebView.getSettings().setJavaScriptEnabled(true);
                mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
                mWebView.loadUrl(MAP_URL);
                mWebView.addJavascriptInterface(mMapJsiImpl, MAP_JSI_NAME);
            }
        });

        return root;
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.refresh_menu_items, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_refresh) {
            mWebView.reload();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void runJs(String js) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Loading javascript:" + js);
        }
        mWebView.loadUrl("javascript:" + js);
    }

    /**
     * Helper method to escape JavaScript strings. Useful when passing strings to a WebView via
     * "javascript:" calls.
     */
    private static String escapeJsString(String s) {
        if (s == null) {
            return "";
        }

        return s.replace("'", "\\'").replace("\"", "\\\"");
    }

    public void panLeft(float screenFraction) {
        runJs("IoMap.panLeft('" + screenFraction + "');");
    }

    /**
     * I/O Conference Map JavaScript interface.
     */
    private interface MapJsi {
        void openContentInfo(String test);
        void onMapReady();
    }

    private WebChromeClient mWebChromeClient = new WebChromeClient() {
        public void onConsoleMessage(String message, int lineNumber, String sourceID) {
            Log.i(TAG, "JS Console message: (" + sourceID + ": " + lineNumber + ") " + message);
        }
    };

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
        public void onReceivedError(WebView view, int errorCode, String description,
                String failingUrl) {
            Log.e(TAG, "Error " + errorCode + ": " + description);
            Toast.makeText(view.getContext(), "Error " + errorCode + ": " + description,
                    Toast.LENGTH_LONG).show();
            super.onReceivedError(view, errorCode, description, failingUrl);
        }
    };

    private MapJsi mMapJsiImpl = new MapJsi() {
        public void openContentInfo(String roomId) {
            final String possibleTrackId = ParserUtils.translateTrackIdAlias(roomId);
            final Intent intent;
            if (ParserUtils.LOCAL_TRACK_IDS.contains(possibleTrackId)) {
                // This is a track; open up the sandbox for the track, since room IDs that are
                // track IDs are sandbox areas in the map.
                Uri trackVendorsUri = ScheduleContract.Tracks.buildVendorsUri(possibleTrackId);
                intent = new Intent(Intent.ACTION_VIEW, trackVendorsUri);
            } else {
                Uri roomUri = Rooms.buildSessionsDirUri(roomId);
                intent = new Intent(Intent.ACTION_VIEW, roomUri);
            }
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    ((BaseActivity) getActivity()).openActivityOrFragment(intent);
                }
            });
        }

        public void onMapReady() {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onMapReady");
            }

            final Intent intent = BaseActivity.fragmentArgumentsToIntent(getArguments());

            String showRoomId = null;
            if (!mMapInitialized && intent.hasExtra(EXTRA_ROOM)) {
                showRoomId = intent.getStringExtra(EXTRA_ROOM);
            }

            if (showRoomId != null) {
                runJs("IoMap.showLocationById('" + escapeJsString(showRoomId) + "');");
            }

            mMapInitialized = true;
        }
    };
}
