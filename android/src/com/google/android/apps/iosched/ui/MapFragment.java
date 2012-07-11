/*
 * Copyright 2012 Google Inc.
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

import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.apps.iosched.BuildConfig;
import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.util.UIUtils;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import static com.google.android.apps.iosched.util.LogUtils.LOGD;
import static com.google.android.apps.iosched.util.LogUtils.LOGE;
import static com.google.android.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Shows a {@link WebView} with a map of the conference venue.
 */
public class MapFragment extends SherlockFragment {
    private static final String TAG = makeLogTag(MapFragment.class);

    /**
     * When specified, will automatically point the map to the requested room.
     */
    public static final String EXTRA_ROOM = "com.google.android.iosched.extra.ROOM";

    private static final String SYSTEM_FEATURE_MULTITOUCH
            = "android.hardware.touchscreen.multitouch";

    private static final String MAP_JSI_NAME = "MAP_CONTAINER";
    private static final String MAP_URL = "http://ioschedmap.appspot.com/embed.html";

    private static boolean CLEAR_CACHE_ON_LOAD = BuildConfig.DEBUG;

    private WebView mWebView;
    private View mLoadingSpinner;
    private boolean mMapInitialized = false;

    public interface Callbacks {
        public void onRoomSelected(String roomId);
    }

    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onRoomSelected(String roomId) {
        }
    };

    private Callbacks mCallbacks = sDummyCallbacks;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        EasyTracker.getTracker().trackView("Map");
        LOGD("Tracker", "Map");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_webview_with_spinner,
                container, false);

        mLoadingSpinner = root.findViewById(R.id.loading_spinner);
        mWebView = (WebView) root.findViewById(R.id.webview);
        mWebView.setWebChromeClient(mWebChromeClient);
        mWebView.setWebViewClient(mWebViewClient);
        return root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Initialize web view
        if (CLEAR_CACHE_ON_LOAD) {
            mWebView.clearCache(true);
        }

        boolean hideZoomControls =
                getActivity().getPackageManager().hasSystemFeature(SYSTEM_FEATURE_MULTITOUCH)
                && UIUtils.hasHoneycomb();

        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
        mWebView.loadUrl(MAP_URL + "?multitouch=" + (hideZoomControls ? 1 : 0));
        mWebView.addJavascriptInterface(mMapJsiImpl, MAP_JSI_NAME);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof Callbacks)) {
            throw new ClassCastException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.map, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_refresh) {
            mWebView.reload();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void runJs(final String js) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LOGD(TAG, "Loading javascript:" + js);
                mWebView.loadUrl("javascript:" + js);
            }
        });
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

    public void panBy(float xFraction, float yFraction) {
        runJs("IoMap.panBy(" + xFraction + "," + yFraction + ");");
    }

    /**
     * I/O Conference Map JavaScript interface.
     */
    private interface MapJsi {
        public void openContentInfo(final String roomId);
        public void onMapReady();
    }

    private final WebChromeClient mWebChromeClient = new WebChromeClient() {
        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            LOGD(TAG, "JS Console message: (" + consoleMessage.sourceId() + ": "
                    + consoleMessage.lineNumber() + ") " + consoleMessage.message());
            return false;
        }
    };

    private final WebViewClient mWebViewClient = new WebViewClient() {
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
            LOGE(TAG, "Error " + errorCode + ": " + description);
            Toast.makeText(view.getContext(), "Error " + errorCode + ": " + description,
                    Toast.LENGTH_LONG).show();
            super.onReceivedError(view, errorCode, description, failingUrl);
        }
    };

    private final MapJsi mMapJsiImpl = new MapJsi() {
        public void openContentInfo(final String roomId) {
            Activity activity = getActivity();
            if (activity == null) {
                return;
            }

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mCallbacks.onRoomSelected(roomId);
                }
            });
        }

        public void onMapReady() {
            LOGD(TAG, "onMapReady");

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
