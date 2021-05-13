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
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ShareCompat
import androidx.core.net.toUri
import androidx.core.view.doOnLayout
import androidx.core.view.forEach
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import androidx.transition.TransitionInflater
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.R.style
import com.google.samples.apps.iosched.databinding.FragmentSessionDetailBinding
import com.google.samples.apps.iosched.model.Session
import com.google.samples.apps.iosched.model.SpeakerId
import com.google.samples.apps.iosched.shared.analytics.AnalyticsActions
import com.google.samples.apps.iosched.shared.analytics.AnalyticsHelper
import com.google.samples.apps.iosched.shared.di.MapFeatureEnabledFlag
import com.google.samples.apps.iosched.shared.domain.users.SwapRequestParameters
import com.google.samples.apps.iosched.shared.notifications.AlarmBroadcastReceiver
import com.google.samples.apps.iosched.shared.result.successOr
import com.google.samples.apps.iosched.shared.util.toEpochMilli
import com.google.samples.apps.iosched.ui.messages.SnackbarMessageManager
import com.google.samples.apps.iosched.ui.reservation.RemoveReservationDialogFragment
import com.google.samples.apps.iosched.ui.reservation.RemoveReservationDialogFragment.Companion.DIALOG_REMOVE_RESERVATION
import com.google.samples.apps.iosched.ui.reservation.RemoveReservationDialogParameters
import com.google.samples.apps.iosched.ui.reservation.SwapReservationDialogFragment
import com.google.samples.apps.iosched.ui.reservation.SwapReservationDialogFragment.Companion.DIALOG_SWAP_RESERVATION
import com.google.samples.apps.iosched.ui.schedule.ScheduleTwoPaneViewModel
import com.google.samples.apps.iosched.ui.sessiondetail.SessionDetailNavigationAction.NavigateToSession
import com.google.samples.apps.iosched.ui.sessiondetail.SessionDetailNavigationAction.NavigateToSessionFeedback
import com.google.samples.apps.iosched.ui.sessiondetail.SessionDetailNavigationAction.NavigateToSignInDialogAction
import com.google.samples.apps.iosched.ui.sessiondetail.SessionDetailNavigationAction.NavigateToSpeakerDetail
import com.google.samples.apps.iosched.ui.sessiondetail.SessionDetailNavigationAction.NavigateToSwapReservationDialogAction
import com.google.samples.apps.iosched.ui.sessiondetail.SessionDetailNavigationAction.NavigateToYoutube
import com.google.samples.apps.iosched.ui.sessiondetail.SessionDetailNavigationAction.RemoveReservationDialogAction
import com.google.samples.apps.iosched.ui.sessiondetail.SessionDetailNavigationAction.ShowNotificationsPrefAction
import com.google.samples.apps.iosched.ui.signin.NotificationsPreferenceDialogFragment
import com.google.samples.apps.iosched.ui.signin.NotificationsPreferenceDialogFragment.Companion.DIALOG_NOTIFICATIONS_PREFERENCE
import com.google.samples.apps.iosched.ui.signin.SignInDialogFragment
import com.google.samples.apps.iosched.ui.signin.SignInDialogFragment.Companion.DIALOG_SIGN_IN
import com.google.samples.apps.iosched.util.doOnApplyWindowInsets
import com.google.samples.apps.iosched.util.openWebsiteUrl
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class SessionDetailFragment : Fragment(), SessionFeedbackFragment.Listener {

    private var shareString = ""

    @Inject
    lateinit var snackbarMessageManager: SnackbarMessageManager

    private val sessionDetailViewModel: SessionDetailViewModel by viewModels()
    private val scheduleTwoPaneViewModel: ScheduleTwoPaneViewModel by activityViewModels()

    @Inject
    lateinit var analyticsHelper: AnalyticsHelper

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
    ): View {
        sharedElementReturnTransition =
            TransitionInflater.from(context).inflateTransition(R.transition.speaker_shared_enter)
        // Delay the enter transition until speaker image has loaded.
        postponeEnterTransition(500L, TimeUnit.MILLISECONDS)

        val themedInflater =
            inflater.cloneInContext(ContextThemeWrapper(requireActivity(), style.AppTheme_Detail))
        binding = FragmentSessionDetailBinding.inflate(themedInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewModel = sessionDetailViewModel
        binding.lifecycleOwner = viewLifecycleOwner

        binding.sessionDetailBottomAppBar.run {
            inflateMenu(R.menu.session_detail_menu)
            menu.findItem(R.id.menu_item_map)?.isVisible = isMapEnabled
            lifecycleScope.launchWhenStarted {
                sessionDetailViewModel.session.collect { session ->
                    menu.findItem(R.id.menu_item_ask_question).isVisible =
                        session?.doryLink?.isNotBlank() == true
                }
            }
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_item_share -> {
                        ShareCompat.IntentBuilder.from(requireActivity())
                            .setType("text/plain")
                            .setText(shareString)
                            .setChooserTitle(R.string.intent_chooser_session_detail)
                            .startChooser()
                    }
                    R.id.menu_item_star -> {
                        sessionDetailViewModel.userSession.value?.let(
                            scheduleTwoPaneViewModel::onStarClicked
                        )
                    }
                    R.id.menu_item_map -> {
                        // TODO support opening Map
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
            tagRecycledViewPool,
            scheduleTwoPaneViewModel
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
        lifecycleScope.launchWhenStarted {
            sessionDetailViewModel.relatedUserSessions.collect {
                detailsAdapter.related = it.successOr(emptyList())
            }
        }

        lifecycleScope.launchWhenStarted {
            sessionDetailViewModel.session.collect { session ->
                shareString = if (session == null) {
                    ""
                } else {
                    getString(R.string.share_text_session_detail, session.title, session.sessionUrl)
                }
                detailsAdapter.speakers = session?.speakers?.toList() ?: emptyList()
                // ViewBinding is binding the session so we should wait until after the session has
                // been laid out to report fully drawn. Note that we are *not* waiting for the
                // speaker images to be downloaded and displayed because we are showing a
                // placeholder image. Thus the screen appears fully drawn to the user. In terms of
                // performance, this allows us to obtain a stable start up times by not including
                // the network call to download images, which can vary greatly based on
                // uncontrollable factors, mainly network speed.
                binding.sessionDetailRecyclerView.doOnLayout {
                    // If this activity was launched from a deeplink, then the logcat statement is
                    // printed. Otherwise, SessionDetailFragment is started from the MainActivity
                    // which would have already reported fully drawn to the framework.
                    activity?.reportFullyDrawn()
                }
            }
        }

        // Navigation
        lifecycleScope.launchWhenStarted {
            sessionDetailViewModel.navigationActions.collect { action ->
                when (action) {
                    is NavigateToSession -> findNavController().navigate(
                        SessionDetailFragmentDirections.toSessionDetail(action.sessionId)
                    )
                    is NavigateToSessionFeedback -> openFeedbackDialog(action.sessionId)
                    NavigateToSignInDialogAction -> openSignInDialog(requireActivity())
                    is NavigateToSpeakerDetail -> {
                        val sharedElement = findSpeakerHeadshot(
                            binding.sessionDetailRecyclerView,
                            action.speakerId
                        )
                        findNavController().navigate(
                            SessionDetailFragmentDirections.toSpeakerDetail(action.speakerId),
                            FragmentNavigatorExtras(sharedElement to sharedElement.transitionName)
                        )
                    }
                    is NavigateToSwapReservationDialogAction ->
                        openSwapReservationDialog(requireActivity(), action.params)
                    is NavigateToYoutube -> openYoutubeUrl(action.videoId)
                    is RemoveReservationDialogAction -> openRemoveReservationDialog(
                        requireActivity(), action.params
                    )
                    ShowNotificationsPrefAction -> openNotificationsPreferenceDialog()
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            // Only show the back/up arrow in the toolbar in single-pane configurations.
            scheduleTwoPaneViewModel.isTwoPane.collect { isTwoPane ->
                if (isTwoPane) {
                    binding.toolbar.navigationIcon = null
                    binding.toolbar.setNavigationOnClickListener(null)
                } else {
                    binding.toolbar.navigationIcon =
                        AppCompatResources.getDrawable(requireContext(), R.drawable.ic_arrow_back)
                    binding.toolbar.setNavigationOnClickListener {
                        scheduleTwoPaneViewModel.returnToListPane()
                    }
                }
            }
        }

        // When opened from the post session notification, open the feedback dialog
        requireNotNull(arguments).apply {
            val sessionId = SessionDetailFragmentArgs.fromBundle(this).sessionId
            val openRateSession =
                arguments?.getBoolean(AlarmBroadcastReceiver.EXTRA_SHOW_RATE_SESSION_FLAG) ?: false
            lifecycleScope.launchWhenStarted {
                sessionDetailViewModel.showFeedbackButton.collect {
                    if (it && openRateSession) {
                        openFeedbackDialog(sessionId)
                    }
                }
            }
        }

        // Observing the changes from Fragment because data binding doesn't work with menu items.
        val menu = binding.sessionDetailBottomAppBar.menu
        val starMenu = menu.findItem(R.id.menu_item_star)
        lifecycleScope.launchWhenStarted {
            sessionDetailViewModel.shouldShowStarInBottomNav.collect { showStar ->
                starMenu.isVisible = showStar
            }
        }
        lifecycleScope.launchWhenStarted {
            sessionDetailViewModel.userEvent.collect { userEvent ->
                userEvent?.let {
                    if (it.isStarred) {
                        starMenu.setIcon(R.drawable.ic_star)
                    } else {
                        starMenu.setIcon(R.drawable.ic_star_border)
                    }
                }
            }
        }

        var titleUpdated = false
        lifecycleScope.launchWhenStarted {
            sessionDetailViewModel.session.collect {
                if (it != null && !titleUpdated) {
                    sessionTitle = it.title
                    activity?.let { activity ->
                        analyticsHelper.sendScreenView("Session Details: $sessionTitle", activity)
                    }
                    titleUpdated = true
                }
            }
        }
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
            .putExtra(
                CalendarContract.Events.DESCRIPTION,
                session.getCalendarDescription(
                    getString(R.string.paragraph_delimiter),
                    getString(R.string.speaker_delimiter)
                )
            )
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
    }
}
