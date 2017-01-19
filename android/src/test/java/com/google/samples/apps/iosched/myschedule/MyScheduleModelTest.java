/*
 * Copyright 2016 Google Inc. All rights reserved.
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

package com.google.samples.apps.iosched.myschedule;

import android.content.Context;
import android.content.SharedPreferences;
import android.test.suitebuilder.annotation.SmallTest;

import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.archframework.Model;
import com.google.samples.apps.iosched.model.ScheduleHelper;
import com.google.samples.apps.iosched.model.ScheduleItem;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class MyScheduleModelTest {

    private static final long FAKE_CURRENT_TIME_OFFSET = 0l;

    private static final String FAKE_TITLE_1 = "FAKE TITLE 1";

    private static final String FAKE_TITLE_2 = "FAKE TITLE 2";

    @Mock
    private Context mMockContext;

    @Mock
    private SharedPreferences mMockSharedPreferences;

    @Mock
    private ScheduleHelper mMockScheduleHelper;

    @Mock
    private ArrayList<ScheduleItem> mMockScheduleItems;

    @Mock
    private Model.DataQueryCallback mMockDataQueryCallback;

    @Mock
    private Model.UserActionCallback mMockUserActionCallback;

    @Mock
    private ScheduleItem mMockScheduleItem1;

    @Mock
    private ScheduleItem mMockScheduleItem2;

    @Captor
    private ArgumentCaptor<MyScheduleModel.LoadScheduleDataListener>
            mLoadScheduleDataCallbackCaptor;

    private MyScheduleModel mMyScheduleModel;

    @Before
    public void setUp() {
        // Init mocks
        initSharedPreferencesMock();
        initMockContextWithFakeCurrentTime();

        // Create an instance of the model.
        mMyScheduleModel = spy(new MyScheduleModel(mMockScheduleHelper, mMockContext));

    }

    @Test
    public void scheduleQuery_scheduleHelperCalled_dataUpdatedAndCallbackFired() {
        // Given mock schedule items
        setUpMockScheduleItems();
        int conferenceDays = Config.CONFERENCE_DAYS.length;

        // When requesting a data update
        mMyScheduleModel.requestData(MyScheduleModel.MyScheduleQueryEnum.SCHEDULE,
                mMockDataQueryCallback);

        // Then the schedule helper is called for each day
        verify(mMockScheduleHelper, times(conferenceDays)).getScheduleDataAsync(
                mLoadScheduleDataCallbackCaptor.capture(), anyLong(), anyLong());

        // Given the schedule helper returning the same mock schedule items for each day
        mLoadScheduleDataCallbackCaptor.getValue().onDataLoaded(mMockScheduleItems);

        // Then the model is updated with the mock schedule items for the last conference day
        // and callback is fired
        verify(mMyScheduleModel).updateCache(conferenceDays, mMockScheduleItems,
                mMockDataQueryCallback);
        assertThat(mMyScheduleModel.getConferenceDataForDay(conferenceDays).size(), is(2));
        assertThat(mMyScheduleModel.getConferenceDataForDay(conferenceDays).get(0).getTitle(),
                is(FAKE_TITLE_1));
        assertThat(mMyScheduleModel.getConferenceDataForDay(conferenceDays).get(1).getTitle(),
                is(FAKE_TITLE_2));
        verify(mMockDataQueryCallback).onModelUpdated(mMyScheduleModel,
                MyScheduleModel.MyScheduleQueryEnum.SCHEDULE);
    }

    @Test
    public void reloadDataUserAction_scheduleHelperCalled_dataUpdatedAndCallbackFired() {
        // Given mock schedule items
        setUpMockScheduleItems();
        int conferenceDays = Config.CONFERENCE_DAYS.length;

        // When delivering user action to reload the data
        mMyScheduleModel.deliverUserAction(MyScheduleModel.MyScheduleUserActionEnum.RELOAD_DATA,
                null, mMockUserActionCallback);

        // Then the schedule helper is called for each day
        verify(mMockScheduleHelper, times(conferenceDays)).getScheduleDataAsync(
                mLoadScheduleDataCallbackCaptor.capture(), anyLong(), anyLong());

        // Given the schedule helper returning the same mock schedule items for each day
        mLoadScheduleDataCallbackCaptor.getValue().onDataLoaded(mMockScheduleItems);

        // Then the model is updated with the mock schedule items for the last conference day
        // and callback is fired
        verify(mMyScheduleModel).updateCache(eq(conferenceDays), eq(mMockScheduleItems),
                any(Model.DataQueryCallback.class));
        assertThat(mMyScheduleModel.getConferenceDataForDay(conferenceDays).size(), is(2));
        assertThat(mMyScheduleModel.getConferenceDataForDay(conferenceDays).get(0).getTitle(),
                is(FAKE_TITLE_1));
        assertThat(mMyScheduleModel.getConferenceDataForDay(conferenceDays).get(1).getTitle(),
                is(FAKE_TITLE_2));
        verify(mMockUserActionCallback).onModelUpdated(mMyScheduleModel,
                MyScheduleModel.MyScheduleUserActionEnum.RELOAD_DATA);
    }

    @Test
    public void redrawUIUserAction_scheduleHelperNotCalled_CallbackFired() {
        // When delivering user action to redraw the UI
        mMyScheduleModel
                .deliverUserAction(MyScheduleModel.MyScheduleUserActionEnum.REDRAW_UI, null,
                        mMockUserActionCallback);

        // Then the schedule helper is not called
        verify(mMockScheduleHelper, never()).getScheduleDataAsync(
                mLoadScheduleDataCallbackCaptor.capture(), anyLong(), anyLong());

        // Then the callback is fired
        verify(mMockUserActionCallback).onModelUpdated(mMyScheduleModel,
                MyScheduleModel.MyScheduleUserActionEnum.REDRAW_UI);
    }

    private void initSharedPreferencesMock() {
        when(mMockContext.getPackageName()).thenReturn("mock_name");
        when(mMockContext.getSharedPreferences("mock_name_preferences", Context.MODE_PRIVATE))
                .thenReturn(mMockSharedPreferences);
    }

    private void initMockContextWithFakeCurrentTime() {
        when(mMockSharedPreferences.getLong("mock_current_time", eq(anyLong())))
                .thenReturn(FAKE_CURRENT_TIME_OFFSET);
    }

    private void setUpMockScheduleItems() {
        when(mMockScheduleItems.size()).thenReturn(2);
        when(mMockScheduleItems.get(0)).thenReturn(mMockScheduleItem1);
        when(mMockScheduleItems.get(1)).thenReturn(mMockScheduleItem2);
        when(mMockScheduleItem1.getTitle()).thenReturn(FAKE_TITLE_1);
        when(mMockScheduleItem2.getTitle()).thenReturn(FAKE_TITLE_2);
    }

}
