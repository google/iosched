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

package com.google.samples.apps.iosched.ui.signin

import android.view.View
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.shared.data.signin.AuthenticatedUserInfo

@BindingAdapter("navHeaderTitle")
fun setNavHeaderTitle(view: TextView, user: AuthenticatedUserInfo?) {
    if (user?.isSignedIn() == true) {
        view.text = user.getDisplayName()
    } else {
        view.setText(R.string.welcome)
    }
}

@BindingAdapter("navHeaderSubtitle")
fun setNavHeaderSubtitle(view: TextView, user: AuthenticatedUserInfo?) {
    if (user?.isSignedIn() == true) {
        view.text = user.getEmail()
    } else {
        view.setText(R.string.sign_in_hint)
    }
}

@BindingAdapter("navHeaderContentDescription")
fun setNavHeaderContentDescription(view: View, user: AuthenticatedUserInfo?) {
    view.contentDescription = if (user?.isSignedIn() == true) {
        view.resources.getString(R.string.a11y_signed_in_content_description, user.getDisplayName())
    } else {
        view.resources.getString(R.string.a11y_signed_out_content_description)
    }
}
