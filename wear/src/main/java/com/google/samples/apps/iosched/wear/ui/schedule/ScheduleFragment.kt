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

package com.google.samples.apps.iosched.wear.ui.schedule

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.samples.apps.iosched.shared.result.EventObserver
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay.DAY_1
import com.google.samples.apps.iosched.shared.util.activityViewModelProvider
import com.google.samples.apps.iosched.wear.R
import com.google.samples.apps.iosched.wear.ui.WearableFragment
import kotlinx.android.synthetic.main.fragment_schedule.*
import javax.inject.Inject

/**
 * Lists the remaining sessions in a user's schedule based on the current time.
 */
class ScheduleFragment : WearableFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var viewModel: ScheduleViewModel

    private lateinit var adapter: ScheduleAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_schedule, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = activityViewModelProvider(viewModelFactory)
        adapter = ScheduleAdapter()

        wearableRecyclerView.apply {
            // Aligns the first and last items on the list vertically centered on the screen.
            isEdgeItemsCenteringEnabled = true

            // Improves performance because we know changes in content do not change the layout
            // size of the RecyclerView.
            setHasFixedSize(true)
            adapter = this@ScheduleFragment.adapter
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // TODO: Replace DAY_1 with current day once sign-in is implemented (followup CL).
        viewModel.getSessionsForDay(DAY_1).observe(requireActivity(), Observer { list ->
            adapter.submitList(list ?: emptyList())
        })

        // Show an error message
        viewModel.errorMessage.observe(this, EventObserver { errorMsg ->
            //TODO: Change once there's a way to show errors to the user
            Toast.makeText(this.context, errorMsg, Toast.LENGTH_LONG).show()
        })
    }

    override fun onUpdateAmbient() {
        // TODO(b/74259577): implement ambient UI
    }
}
