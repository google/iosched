package com.google.samples.apps.iosched.videolibrary;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.google.samples.apps.iosched.util.NetworkUtils;

import no.java.schedule.R;

/**
 * Created by kkho on 28.05.2016.
 */
public class JzVideoLibraryFragment extends Fragment {
    private WebView mVimeoWebView;
    private SwipeRefreshLayout mSwipeRefeshLayout;
    private final static String VIMEO_WEB_URL = "https://vimeo.com/javazone/videos";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_videowebview, container, false);
        mVimeoWebView = (WebView) rootView.findViewById(R.id.vimeo_webview);
        mSwipeRefeshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_container);
        initializeWebView(savedInstanceState);
        initializeSwipeRefresh();
        return rootView;
    }

    private void initializeWebView(Bundle savedInstanceState) {
        mVimeoWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        mVimeoWebView.getSettings().setJavaScriptEnabled(true);
        mVimeoWebView.getSettings().setAllowFileAccess(true);
        mVimeoWebView.getSettings().setDomStorageEnabled(true);
        mVimeoWebView.setWebViewClient(new VimeoVideoWebViewClient());

        if (NetworkUtils.checkInternetConnection(getContext())) {
            if (savedInstanceState == null) {
                mVimeoWebView.loadUrl(VIMEO_WEB_URL);
            } else {
                mVimeoWebView.restoreState(savedInstanceState);
            }
        } else {
            Toast.makeText(getActivity(), "No internet connection. Please turn on the internet",
                    Toast.LENGTH_LONG).show();
            webViewKeyDown(KeyEvent.KEYCODE_BACK);

        }
    }

    private void initializeSwipeRefresh() {
        mSwipeRefeshLayout.setColorSchemeColors(R.color.jz_lightred,
                R.color.jz_green, R.color.jz_darkred, R.color.jz_orange,
                R.color.jz_yellow);
        mSwipeRefeshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mSwipeRefeshLayout.setRefreshing(true);
                (new Handler()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mSwipeRefeshLayout.setRefreshing(false);
                        if (!NetworkUtils.checkInternetConnection(getActivity())) {
                            Toast.makeText(getActivity(), "No internet connection. Please turn on the internet",
                                    Toast.LENGTH_LONG).show();

                        } else {
                            mVimeoWebView.loadUrl(VIMEO_WEB_URL);
                        }
                    }
                }, 4000);
            }
        });
    }

    public void doWebViewCall(String searchParam) {
        mVimeoWebView.clearView();
        mVimeoWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        mVimeoWebView.getSettings().setJavaScriptEnabled(true);
        mVimeoWebView.getSettings().setAllowFileAccess(true);
        mVimeoWebView.getSettings().setDomStorageEnabled(true);
        mVimeoWebView.loadUrl(VIMEO_WEB_URL + "/search:developer");
    }

    public boolean webViewKeyDown(int keyCode) {
        // Check if the key event was the Back button and if there's history
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            if (mVimeoWebView.canGoBack()) {
                mVimeoWebView.goBack();
                return true;
            }
        }

        return false;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        mVimeoWebView.saveState(outState);
    }

    private class VimeoVideoWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }
}
