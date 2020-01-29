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

import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ShareCompat
import androidx.core.net.toUri
import androidx.core.view.forEach
import androidx.core.view.updatePadding
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import androidx.transition.TransitionInflater
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.R.style
import com.google.samples.apps.iosched.databinding.FragmentSessionDetailBinding
import com.google.samples.apps.iosched.model.Session
import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.model.SpeakerId
import com.google.samples.apps.iosched.shared.analytics.AnalyticsActions
import com.google.samples.apps.iosched.shared.analytics.AnalyticsHelper
import com.google.samples.apps.iosched.shared.di.MapFeatureEnabledFlag
import com.google.samples.apps.iosched.shared.domain.users.SwapRequestParameters
import com.google.samples.apps.iosched.shared.notifications.AlarmBroadcastReceiver
import com.google.samples.apps.iosched.shared.result.EventObserver
import com.google.samples.apps.iosched.shared.util.activityViewModelProvider
import com.google.samples.apps.iosched.shared.util.toEpochMilli
import com.google.samples.apps.iosched.shared.util.viewModelProvider
import com.google.samples.apps.iosched.ui.MainNavigationFragment
import com.google.samples.apps.iosched.ui.messages.SnackbarMessageManager
import com.google.samples.apps.iosched.ui.prefs.SnackbarPreferenceViewModel
import com.google.samples.apps.iosched.ui.reservation.RemoveReservationDialogFragment
import com.google.samples.apps.iosched.ui.reservation.RemoveReservationDialogFragment.Companion.DIALOG_REMOVE_RESERVATION
import com.google.samples.apps.iosched.ui.reservation.RemoveReservationDialogParameters
import com.google.samples.apps.iosched.ui.reservation.SwapReservationDialogFragment
import com.google.samples.apps.iosched.ui.reservation.SwapReservationDialogFragment.Companion.DIALOG_SWAP_RESERVATION
import com.google.samples.apps.iosched.ui.schedule.ScheduleFragmentDirections.Companion.toSessionDetail
import com.google.samples.apps.iosched.ui.sessiondetail.SessionDetailFragmentDirections.Companion.toSpeakerDetail
import com.google.samples.apps.iosched.ui.setUpSnackbar
import com.google.samples.apps.iosched.ui.signin.NotificationsPreferenceDialogFragment
import com.google.samples.apps.iosched.ui.signin.NotificationsPreferenceDialogFragment.Companion.DIALOG_NOTIFICATIONS_PREFERENCE
import com.google.samples.apps.iosched.ui.signin.SignInDialogFragment
import com.google.samples.apps.iosched.ui.signin.SignInDialogFragment.Companion.DIALOG_SIGN_IN
import com.google.samples.apps.iosched.util.doOnApplyWindowInsets
import com.google.samples.apps.iosched.util.openWebsiteUrl
import com.google.samples.apps.iosched.util.postponeEnterTransition
import javax.inject.Inject
import javax.inject.Named
import timber.log.Timber

class SessionDetailFragment : MainNavigationFragment(), SessionFeedbackFragment.Listener {

    private var shareString = ""

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject lateinit var snackbarMessageManager: SnackbarMessageManager

    private lateinit var sessionDetailViewModel: SessionDetailViewModel

    @Inject lateinit var analyticsHelper: AnalyticsHelper

    @Inject
    @field:Named("tagViewPool")
    lateinit var tagRecycledViewPool: RecycledViewPool

    @Inject
    @JvmField
    @MapFeatureEnabledFlag
    var isMapEnabled: Boolean = false

    private var session: Session? = null

    private lateinit var sessionTitle: String

    private lateinit var binding: FragmentSessionDetailBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        sessionDetailViewModel = viewModelProvider(viewModelFactory)

        sharedElementReturnTransition =
            TransitionInflater.from(context).inflateTransition(R.transition.speaker_shared_enter)
        // Delay the enter transition until speaker image has loaded.
        postponeEnterTransition(500L)

        val themedInflater =
            inflater.cloneInContext(ContextThemeWrapper(requireActivity(), style.AppTheme_Detail))
        binding = FragmentSessionDetailBinding.inflate(themedInflater, container, false).apply {
            viewModel = sessionDetailViewModel
            lifecycleOwner = viewLifecycleOwner
        }

