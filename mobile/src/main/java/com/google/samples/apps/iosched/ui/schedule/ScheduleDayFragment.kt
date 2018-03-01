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

package com.google.samples.apps.iosched.ui.schedule

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay
import com.google.samples.apps.iosched.shared.util.activityViewModelProvider
import com.google.samples.apps.iosched.shared.util.getEnum
import com.google.samples.apps.iosched.shared.util.lazyFast
import com.google.samples.apps.iosched.shared.util.putEnum
import com.google.samples.apps.iosched.util.clearDecorations
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.fragment_schedule_day.*
import javax.inject.Inject

/**
 * Fragment displaying a single conference day's schedule
 */
class ScheduleDayFragment : DaggerFragment() {

    companion object {
        private const val TAG = "ScheduleDayFragment"
        private const val ARG_CONFERENCE_DAY = "arg.CONFERENCE_DAY"

        fun newInstance(day: ConferenceDay): ScheduleDayFragment {
            val args = Bundle().apply {
                putEnum(ARG_CONFERENCE_DAY, day)
            }
            return ScheduleDayFragment().apply { arguments = args }
        }
    }

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var viewModel: ScheduleViewModel

    private val conferenceDay: ConferenceDay by lazyFast {
        val args = arguments ?: throw IllegalStateException("Missing arguments!")
        args.getEnum<ConferenceDay>(ARG_CONFERENCE_DAY)
    }

    private lateinit var adapter: ScheduleDayAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_schedule_day, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = activityViewModelProvider(viewModelFactory)
        adapter = ScheduleDayAdapter(viewModel)
        recyclerview.adapter = adapter
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.getSessionsForDay(conferenceDay).observe(requireActivity(), Observer { list ->
            adapter.submitList(list ?: emptyList())

            // Recreate the decoration used for the sticky time headers
            recyclerview.clearDecorations()
            if (list != null && list.isNotEmpty()) {
                recyclerview.addItemDecoration(
                    ScheduleTimeHeadersDecoration(recyclerview.context, list))
            }
        })

        // Show an error message
        viewModel.errorMessage.observe(this, Observer { message ->
            //TODO: Change once there's a way to show errors to the user
            if (!message.isNullOrEmpty() && !viewModel.wasErrorMessageShown()) {
                // Prevent the message from showing more than once
                viewModel.onErrorMessageShown()
                Toast.makeText(this.context, message, Toast.LENGTH_LONG).show()
            }
        })
    }
}
