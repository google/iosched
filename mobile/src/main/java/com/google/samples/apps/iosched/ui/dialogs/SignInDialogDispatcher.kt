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

package com.google.samples.apps.iosched.ui.dialogs

import androidx.fragment.app.FragmentActivity
import com.google.samples.apps.iosched.ui.signin.SignInDialogFragment
import com.google.samples.apps.iosched.ui.signin.SignOutDialogFragment
import javax.inject.Inject

/**
 * Shows the sign-in dialog.
 */
class SignInDialogDispatcher @Inject constructor() {

    fun openSignInDialog(activity: FragmentActivity) {
        val dialog = SignInDialogFragment()
        dialog.show(activity.supportFragmentManager,
            DIALOG_NEED_TO_SIGN_IN
        )
    }

    fun openSignOutDialog(activity: FragmentActivity) {
        val dialog = SignOutDialogFragment()
        dialog.show(activity.supportFragmentManager,
            DIALOG_CONFIRM_SIGN_OUT
        )
    }

    companion object {
        private const val DIALOG_NEED_TO_SIGN_IN = "dialog_need_to_sign_in"
        const val DIALOG_CONFIRM_SIGN_OUT = "dialog_confirm_sign_out"
    }
}
