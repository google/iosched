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

package com.google.samples.apps.iosched.videolibrary;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.framework.PresenterFragmentImpl;
import com.google.samples.apps.iosched.settings.SettingsUtils;
import com.google.samples.apps.iosched.ui.BaseActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.swipeUp;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isClickable;
import static android.support.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;


@RunWith(AndroidJUnit4.class)
@LargeTest
public class VideoLibraryActivityTest {

    private static final String YEAR_2013 = "2013";

    private static final String YEAR_2014 = "2014";

    private static final String YEAR_2015 = "2015";

    private static final String TOPIC_KEYNOTES = "Keynotes";

    private PresenterFragmentImpl mPresenter;

    @Rule
    public ActivityTestRule<VideoLibraryActivity> mActivityRule =
            new ActivityTestRule<VideoLibraryActivity>(VideoLibraryActivity.class) {
                @Override
                protected Intent getActivityIntent() {
                    // Make sure the EULA screen is not shown.
                    SettingsUtils.markTosAccepted(InstrumentationRegistry.getTargetContext(), true);

                    // Create intent to load the video library.
                    return new Intent(InstrumentationRegistry.getInstrumentation()
                            .getTargetContext().getApplicationContext(),
                            VideoLibraryActivity.class);
                }
            };

    @Before
    public void setupMembersAndIdlingResource() {
        mPresenter = (PresenterFragmentImpl) mActivityRule.getActivity()
                .getFragmentManager().findFragmentByTag(BaseActivity.PRESENTER_TAG);
        Espresso.registerIdlingResources(mPresenter.getLoaderIdlingResource());
    }

    @After
    public void unregisterIdlingResource() {
        Espresso.unregisterIdlingResources(mPresenter.getLoaderIdlingResource());
    }

    @Test
    public void checkPreconditions() {
        // Check that the presenter exists.
        assertNotNull(mActivityRule.getActivity().getFragmentManager()
                .findFragmentByTag(BaseActivity.PRESENTER_TAG));

        // Check the model, initial queries and valid user actions of the Presenter
        // No need to check the UpdatableView because other tests will fail if expected views aren't
        // present.
        assertThat("The presenter model should be an instance of VideoLibraryModel",
                mPresenter.getModel(), instanceOf(VideoLibraryModel.class));
        assertThat(new VideoLibraryModel.VideoLibraryQueryEnum[]{
                        VideoLibraryModel.VideoLibraryQueryEnum.VIDEOS,
                        VideoLibraryModel.VideoLibraryQueryEnum.MY_VIEWED_VIDEOS},
                is(equalTo(mPresenter.getInitialQueriesToLoad())));
        assertThat(new VideoLibraryModel.VideoLibraryUserActionEnum[]{
                        VideoLibraryModel.VideoLibraryUserActionEnum.RELOAD,
                        VideoLibraryModel.VideoLibraryUserActionEnum.VIDEO_PLAYED},
                is(equalTo(mPresenter.getValidUserActions())));
    }

    @Test
    public void videosList_InitiallyHasVideoGroupTitleWithYear() {
        onView(withId(R.id.videos_collection_view)).check(matches(
                hasDescendant(withText(anyOf(
                        containsString(TOPIC_KEYNOTES), containsString(YEAR_2013),
                        containsString(YEAR_2014), containsString(YEAR_2015))))));
    }

    @Test
    public void videosList_InitiallyHasClickableVideos() {
        onView(withId(R.id.videos_collection_view)).check(matches(hasDescendant(isClickable())));
    }

    @Test
    public void headerBar_IsInitiallyDisplayed() {
        onView(withId(R.id.headerbar)).check(matches(isCompletelyDisplayed()));
    }

    @Test
    public void headerBar_HidesAfterSwipeUp() {
        ViewInteraction view = onView(withId(R.id.headerbar));

        // Swiping up should hide the header bar.
        onView(withId(R.id.videos_collection_view)).perform(swipeUp());
        onView(withId(R.id.videos_collection_view)).perform(swipeUp());

        // Check if the header bar is hidden.
        view.check(matches(not(isDisplayed())));
    }
}
