/*
 * Copyright 2018 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.iosched.ui.schedule

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.FragmentScheduleBinding
import kotlinx.android.synthetic.main.toolbar.*


class ScheduleFragment : Fragment() {

    companion object {
        val TAG: String = ScheduleFragment::class.java.simpleName
    }

    override fun onCreateView(inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val viewModel: ScheduleViewModel = ViewModelProviders.of(
                this, ScheduleViewModelFactory()).get(ScheduleViewModel::class.java)

        val binding: FragmentScheduleBinding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_schedule, container, false)

        // Set the layout variables
        binding.viewModel = viewModel
        binding.setLifecycleOwner(this)

        // TODO: This is an example subscription
        observeViewModel(viewModel, binding)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        toolbar.setTitle(R.string.title_schedule)
    }

    private fun observeViewModel(viewModel: ScheduleViewModel, b: FragmentScheduleBinding) {

        // Update text if there are sessions available
        viewModel.sessions.observe(this, Observer { sessions ->
            b.textView.let {
                val sb = StringBuilder()
                sessions?.forEach { session ->
                    sb.append("${session.title} by ${session.speakers.joinToString(", ")}")
                }
                it.text = sb.toString()
            }
        })

        // Update text if the screen is in loading state.
        viewModel.isLoading.observe(this, Observer { isLoading ->
            if (isLoading!!) b.textView.text = getString(R.string.loading)
        })
    }
}
