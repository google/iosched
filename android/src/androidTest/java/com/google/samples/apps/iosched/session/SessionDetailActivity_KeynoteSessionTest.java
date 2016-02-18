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

import android.content.Intent;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.injection.ModelProvider;
import com.google.samples.apps.iosched.mockdata.SessionsMockCursor;
import com.google.samples.apps.iosched.mockdata.SpeakersMockCursor;
import com.google.samples.apps.iosched.mockdata.TagMetadataMockCursor;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.settings.SettingsUtils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.core.IsNot.not;

/**
 * Tests for {@link SessionDetailActivity} when showing a keynote session.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class SessionDetailActivity_KeynoteSessionTest {

    public static final String SESSION_ID = "__keynote__";

    private Uri mSessionUri;

    @Rule
    public ActivityTestRule<SessionDetailActivity> mActivityRule =
            new ActivityTestRule<SessionDetailActivity>(SessionDetailActivity.class) {
                @Override
                protected Intent getActivityIntent() {
                    // Make sure the EULA screen is not shown.
                    SettingsUtils.markTosAccepted(InstrumentationRegistry.getTargetContext(), true);

                    // Create session uri
                    mSessionUri = ScheduleContract.Sessions.buildSessionUri(SESSION_ID);

                    // Create a stub model to simulate a keynote session
                    ModelProvider.setStubSessionDetailModel(new StubSessionDetailModel(mSessionUri,
                            InstrumentationRegistry.getTargetContext(),
                            SessionsMockCursor.getCursorForKeynoteSession(),
                            SpeakersMockCursor.getCursorForNoSpeaker(),
                            TagMetadataMockCursor.getCursorForSingleTagMetadata()));

                    // Create intent to load the keynote session.
                    Intent intent = new Intent(Intent.ACTION_VIEW, mSessionUri);
                    return intent;
                }
            };

    @Test
    public void sessionTitle_ShowsCorrectTitle() {
        onView(withId(R.id.session_title)).check(matches(
                allOf(withText(SessionsMockCursor.FAKE_TITLE_KEYNOTE), isDisplayed())));
    }

    @Test
    public void speakersSection_IsNotVisible() {
        onView(withId(R.id.session_speakers_block)).check(matches(not(isDisplayed())));
    }

    @Test
    public void tagSection_IsNotVisible() {
        onView(withId(R.id.session_tags_container)).check(matches(not(isDisplayed())));
    }

}
