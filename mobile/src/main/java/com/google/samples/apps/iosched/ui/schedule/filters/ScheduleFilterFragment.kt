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

package com.google.samples.apps.iosched.ui.schedule.filters

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.samples.apps.iosched.databinding.FragmentScheduleFilterBinding
import com.google.samples.apps.iosched.shared.util.activityViewModelProvider
import com.google.samples.apps.iosched.ui.schedule.ScheduleViewModel
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.fragment_schedule_filter.*
import javax.inject.Inject

/**
 * Fragment that shows the list of filters for the Schedule
 */
class ScheduleFilterFragment : DaggerFragment() {

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var viewModel: ScheduleViewModel

    private lateinit var filterAdapter: ScheduleFilterAdapter

    private lateinit var binding: FragmentScheduleFilterBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentScheduleFilterBinding.inflate(inflater, container, false)
        binding.setLifecycleOwner(this)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = activityViewModelProvider(viewModelFactory)
        binding.viewModel = viewModel

        filterAdapter = ScheduleFilterAdapter(viewModel)
        viewModel.tagFilters.observe(this, Observer { filterAdapter.submitTagFilterList(it) })

        recyclerview.apply {
            adapter = filterAdapter
            setHasFixedSize(true)
            (layoutManager as GridLayoutManager).spanSizeLookup =
                    ScheduleFilterSpanSizeLookup(filterAdapter)
        }
    }
}
