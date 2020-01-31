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

import android.content.Context
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.SearchView
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.samples.apps.iosched.R.style
import com.google.samples.apps.iosched.databinding.FragmentSearchBinding
import com.google.samples.apps.iosched.shared.analytics.AnalyticsHelper
import com.google.samples.apps.iosched.shared.result.EventObserver
import com.google.samples.apps.iosched.shared.util.viewModelProvider
import com.google.samples.apps.iosched.ui.MainNavigationFragment
import com.google.samples.apps.iosched.ui.search.SearchFragmentDirections.Companion.toSessionDetail
import com.google.samples.apps.iosched.ui.search.SearchFragmentDirections.Companion.toSpeakerDetail
import com.google.samples.apps.iosched.util.doOnApplyWindowInsets
import com.google.samples.apps.iosched.util.openWebsiteUrl
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_search.view.searchView

class SearchFragment : MainNavigationFragment() {
    @Inject lateinit var analyticsHelper: AnalyticsHelper
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var binding: FragmentSearchBinding
    private lateinit var viewModel: SearchViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val themedInflater =
            inflater.cloneInContext(ContextThemeWrapper(requireActivity(), style.AppTheme_Detail))
        binding = FragmentSearchBinding.inflate(themedInflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
        }
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = viewModelProvider(viewModelFactory)
        binding.viewModel = viewModel

        viewModel.navigateToSessionAction.observe(viewLifecycleOwner, EventObserver { sessionId ->
            findNavController().navigate(toSessionDetail(sessionId))
        })
        viewModel.navigateToSpeakerAction.observe(viewLifecycleOwner, EventObserver { speakerId ->
            findNavController().navigate(toSpeakerDetail(speakerId))
        })
        viewModel.navigateToCodelabAction.observe(viewLifecycleOwner, EventObserver { url ->
            openWebsiteUrl(requireActivity(), url)
        })
        analyticsHelper.sendScreenView("Search", requireActivity())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.searchView.apply {
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
        binding.recyclerView.doOnApplyWindowInsets { v, insets, padding ->
            v.updatePadding(bottom = padding.bottom + insets.systemWindowInsetBottom)
        }
    }

    override fun onPause() {
        dismissKeyboard(binding.toolbar.searchView)
        super.onPause()
    }

    private fun showKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_FORCED)
    }

    private fun dismissKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
