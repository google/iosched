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

package com.google.samples.apps.iosched.ui.sessiondetail

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.FragmentSessionFeedbackBinding
import com.google.samples.apps.iosched.databinding.ItemQuestionBinding
import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.shared.result.data
import com.google.samples.apps.iosched.util.launchAndRepeatWithDialogLifecycle
import com.google.samples.apps.iosched.widget.SimpleRatingBar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect

@ExperimentalCoroutinesApi
@AndroidEntryPoint
class SessionFeedbackFragment : AppCompatDialogFragment() {

    private val viewModel: SessionFeedbackViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sessionId = arguments?.getString(ARG_SESSION_ID)
        if (sessionId == null) {
            dismiss()
        } else {
            viewModel.setSessionId(sessionId)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = FragmentSessionFeedbackBinding.inflate(LayoutInflater.from(context))
        val questionAdapter = QuestionAdapter()
        binding.questions.run {
            layoutManager = LinearLayoutManager(context)
            adapter = questionAdapter
        }

        questionAdapter.submitList(viewModel.questions)

        launchAndRepeatWithDialogLifecycle {
            viewModel.userSession.collect {
                it.data?.let {
                    dialog?.setTitle(it.session.title)
                }
            }
        }
        return MaterialAlertDialogBuilder(requireContext())
            // The actual title is set asynchronously, but there has to be some title to
            // initialize the view first.
            .setTitle("-")
            .setView(binding.root)
            .setPositiveButton(R.string.feedback_submit) { _, _ ->
                viewModel.submit(questionAdapter.feedbackUpdates)
                (parentFragment as Listener).onFeedbackSubmitted()
            }
            .setNegativeButton(android.R.string.cancel, /* ignore */ null)
            .create()
    }

    internal interface Listener {
        fun onFeedbackSubmitted()
    }

    companion object {
        private const val ARG_SESSION_ID = "session_id"

        fun createInstance(sessionId: SessionId) = SessionFeedbackFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_SESSION_ID, sessionId)
            }
        }
    }
}

class QuestionViewHolder(val binding: ItemQuestionBinding) : RecyclerView.ViewHolder(binding.root)

class QuestionAdapter : ListAdapter<Question, QuestionViewHolder>(DIFF_CALLBACK) {

    val feedbackUpdates = mutableMapOf<String, Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
        return QuestionViewHolder(
            ItemQuestionBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {
        val question = getItem(position)
        holder.binding.question = feedbackUpdates[question.key]?.let {
            question.copy(currentRating = it)
        } ?: question
        holder.binding.rating.setOnRateListener(object : SimpleRatingBar.OnRateListener {
            override fun onRate(rate: Int) {
                feedbackUpdates[question.key] = rate
            }
        })
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Question>() {
            override fun areItemsTheSame(oldItem: Question, newItem: Question): Boolean {
                return oldItem.key == newItem.key
            }

            override fun areContentsTheSame(oldItem: Question, newItem: Question): Boolean {
                return oldItem == newItem
            }
        }
    }
}
