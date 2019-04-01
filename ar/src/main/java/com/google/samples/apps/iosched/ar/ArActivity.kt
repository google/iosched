/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.iosched.ar

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.web.webview.ArWebView
import com.google.samples.apps.iosched.domain.ar.ArConstants

class ArActivity : AppCompatActivity() {

    private lateinit var pinnedSessionsJson: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pinnedSessionsJson =
            intent?.extras?.getString(ArConstants.PINNED_SESSIONS_JSON_KEY, "") ?: ""

        val arWebView = ArWebView(this)
        setContentView(arWebView)
        arWebView.apply {
            webView.webViewClient = SendPinnedSessionsWebViewClient(pinnedSessionsJson)
            webView.settings.mediaPlaybackRequiresUserGesture = false
            // Loading a single entry point because all the user flow happens in JavaScript from the
            // teaser page and requesting ARCore apk and camera permission
            loadUrl("https://sp-io2019.appspot.com/")
        }
    }

    /**
     * WebViewClient that sends the pinned sessions as json to the WebView.
     * Defining it as a class otherwise an anonymous class was stripped from proguard.
     */
    private class SendPinnedSessionsWebViewClient(val json: String) : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            val javaScript =
                "if (window.app && window.app.sendIOAppUserAgenda) " +
                        "window.app.sendIOAppUserAgenda('$json');"
            view?.evaluateJavascript(javaScript) {}
        }
    }
}