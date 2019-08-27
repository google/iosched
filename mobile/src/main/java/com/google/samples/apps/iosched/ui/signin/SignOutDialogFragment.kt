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

package com.google.samples.apps.iosched.ui.signin

import android.app.Dialog
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.shared.domain.internal.DefaultScheduler
import com.google.samples.apps.iosched.shared.util.viewModelProvider
import com.google.samples.apps.iosched.ui.signin.SignInEvent.RequestSignOut
import com.google.samples.apps.iosched.util.signin.SignInHandler
import dagger.android.support.DaggerAppCompatDialogFragment
import javax.inject.Inject

/**
 * Dialog that confirms that a user wishes to sign out.
 */
class SignOutDialogFragment : DaggerAppCompatDialogFragment() {

    @Inject
    lateinit var signInHandler: SignInHandler

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var signInViewModel: SignInViewModel

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        signInViewModel = viewModelProvider(viewModelFactory)
        signInViewModel.performSignInEvent.observe(this, Observer { request ->
            if (request.peekContent() == RequestSignOut) {
                request.getContentIfNotHandled()
                context?.let {
                    DefaultScheduler.execute {
                        signInHandler.signOut(it) {
                            dismiss()
                        }
                    }
                }
            }
        })
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_sign_out_title)
            .setMessage(R.string.dialog_sign_out_content)
            .setNegativeButton(R.string.not_now, null)
            .setPositiveButton(R.string.sign_out) { _, _ ->
                signInViewModel.onSignOut()
            }
            .create()
    }
}
