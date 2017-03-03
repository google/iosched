/*
 * Copyright (c) 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.samples.apps.iosched.util;

import android.test.suitebuilder.annotation.SmallTest;

import com.google.samples.apps.iosched.BuildConfig;
import com.google.samples.apps.iosched.Config;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ParserUtils.class})
@SmallTest
public class UIUtilsTest {

    @Before
    public void setUpDates() {
        setUpParserUtils();
    }

    @Test
    public void startTimeToDayIndex_BeforeStart_ReturnsZero() {
        // Given a start time 24 hours before the start of the conference
        long startTime = Config.CONFERENCE_START_MILLIS - TimeUtils.HOUR * 24;

        // When getting the day index for the start time
        int index = UIUtils.startTimeToDayIndex(startTime);

        // Then the index is 0
        assertThat(index, is(0));
    }

    @Test
    public void startTimeToDayIndex_FirstDay_ReturnsZero() {
        // Given a start time 1 hour after the start of the conference
        long startTime = Config.CONFERENCE_START_MILLIS + TimeUtils.HOUR * 1;

        // When getting the day index for the start time
        int index = UIUtils.startTimeToDayIndex(startTime);

        // Then the index is 0
        assertThat(index, is(0));
    }

    @Test
    public void startTimeToDayIndex_LastDay_ReturnsLastDay() {
        // Given a start time 1 hour1 before the end of the conference
        long startTime = Config.CONFERENCE_END_MILLIS - TimeUtils.HOUR * 1;

        // When getting the day index for the start time
        int index = UIUtils.startTimeToDayIndex(startTime);

        // Then the index is the last day of the conference
        int lastDay = Config.CONFERENCE_DAYS.length - 1;
        assertThat(index, is(lastDay));
    }

    @Test
    public void startTimeToDayIndex_OneHourBeforeStartOfLastDay_ReturnsZero() {
        // Given a start time 1 hour before the start of the last day of the conference
        long startTime =
                Config.CONFERENCE_DAYS[Config.CONFERENCE_DAYS.length - 1][0] - TimeUtils.HOUR * 1;

        // When getting the day index for the start time
        int index = UIUtils.startTimeToDayIndex(startTime);

        // Then the index is 0
        assertThat(index, is(0));
    }

    @Test
    public void startTimeToDayIndex_AfterEnd_ReturnsLastDay() {
        // Given a start time 24 hours after the start of the conference
        long startTime = Config.CONFERENCE_END_MILLIS + TimeUtils.HOUR * 24;

        // When getting the day index for the start time
        int index = UIUtils.startTimeToDayIndex(startTime);

        // Then the index is the last day of the conference
        int lastDay = Config.CONFERENCE_DAYS.length - 1;
        assertThat(index, is(lastDay));
    }

    /**
     * Conference config date setup uses {@link ParserUtils#parseTime(String)}, so mocking the
     * output.
     */
    private void setUpParserUtils() {
        PowerMockito.mockStatic(ParserUtils.class);
        long conferenceDayDuration = 12 * TimeUtils.HOUR;
        BDDMockito.given(ParserUtils.parseTime(BuildConfig.CONFERENCE_DAY1_START))
                  .willReturn(0L);
        BDDMockito.given(ParserUtils.parseTime(BuildConfig.CONFERENCE_DAY1_END))
                  .willReturn(conferenceDayDuration);
        BDDMockito.given(ParserUtils.parseTime(BuildConfig.CONFERENCE_DAY2_START))
                  .willReturn((long) (24 * TimeUtils.HOUR));
        BDDMockito.given(ParserUtils.parseTime(BuildConfig.CONFERENCE_DAY2_END))
                  .willReturn((long) (24 * TimeUtils.HOUR) + conferenceDayDuration);
        BDDMockito.given(ParserUtils.parseTime(BuildConfig.CONFERENCE_DAY3_START))
                  .willReturn((long) (2 * 24 * TimeUtils.HOUR));
        BDDMockito.given(ParserUtils.parseTime(BuildConfig.CONFERENCE_DAY3_END))
                  .willReturn((long) (2 * 24 * TimeUtils.HOUR) + conferenceDayDuration);
    }

}