        binding.sessionDetailBottomAppBar.run {
            inflateMenu(R.menu.session_detail_menu)
            menu.findItem(R.id.menu_item_map)?.isVisible = isMapEnabled
            sessionDetailViewModel.session.observe(viewLifecycleOwner, Observer { session ->
                menu.findItem(R.id.menu_item_ask_question).isVisible = session.doryLink.isNotBlank()
            })
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_item_share -> {
                        ShareCompat.IntentBuilder.from(activity)
                            .setType("text/plain")
                            .setText(shareString)
                            .setChooserTitle(R.string.intent_chooser_session_detail)
                            .startChooser()
                    }
                    R.id.menu_item_star -> {
                        sessionDetailViewModel.onStarClicked()
                    }
                    R.id.menu_item_map -> {
                        val directions = SessionDetailFragmentDirections.toMap(
                            featureId = session?.room?.id,
                            startTime = session?.startTime?.toEpochMilli() ?: 0L
                        )
                        findNavController().navigate(directions)
                    }
                    R.id.menu_item_ask_question -> {
                        sessionDetailViewModel.session.value?.let { session ->
                            openWebsiteUrl(requireContext(), session.doryLink)
                        }
                    }
                    R.id.menu_item_calendar -> {
                        sessionDetailViewModel.session.value?.let(::addToCalendar)
                    }
                }
                true
            }
        }

        val detailsAdapter = SessionDetailAdapter(
            viewLifecycleOwner,
            sessionDetailViewModel,
            tagRecycledViewPool
        )
        binding.sessionDetailRecyclerView.run {
            adapter = detailsAdapter
            itemAnimator?.run {
                addDuration = 120L
                moveDuration = 120L
                changeDuration = 120L
                removeDuration = 100L
            }
            doOnApplyWindowInsets { view, insets, state ->
                view.updatePadding(bottom = state.bottom + insets.systemWindowInsetBottom)
                // CollapsingToolbarLayout's defualt scrim visible trigger height is a bit large.
                // Choose something smaller so that the content stays visible longer.
                binding.collapsingToolbar.scrimVisibleHeightTrigger =
                    insets.systemWindowInsetTop * 2
            }
        }

        sessionDetailViewModel.session.observe(viewLifecycleOwner, Observer {
            detailsAdapter.speakers = it?.speakers?.toList() ?: emptyList()
        })

        sessionDetailViewModel.relatedUserSessions.observe(viewLifecycleOwner, Observer {
            detailsAdapter.related = it ?: emptyList()
        })

        sessionDetailViewModel.session.observe(viewLifecycleOwner, Observer {
            session = it
            shareString = if (it == null) {
                ""
            } else {
                getString(R.string.share_text_session_detail, it.title, it.sessionUrl)
            }
        })

        sessionDetailViewModel.navigateToYouTubeAction.observe(
            viewLifecycleOwner,
            EventObserver { youtubeUrl ->
                openYoutubeUrl(youtubeUrl)
            }
        )

        sessionDetailViewModel.navigateToSessionAction.observe(
            viewLifecycleOwner,
            EventObserver { sessionId ->
                findNavController().navigate(toSessionDetail(sessionId))
            }
        )

        val snackbarPreferenceViewModel: SnackbarPreferenceViewModel =
            activityViewModelProvider(viewModelFactory)
        setUpSnackbar(
            sessionDetailViewModel.snackBarMessage,
            binding.snackbar,
            snackbarMessageManager,
            actionClickListener = {
                snackbarPreferenceViewModel.onStopClicked()
            }
        )

        sessionDetailViewModel.errorMessage.observe(viewLifecycleOwner, EventObserver { errorMsg ->
            // TODO: Change once there's a way to show errors to the user
            Toast.makeText(this.context, errorMsg, Toast.LENGTH_LONG).show()
        })

        sessionDetailViewModel.navigateToSignInDialogAction.observe(
            viewLifecycleOwner,
            EventObserver {
                openSignInDialog(requireActivity())
            }
        )
        sessionDetailViewModel.navigateToRemoveReservationDialogAction.observe(
            viewLifecycleOwner,
            EventObserver {
                openRemoveReservationDialog(requireActivity(), it)
            }
        )
        sessionDetailViewModel.navigateToSwapReservationDialogAction.observe(
            viewLifecycleOwner,
            EventObserver {
                openSwapReservationDialog(requireActivity(), it)
            }
        )

        sessionDetailViewModel.shouldShowNotificationsPrefAction.observe(
            viewLifecycleOwner,
            EventObserver {
                if (it) {
                    openNotificationsPreferenceDialog()
                }
            }
        )

        sessionDetailViewModel.navigateToSpeakerDetail.observe(
            viewLifecycleOwner,
            EventObserver { speakerId ->
                val sharedElement = findSpeakerHeadshot(
                    binding.sessionDetailRecyclerView,
                    speakerId
                )
                val extras = FragmentNavigatorExtras(sharedElement to sharedElement.transitionName)
                findNavController().navigate(toSpeakerDetail(speakerId), extras)
            }
        )

        sessionDetailViewModel.navigateToSessionFeedbackAction.observe(
            viewLifecycleOwner,
            EventObserver {
                openFeedbackDialog(it)
            }
        )

        // When opened from the post session notification, open the feedback dialog
        requireNotNull(arguments).apply {
            val sessionId = getString(EXTRA_SESSION_ID)
                    ?: SessionDetailFragmentArgs.fromBundle(this).sessionId
            val openRateSession =
                arguments?.getBoolean(AlarmBroadcastReceiver.EXTRA_SHOW_RATE_SESSION_FLAG) ?: false
            sessionDetailViewModel.showFeedbackButton.observe(viewLifecycleOwner, Observer {
                if (it == true && openRateSession) {
                    openFeedbackDialog(sessionId)
                }
            })
        }
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        Timber.d("Loading details for session $arguments")

        requireNotNull(arguments).apply {
            // TODO(benbaxter): Only use SessionDetailFragmentArgs and delete SessionDetailActivity.
            // Default with the value passed from the activity, otherwise assume the fragment was
            // added from the navigation controller.
            val sessionId = getString(EXTRA_SESSION_ID)
                ?: SessionDetailFragmentArgs.fromBundle(this).sessionId

            sessionDetailViewModel.setSessionId(sessionId)
        }
    }

    override fun onStop() {
        super.onStop()
        // Force a refresh when this screen gets added to a backstack and user comes back to it.
        sessionDetailViewModel.setSessionId(null)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // Observing the changes from Fragment because data binding doesn't work with menu items.
        val menu = binding.sessionDetailBottomAppBar.menu
        val starMenu = menu.findItem(R.id.menu_item_star)
        sessionDetailViewModel.shouldShowStarInBottomNav.observe(
            viewLifecycleOwner,
            Observer { showStar ->
                starMenu.isVisible = showStar == true
            }
        )
        sessionDetailViewModel.userEvent.observe(viewLifecycleOwner, Observer { userEvent ->
            userEvent?.let {
                if (it.isStarred) {
                    starMenu.setIcon(R.drawable.ic_star)
                } else {
                    starMenu.setIcon(R.drawable.ic_star_border)
                }
            }
        })

        var titleUpdated = false
        sessionDetailViewModel.session.observe(viewLifecycleOwner, Observer {
            if (it != null && !titleUpdated) {
                sessionTitle = it.title
                activity?.let { activity ->
                    analyticsHelper.sendScreenView("Session Details: $sessionTitle", activity)
                }
                titleUpdated = true
            }
        })
    }

    override fun onFeedbackSubmitted() {
        binding.snackbar.show(R.string.feedback_thank_you)
    }

    private fun openYoutubeUrl(youtubeUrl: String) {
        analyticsHelper.logUiEvent(sessionTitle, AnalyticsActions.YOUTUBE_LINK)
        startActivity(Intent(Intent.ACTION_VIEW, youtubeUrl.toUri()))
    }

    private fun openSignInDialog(activity: FragmentActivity) {
        SignInDialogFragment().show(activity.supportFragmentManager, DIALOG_SIGN_IN)
    }

    private fun openNotificationsPreferenceDialog() {
        NotificationsPreferenceDialogFragment()
            .show(requireActivity().supportFragmentManager, DIALOG_NOTIFICATIONS_PREFERENCE)
    }

    private fun openRemoveReservationDialog(
        activity: FragmentActivity,
        parameters: RemoveReservationDialogParameters
    ) {
        RemoveReservationDialogFragment.newInstance(parameters)
            .show(activity.supportFragmentManager, DIALOG_REMOVE_RESERVATION)
    }

    private fun openSwapReservationDialog(
        activity: FragmentActivity,
        parameters: SwapRequestParameters
    ) {
        SwapReservationDialogFragment.newInstance(parameters)
            .show(activity.supportFragmentManager, DIALOG_SWAP_RESERVATION)
    }

    private fun findSpeakerHeadshot(speakers: ViewGroup, speakerId: SpeakerId): View {
        speakers.forEach {
            if (it.getTag(R.id.tag_speaker_id) == speakerId) {
                return it.findViewById(R.id.speaker_item_headshot)
            }
        }
        Timber.e("Could not find view for speaker id $speakerId")
        return speakers
    }

    private fun addToCalendar(session: Session) {
        val intent = Intent(Intent.ACTION_INSERT)
            .setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.Events.TITLE, session.title)
            .putExtra(CalendarContract.Events.EVENT_LOCATION, session.room?.name)
            .putExtra(CalendarContract.Events.DESCRIPTION, session.getCalendarDescription(
                getString(R.string.paragraph_delimiter),
                getString(R.string.speaker_delimiter)
            ))
            .putExtra(
                CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                session.startTime.toEpochMilli()
            )
            .putExtra(
                CalendarContract.EXTRA_EVENT_END_TIME,
                session.endTime.toEpochMilli()
            )
        if (intent.resolveActivity(requireContext().packageManager) != null) {
            startActivity(intent)
        }
    }

    private fun openFeedbackDialog(sessionId: String) {
        SessionFeedbackFragment.createInstance(sessionId)
            .show(childFragmentManager, FRAGMENT_SESSION_FEEDBACK)
    }

    companion object {
        private const val FRAGMENT_SESSION_FEEDBACK = "feedback"

        private const val EXTRA_SESSION_ID = "SESSION_ID"

        fun newInstance(sessionId: SessionId, openRateSession: Boolean): SessionDetailFragment {
            val bundle = Bundle().apply {
                putString(EXTRA_SESSION_ID, sessionId)
                putBoolean(AlarmBroadcastReceiver.EXTRA_SHOW_RATE_SESSION_FLAG, openRateSession)
            }
            return SessionDetailFragment().apply { arguments = bundle }
        }
    }
}
