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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.view.isGone
import androidx.databinding.BindingAdapter
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.samples.apps.iosched.databinding.DialogSignOutBinding
import com.google.samples.apps.iosched.shared.data.signin.AuthenticatedUserInfo
import com.google.samples.apps.iosched.ui.signin.SignInNavigationAction.RequestSignOut
import com.google.samples.apps.iosched.util.executeAfter
import com.google.samples.apps.iosched.util.signin.SignInHandler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

/**
 * Dialog that confirms that a user wishes to sign out.
 */
@AndroidEntryPoint
class SignOutDialogFragment : AppCompatDialogFragment() {

    @Inject
    lateinit var signInHandler: SignInHandler

    private val signInViewModel: SignInViewModel by viewModels()

    private lateinit var binding: DialogSignOutBinding

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
        binding = DialogSignOutBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            signInViewModel.signInNavigationActions.collect { action ->
                if (action == RequestSignOut) {
                    signInHandler.signOut(requireContext())
                    dismiss()
                }
            }
        }

        binding.executeAfter {
            viewModel = signInViewModel
            lifecycleOwner = viewLifecycleOwner
        }

        if (showsDialog) {
            (requireDialog() as AlertDialog).setView(binding.root)
        }
    }
}

@BindingAdapter("username")
fun setUsername(textView: TextView, userInfo: AuthenticatedUserInfo?) {
    val displayName = userInfo?.getDisplayName()
    textView.text = displayName
    textView.isGone = displayName.isNullOrEmpty()
}

@BindingAdapter("userEmail")
fun setUserEmail(textView: TextView, userInfo: AuthenticatedUserInfo?) {
    val email = userInfo?.getEmail()
    textView.text = email
    textView.isGone = email.isNullOrEmpty()
}
