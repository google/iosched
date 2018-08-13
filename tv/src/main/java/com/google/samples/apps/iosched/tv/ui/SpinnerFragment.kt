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

package com.google.samples.apps.iosched.tv.ui

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import com.google.samples.apps.iosched.tv.R

/**
 * Displays a loading spinner. To use, add this fragment to your frame but don't forget to remove
 * when you are done.
 *
 * To add, add this fragment with the fragment manager.
 * ```
 * fragmentManager?.inTransaction {
 *  add(R.id.main_frame, spinnerFragment)
 * }
 * ```
 * Don't forget to remove this fragment in your [Fragment.onStop].
 * ```
 * override fun onStop() {
 *  super.onStop()
 *  fragmentManager?.inTransaction {
 *    remove(spinnerFragment)
 *  }
 * }
 *
 * ```
 */
class SpinnerFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val context = inflater.context
        val progressBar = ProgressBar(context)
        if (container is FrameLayout) {
            val res = context.resources
            val width = res.getDimensionPixelSize(R.dimen.spinner_size)
            val height = res.getDimensionPixelSize(R.dimen.spinner_size)
            val layoutParams = FrameLayout.LayoutParams(width, height, Gravity.CENTER)
            progressBar.layoutParams = layoutParams
        }
        return progressBar
    }
}
