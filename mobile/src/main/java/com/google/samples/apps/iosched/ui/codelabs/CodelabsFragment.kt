/*
 * Copyright 2019 Google LLC
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

package com.google.samples.apps.iosched.ui.codelabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updatePadding
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import com.google.samples.apps.iosched.databinding.FragmentCodelabsBinding
import com.google.samples.apps.iosched.shared.util.viewModelProvider
import com.google.samples.apps.iosched.ui.MainNavigationFragment
import com.google.samples.apps.iosched.util.doOnApplyWindowInsets
import com.google.samples.apps.iosched.util.openWebsiteUrl
import javax.inject.Inject
import javax.inject.Named

class CodelabsFragment : MainNavigationFragment(), CodelabsActionsHandler {

    companion object {
        private const val CODELABS_WEBSITE = "https://codelabs.developers.google.com"
    }

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    @field:Named("tagViewPool")
    lateinit var tagRecycledViewPool: RecycledViewPool

    private lateinit var binding: FragmentCodelabsBinding
    private lateinit var codelabsViewModel: CodelabsViewModel
    private lateinit var codelabsAdapter: CodelabsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCodelabsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        codelabsAdapter = CodelabsAdapter(tagRecycledViewPool, this, savedInstanceState)
        binding.codelabsList.adapter = codelabsAdapter

        // Pad the bottom of the RecyclerView so that the content scrolls up above the nav bar
        binding.codelabsList.doOnApplyWindowInsets { v, insets, padding ->
            v.updatePadding(bottom = padding.bottom + insets.systemWindowInsetBottom)
        }

        codelabsViewModel = viewModelProvider(viewModelFactory)
        codelabsViewModel.codelabs.observe(this, Observer {
            codelabsAdapter.submitList(it)
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        codelabsAdapter.onSaveInstanceState(outState)
    }

    override fun dismissCodelabsInfoCard() {
        // Pass to ViewModel, which will update the list contents
        codelabsViewModel.dismissCodelabsInfoCard()
    }

    override fun openCodelabsOnMap() {
        findNavController().navigate(CodelabsFragmentDirections.toMap())
    }

    override fun launchCodelabsWebsite() {
        openWebsiteUrl(requireContext(), CODELABS_WEBSITE)
    }
}
