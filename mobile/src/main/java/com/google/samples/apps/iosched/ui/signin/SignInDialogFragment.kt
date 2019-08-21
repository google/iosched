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

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.samples.apps.iosched.databinding.DialogSignInBinding
import com.google.samples.apps.iosched.shared.result.EventObserver
import com.google.samples.apps.iosched.shared.util.viewModelProvider
import com.google.samples.apps.iosched.ui.signin.SignInEvent.RequestSignIn
import com.google.samples.apps.iosched.util.signin.SignInHandler
import com.google.samples.apps.iosched.widget.CustomDimDialogFragment
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.AndroidSupportInjection
import dagger.android.support.HasSupportFragmentInjector
import javax.inject.Inject

/**
 * Dialog that tells the user to sign in to continue the operation.
 */
class SignInDialogFragment : CustomDimDialogFragment(), HasSupportFragmentInjector {

    @Inject
    lateinit var fragmentInjector: DispatchingAndroidInjector<Fragment>

    @Inject
    lateinit var signInHandler: SignInHandler

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var signInViewModel: SignInViewModel

    override fun supportFragmentInjector(): AndroidInjector<Fragment> {
        return fragmentInjector
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        AndroidSupportInjection.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        signInViewModel = viewModelProvider(viewModelFactory)
        val binding = DialogSignInBinding.inflate(inflater, container, false).apply {
            viewModel = signInViewModel
        }

        signInViewModel.performSignInEvent.observe(this, Observer { request ->
            if (request.peekContent() == RequestSignIn) {
                request.getContentIfNotHandled()
                activity?.let {
                    signInHandler.makeSignInIntent().observe(this, Observer {
                        startActivityForResult(it, SIGN_IN_ACTIVITY_REQUEST_CODE)
                        dismiss()
                    })
                }
            }
        })

        signInViewModel.dismissDialogAction.observe(this, EventObserver {
            dismiss()
        })
        return binding.root
    }

    companion object {
        const val SIGN_IN_ACTIVITY_REQUEST_CODE = 42
    }
}
