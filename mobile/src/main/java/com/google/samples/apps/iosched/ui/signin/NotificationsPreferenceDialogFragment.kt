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
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.shared.util.viewModelProvider
import com.google.samples.apps.iosched.widget.CustomDimDialogFragment
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.AndroidSupportInjection
import dagger.android.support.HasSupportFragmentInjector
import kotlinx.android.synthetic.main.dialog_notifications_preference.notifications_pref_button_no
import kotlinx.android.synthetic.main.dialog_notifications_preference.notifications_pref_button_yes
import javax.inject.Inject

/**
 * Dialog that asks the user notifications preference.
 */
class NotificationsPreferenceDialogFragment : CustomDimDialogFragment(),
    HasSupportFragmentInjector {

    @Inject
    lateinit var fragmentInjector: DispatchingAndroidInjector<Fragment>

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var viewModel: NotificationsPreferenceViewModel

    override fun supportFragmentInjector(): AndroidInjector<Fragment> {
        return fragmentInjector
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        AndroidSupportInjection.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_notifications_preference, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = viewModelProvider(viewModelFactory)

        notifications_pref_button_no.setOnClickListener {
            viewModel.onNoClicked()
            dismiss()
        }
        notifications_pref_button_yes.setOnClickListener {
            viewModel.onYesClicked()
            dismiss()
        }
    }

    override fun onDismiss(dialog: DialogInterface?) {
        super.onDismiss(dialog)
        viewModel.onDismissed()
    }

    companion object {
        const val DIALOG_NOTIFICATIONS_PREFERENCE = "dialog_notifications_preference"
    }
}
