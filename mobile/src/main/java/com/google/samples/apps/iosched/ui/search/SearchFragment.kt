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

package com.google.samples.apps.iosched.ui.search

import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnNextLayout
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.FragmentSearchBinding
import com.google.samples.apps.iosched.databinding.SearchActiveFiltersNarrowBinding
import com.google.samples.apps.iosched.databinding.SearchActiveFiltersWideBinding
import com.google.samples.apps.iosched.shared.analytics.AnalyticsHelper
import com.google.samples.apps.iosched.ui.schedule.ScheduleTwoPaneViewModel
import com.google.samples.apps.iosched.ui.sessioncommon.SessionsAdapter
import com.google.samples.apps.iosched.util.doOnApplyWindowInsets
import com.google.samples.apps.iosched.util.launchAndRepeatWithViewLifecycle
import com.google.samples.apps.iosched.util.setContentMaxWidth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class SearchFragment : Fragment() {

    @Inject
    lateinit var analyticsHelper: AnalyticsHelper

    @Inject
    @Named("tagViewPool")
    lateinit var tagViewPool: RecycledViewPool

    private lateinit var binding: FragmentSearchBinding

    private val viewModel: SearchViewModel by viewModels()
    private val scheduleTwoPaneViewModel: ScheduleTwoPaneViewModel by activityViewModels()

    private lateinit var sessionsAdapter: SessionsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val themedInflater =
            inflater.cloneInContext(ContextThemeWrapper(requireActivity(), R.style.AppTheme_Detail))
        binding = FragmentSearchBinding.inflate(themedInflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewModel = viewModel

        binding.toolbar.apply {
            inflateMenu(R.menu.search_menu)
            setOnMenuItemClickListener {
                if (it.itemId == R.id.action_open_filters) {
                    findFiltersFragment().showFiltersSheet()
                    true
                } else {
                    false
                }
            }
        }

        binding.searchView.apply {
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    dismissKeyboard(this@apply)
                    return true
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    viewModel.onSearchQueryChanged(newText)
                    return true
                }
            })

            // Set focus on the SearchView and open the keyboard
            setOnQueryTextFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    showKeyboard(view.findFocus())
                }
            }
            requestFocus()
        }

        sessionsAdapter = SessionsAdapter(
            tagViewPool,
            viewModel.showReservations,
            viewModel.timeZoneId,
            viewLifecycleOwner,
            scheduleTwoPaneViewModel, // OnSessionClickListener
            scheduleTwoPaneViewModel // OnSessionStarClickListener
        )
        binding.recyclerView.apply {
            adapter = sessionsAdapter
            doOnApplyWindowInsets { v, insets, padding ->
                val systemInsets = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
                )
                v.updatePadding(bottom = padding.bottom + systemInsets.bottom)
            }
            doOnNextLayout {
                setContentMaxWidth(this)
            }
        }

        launchAndRepeatWithViewLifecycle {
            viewModel.searchResults.collect {
                sessionsAdapter.submitList(it)
            }
        }

        /* The active filters on Search can appear in one of two places:
         *   - In the toolbar next to the search field (on wide screens)
         *   - In the app bar below the toolbar (on narrow screens)
         *
         * Normally this could be handled by a resource with a width qualifier, e.g. layout-w720dp.
         * However, Search can appear in the list pane of a two pane layout. When both panes are
         * visible, a resource qualifier like the above will give us the "wide" state (based on the
         * device width) when we actually want the "narrow" state (based on the list pane width).
         * Instead we check the toolbar width after first layout and inflate one of two ViewStubs.
         */
        binding.toolbar.doOnNextLayout { toolbar ->
            val threshold =
                resources.getDimensionPixelSize(R.dimen.active_filters_in_toolbar_threshold)
            if (toolbar.width >= threshold) {
                binding.activeFiltersWideStub.viewStub?.apply {
                    setOnInflateListener { _, inflated ->
                        SearchActiveFiltersWideBinding.bind(inflated).apply {
                            viewModel = this@SearchFragment.viewModel
                            lifecycleOwner = viewLifecycleOwner
                        }
                    }
                    inflate()
                }
            } else {
                binding.activeFiltersNarrowStub.viewStub?.apply {
                    setOnInflateListener { _, inflated ->
                        SearchActiveFiltersNarrowBinding.bind(inflated).apply {
                            viewModel = this@SearchFragment.viewModel
                            lifecycleOwner = viewLifecycleOwner
                        }
                    }
                    inflate()
                }
            }
        }

        if (savedInstanceState == null) {
            // On first entry, show the filters.
            findFiltersFragment().showFiltersSheet()
        }
        analyticsHelper.sendScreenView("Search", requireActivity())
    }

    override fun onPause() {
        dismissKeyboard(binding.searchView)
        super.onPause()
    }

    private fun showKeyboard(view: View) {
        ViewCompat.getWindowInsetsController(view)?.show(WindowInsetsCompat.Type.ime())
    }

    private fun dismissKeyboard(view: View) {
        ViewCompat.getWindowInsetsController(view)?.hide(WindowInsetsCompat.Type.ime())
    }

    private fun findFiltersFragment(): SearchFilterFragment {
        return childFragmentManager.findFragmentById(R.id.filter_sheet) as SearchFilterFragment
    }
}
