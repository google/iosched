/*
 * Copyright 2021 Google LLC
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

package com.google.samples.apps.iosched.ui.util

import android.webkit.WebView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.google.android.material.composethemeadapter.MdcTheme

@Composable
fun IoschedWebViewDialog(
    title: String,
    url: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    IoschedDialog(title, onDismissRequest, modifier) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    loadUrl(url)
                }
            }
        )
    }
}

@Composable
fun IoschedDialog(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colors.surface,
            contentColor = contentColorFor(MaterialTheme.colors.surface)
        ) {
            Column(modifier.padding(top = 24.dp, bottom = 8.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.subtitle1,
                    fontSize = 20.sp,
                    fontFamily = GoogleSansMediumFamily,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(Modifier.height(8.dp))
                content()
            }
        }
    }
}

// Previewing Dialogs doesn't work: https://issuetracker.google.com/186502047
@Preview
@Composable
private fun IoschedDialogPreview() {
    MdcTheme {
        Surface {
            IoschedDialog("I'm a dialog", { }) {
                Text("I'm the content")
            }
        }
    }
}
