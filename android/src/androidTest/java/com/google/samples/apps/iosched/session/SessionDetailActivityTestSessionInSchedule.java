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

import com.google.samples.apps.iosched.Config;
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
import static android.support.test.espresso.action.ViewActions.swipeUp;
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

/**
 * Tests for {@link SessionDetailActivity} when showing a session that is not the keynote and that
 * is in user schedule, and that is ready for feedback submission.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class SessionDetailActivityTestSessionInSchedule {

    public static final String SESSION_ID = "5b7836c8-82bf-e311-b297-00155d5066d7";

    private static final String FAKE_TITLE = "FAKE TITLE";

    private static final String FAKE_SPEAKER = "FAKE SPEAKER";

    private static final String FAKE_TAG_ID1 = "id1";

    private static final String FAKE_TAG_NAME1 = "Tag1";

    private static final String FAKE_TAG_ID2 = "id2";

    private static final String FAKE_TAG_NAME2 = "Tag2";

    private static final String FAKE_TAG_STRING = FAKE_TAG_ID1 + "," + FAKE_TAG_ID2;

    private static final String FAKE_TAG_CATEGORY = Config.Tags.CATEGORY_TOPIC;

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
        // Create a fake model to simulate a session in schedule.
        SessionDetailModel fakeModel = new FakeSessionDetailModelInSchedule(mSessionUri,
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
    public void speakersSection_IsVisible() {
        onView(withId(R.id.session_detail_frag)).perform(swipeUp());
        onView(withId(R.id.session_speakers_block)).check(matches(isDisplayed()));
    }

    @Test
    public void tagSection_IsVisible() {
        onView(withId(R.id.session_tags_container)).perform(scrollTo()).
                check(matches(isDisplayed()));
    }

    @Test
    @Ignore("Will be written with Intento")
    public void tag_OnClick_IntentFired() {
    }

    @Test
    @Ignore("Will be written with Intento")
    public void youTubeVideo_WhenClicked_IntentFired() {
    }

    @Test
    public void feedbackCard_IsVisible() {
        onView(withId(R.id.give_feedback_card)).perform(scrollTo()).check(matches(isDisplayed()));
    }

    @Test
    @Ignore("Will be written with Intento")
    public void feedbackCard_OnClick_IntentFired() {
        onView(withId(R.id.give_feedback_card)).perform(click());
    }

    /**
     * Fakes data for a session in user schedule, not keynote, with no live stream, no feedback,
     * ready for feedback,1 speaker, 2 tags, and a youtube link.
     */
    public static class FakeSessionDetailModelInSchedule extends SessionDetailModel {

        public FakeSessionDetailModelInSchedule(Uri sessionUri, Context context,
                SessionsHelper sessionsHelper) {
            super(sessionUri, context, sessionsHelper);
        }

        @Override
        public boolean isKeynote() {
            return false;
        }

        @Override
        public boolean isInSchedule() {
            return true;
        }

        @Override
        public boolean isInScheduleWhenSessionFirstLoaded() {
            return true;
        }

        @Override
        public boolean hasLiveStream() {
            return false;
        }

        @Override
        public boolean hasFeedback() {
            return false;
        }

        @Override
        public boolean isSessionReadyForFeedback() {
            return true;
        }

        @Override
        public String getSessionTitle() {
            return FAKE_TITLE;
        }

        @Override
        public TagMetadata getTagMetadata() {
            return new FakeTagMetadata();
        }

        @Override
        public String getTagsString() {
            return FAKE_TAG_STRING;
        }


        @Override
        public List<Pair<Integer, Intent>> getLinks() {
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

    /**
     * Fakes tag metadata with 2 tags for category Config.Tags.CATEGORY_TOPIC.
     */
    private static class FakeTagMetadata extends TagMetadata {

        public FakeTagMetadata() {
            super();
        }

        @Override
        public Tag getTag(String tagId) {
            if (tagId.equals(FAKE_TAG_ID1)) {
                return new Tag(FAKE_TAG_ID1, FAKE_TAG_NAME1, FAKE_TAG_CATEGORY, 0, null, 256);
            } else if (tagId.equals(FAKE_TAG_ID1)) {
                return new Tag(FAKE_TAG_ID2, FAKE_TAG_NAME2, FAKE_TAG_CATEGORY, 1, null, 0);
            }
            return null;
        }
    }
}
