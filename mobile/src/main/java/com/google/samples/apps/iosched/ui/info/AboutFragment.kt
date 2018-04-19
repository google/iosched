/*
 * Copyright 2018 Google LLC
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

package com.google.samples.apps.iosched.ui.info

import android.app.AlertDialog
import android.content.Intent
import android.databinding.BindingAdapter
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.FragmentInfoAboutBinding
import dagger.android.support.DaggerFragment

// TODO use chrome custom tabs if user has chrome installed.
@BindingAdapter("websiteLink")
fun websiteLink(
    button: Button,
    url: String?
) {
    url ?: return
    button.setOnClickListener {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
        }
        it.context.startActivity(intent, null)
    }
}

@BindingAdapter(value = ["dialogTitle", "fileLink"], requireAll = true)
fun createDialogForFile(button: Button, dialogTitle: String, fileLink: String) {
    val context = button.context
    button.setOnClickListener {
        val webView = WebView(context).apply { loadUrl(fileLink) }
        AlertDialog.Builder(context)
                .setTitle(dialogTitle)
                .setView(webView)
                .create()
                .show()
    }
}

class AboutFragment : DaggerFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return FragmentInfoAboutBinding.inflate(inflater).root
    }
}