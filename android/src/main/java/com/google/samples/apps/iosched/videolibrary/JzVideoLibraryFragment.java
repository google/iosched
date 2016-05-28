package com.google.samples.apps.iosched.videolibrary;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import com.google.samples.apps.iosched.util.NetworkUtils;

import no.java.schedule.R;

/**
 * Created by kkho on 28.05.2016.
 */
public class JzVideoLibraryFragment extends Fragment {
    private WebView mVimeoWebView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_videowebview, container, false);
        mVimeoWebView = (WebView)rootView.findViewById(R.id.vimeo_webview);
        initializeWebView(savedInstanceState);
        return rootView;
    }

    private void initializeWebView(Bundle savedInstanceState) {
        final String webUrl = "https://vimeo.com/javazone/videos";
        mVimeoWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        mVimeoWebView.getSettings().setJavaScriptEnabled(true);
        mVimeoWebView.getSettings().setAllowFileAccess(true);
        mVimeoWebView.getSettings().setDomStorageEnabled(true);

        if (NetworkUtils.checkInternetConnection(getContext())) {
            if (savedInstanceState == null) {
                mVimeoWebView.loadUrl(webUrl);
            } else {
                mVimeoWebView.restoreState(savedInstanceState);
            }
        } else {
            Toast.makeText(getActivity(), "No internet connection. Please turn on the internet",
                    Toast.LENGTH_LONG).show();
            webViewKeyDown(KeyEvent.KEYCODE_BACK);

        }
    }


    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public boolean webViewKeyDown(int keyCode){
        // Check if the key event was the Back button and if there's history
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            if(mVimeoWebView.canGoBack()){
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
}
