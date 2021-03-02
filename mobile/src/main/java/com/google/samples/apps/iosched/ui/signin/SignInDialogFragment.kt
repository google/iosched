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
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.samples.apps.iosched.databinding.DialogSignInBinding
import com.google.samples.apps.iosched.shared.result.EventObserver
import com.google.samples.apps.iosched.ui.signin.SignInEvent.RequestSignIn
import com.google.samples.apps.iosched.util.executeAfter
import com.google.samples.apps.iosched.util.signin.SignInHandler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Dialog that tells the user to sign in to continue the operation.
 */
@AndroidEntryPoint
class SignInDialogFragment : AppCompatDialogFragment() {

    @Inject
    lateinit var signInHandler: SignInHandler

    private val signInViewModel: SignInViewModel by viewModels()

    private lateinit var binding: DialogSignInBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // We want to create a dialog, but we also want to use DataBinding for the content view.
        // We can do that by making an empty dialog and adding the content later.
        return MaterialAlertDialogBuilder(requireContext()).create()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // In case we are showing as a dialog, use getLayoutInflater() instead.
        binding = DialogSignInBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        signInViewModel.performSignInEvent.observe(
            viewLifecycleOwner,
            EventObserver { request ->
                if (request == RequestSignIn) {
                    activity?.let { activity ->
                        val signInIntent = signInHandler.makeSignInIntent()
                        val observer = object : Observer<Intent?> {
                            override fun onChanged(it: Intent?) {
                                activity.startActivityForResult(it, REQUEST_CODE_SIGN_IN)
                                signInIntent.removeObserver(this)
                            }
                        }
                        signInIntent.observeForever(observer)
                    }
                    dismiss()
                }
            }
        )

        binding.executeAfter {
            viewModel = signInViewModel
            lifecycleOwner = viewLifecycleOwner
        }

        if (showsDialog) {
            (requireDialog() as AlertDialog).setView(binding.root)
        }
    }

    companion object {
        const val DIALOG_SIGN_IN = "dialog_sign_in"
        const val REQUEST_CODE_SIGN_IN = 42
    }
}
