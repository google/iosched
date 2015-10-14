/*
 * Copyright 2015 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.iosched.session;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.framework.PresenterFragmentImpl;
import com.google.samples.apps.iosched.model.TagMetadata;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.settings.SettingsUtils;
import com.google.samples.apps.iosched.ui.BaseActivity;
import com.google.samples.apps.iosched.util.SessionsHelper;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

/**
 * Tests for {@link SessionDetailActivity} when showing a keynote session.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class SessionDetailActivityTestKeynoteSession {

    public static final String SESSION_ID = "__keynote__";

    private static final String FAKE_TITLE = "FAKE TITLE";

    private Uri mSessionUri;

    @Rule
    public ActivityTestRule<SessionDetailActivity> mActivityRule =
            new ActivityTestRule<SessionDetailActivity>(SessionDetailActivity.class) {
                @Override
                protected Intent getActivityIntent() {
                    // Make sure the EULA screen is not shown.
                    SettingsUtils.markTosAccepted(InstrumentationRegistry.getTargetContext(), true);

                    // Create intent to load the keynote session.
                    mSessionUri = ScheduleContract.Sessions.buildSessionUri(SESSION_ID);
                    Intent intent = new Intent(Intent.ACTION_VIEW, mSessionUri);

                    return intent;
                }
            };

    @Before
    public void setUpModel() {
        // Create a fake model to simulate a keynote session.
        SessionDetailModel fakeModel = new FakeSessionDetailModelKeynote(mSessionUri,
                mActivityRule.getActivity().getApplicationContext(),
                new SessionsHelper(mActivityRule.getActivity()));

        // Set up the presenter with the fake model.
        final PresenterFragmentImpl presenter = mActivityRule.getActivity()
                .addPresenterFragment(R.id.session_detail_frag, fakeModel,
                        SessionDetailModel.SessionDetailQueryEnum.values(),
                        SessionDetailModel.SessionDetailUserActionEnum.values());

        mActivityRule.getActivity().runOnUiThread(new Runnable() {

            @Override
            public void run() {
                presenter.getLoaderManager().restartLoader(0, null, presenter);
            }
        });
    }

    @Test
    public void checkPreconditions() {
        // Check the session uri
        assertEquals(mSessionUri, mActivityRule.getActivity().getSessionUri());

        // Check the presenter exists
        assertNotNull(mActivityRule.getActivity().getFragmentManager()
                .findFragmentByTag(BaseActivity.PRESENTER_TAG));

        PresenterFragmentImpl presenter = (PresenterFragmentImpl) mActivityRule.getActivity()
                .getFragmentManager().findFragmentByTag(BaseActivity.PRESENTER_TAG);

        // Check the model is the injected fake implementation. Also check, initial queries and
        // valid user actions of the Presenter. No need to check the UpdatableView because other
        // tests will fail if expected views aren't present
        assertThat("The presenter model should be an instance of SessionDetailModel",
                presenter.getModel(), instanceOf(SessionDetailModel.class));
        assertTrue(Arrays.equals(SessionDetailModel.SessionDetailQueryEnum.values(),
                presenter.getInitialQueriesToLoad()));
        assertTrue(Arrays.equals(SessionDetailModel.SessionDetailUserActionEnum.values(),
                presenter.getValidUserActions()));
        assertThat(((SessionDetailModel) presenter.getModel()).getSessionTitle(),
                is(FAKE_TITLE));
    }

    @Test
    public void sessionTitle_ShowsCorrectTitle() {
        onView(withId(R.id.session_title)).check(matches(
                allOf(withText(FAKE_TITLE), isDisplayed())));
    }

    @Test
    @Ignore("Will be written with Intento")
    public void submitFeedback_WhenClicked_IntentFired() {
    }

    @Test
    @Ignore("Will be written with Intento")
    public void showMap_WhenClicked_IntentFired() {
        onView(withId(R.id.menu_map_room)).perform(click());
    }

    @Test
    @Ignore("Will be written with Intento")
    public void showShare_WhenClicked_IntentFired() {
        onView(withId(R.id.menu_share)).perform(click());
    }

    @Test
    public void speakersSection_IsNotVisible() {
        onView(withId(R.id.session_speakers_block)).check(matches(not(isDisplayed())));
    }

    @Test
    public void tagSection_IsNotVisible() {
        onView(withId(R.id.session_tags_container)).check(matches(not(isDisplayed())));
    }

    @Test
    @Ignore("Will be written with Intento")
    public void youTubeVideo_WhenClicked_IntentFired() {
    }

    @Test
    public void feedbackCard_IsVisible() {
        onView(withId(R.id.give_feedback_card)).perform(scrollTo()).check(matches(isDisplayed()));
    }

    /**
     * Fakes data for a keynote session, with no live stream, no feedback, ready for feedback, no
     * speakers, no tag metadata, and a youtube link.
     */
    public static class FakeSessionDetailModelKeynote extends SessionDetailModel {

        public FakeSessionDetailModelKeynote(Uri sessionUri, Context context,
                SessionsHelper sessionsHelper) {
            super(sessionUri, context, sessionsHelper);
        }

        @Override
        public boolean isKeynote(){
            return true;
        }

        @Override
        public boolean hasLiveStream(){
            return false;
        }

        @Override
        public boolean hasFeedback(){
            return false;
        }

        @Override
        public boolean isInScheduleWhenSessionFirstLoaded() {
            return true;
        }

        @Override
        public boolean isSessionReadyForFeedback(){
            return true;
        }

        @Override
        public String getSessionTitle(){
            return FAKE_TITLE;
        }

        @Override
        public List<Speaker> getSpeakers(){
            return new ArrayList<Speaker>();
        }

        @Override
        public TagMetadata getTagMetadata(){
            return null;
        }

        @Override
        public List<Pair<Integer, Intent>> getLinks(){
            List<Pair<Integer, Intent>> links = new ArrayList<Pair<Integer, Intent>>();

            links.add(new Pair<Integer, Intent>(
                    R.string.session_feedback_submitlink,
                    getFeedbackIntent()
            ));

            links.add(new Pair<Integer, Intent>(
                    R.string.session_link_youtube,
                    new Intent(Intent.ACTION_VIEW, Uri.parse("http://youtube.com/"))
            ));

            return links;
        }
    }
}
