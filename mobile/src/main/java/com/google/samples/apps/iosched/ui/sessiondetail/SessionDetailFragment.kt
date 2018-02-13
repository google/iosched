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

package com.google.samples.apps.iosched.ui.sessiondetail

import android.arch.lifecycle.ViewModelProviders
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.FragmentSessionDetailBinding

class SessionDetailFragment : Fragment() {

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        // TODO: wire up detail page to schedule list and get session ID intent extra (b/72671324)
        val dummySessionId = "1"

        val sessionDetailViewModel = ViewModelProviders
                .of(this, SessionDetailViewModelFactory(dummySessionId))
                .get(SessionDetailViewModel::class.java)

        val binding: FragmentSessionDetailBinding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_session_detail, container, false)
        binding.run {
            viewModel = sessionDetailViewModel
            setLifecycleOwner(this@SessionDetailFragment)
        }

        // TODO: replace with Toolbar (b/73537084)
        (activity as AppCompatActivity).setSupportActionBar(binding.sessionDetailToolbar)

        return binding.root
    }
}