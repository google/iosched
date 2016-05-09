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
import android.os.Handler;
import android.support.test.InstrumentationRegistry;

import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.model.ScheduleHelper;
import com.google.samples.apps.iosched.model.ScheduleItem;

import java.util.ArrayList;

/**
 * A stub {@link MyScheduleModel}, to be injected using {@link com.google.samples.apps.iosched
 * .injection.Injection}. It overrides {@link #updateData(DataQueryCallback)} to bypass the {@link
 * ScheduleHelper#getScheduleDataAsync(LoadScheduleDataListener, long, long)} mechanism. Use the
 * classes in {@link com.google.samples.apps.iosched.mockdata} to provide the mock data.
 */
public class StubMyScheduleModel extends MyScheduleModel {

    private ArrayList<ScheduleItem> mMockScheduleDataDay1;

    private ArrayList<ScheduleItem> mMockScheduleDataDay2;

    /**
     * @param context              Pass in the context using {@link
     * InstrumentationRegistry#getTargetContext()}
     * @param mockScheduleDataDay1 Stub data for day 1 of the conference
     * @param mockScheduleDataDay2 Stub data for day 2 of the conference
     */
    public StubMyScheduleModel(Context context,
            ArrayList<ScheduleItem> mockScheduleDataDay1,
            ArrayList<ScheduleItem> mockScheduleDataDay2) {
        super(null, context);
        mMockScheduleDataDay1 = mockScheduleDataDay1;
        mMockScheduleDataDay2 = mockScheduleDataDay2;
    }

    public void setMockScheduleDataDay1(ArrayList<ScheduleItem> mockScheduleDataDay1) {
        mMockScheduleDataDay1 = mockScheduleDataDay1;
    }

    public void setMockScheduleDataDay2(ArrayList<ScheduleItem> mockScheduleDataDay2) {
        mMockScheduleDataDay2 = mockScheduleDataDay2;
    }

    public void fireContentObserver() {
        mObserver.cancelPendingCallback();
        mObserver.onChange(true);
     }

    /**
     * This bypasses the use of {@link ScheduleHelper} to get the data and sets the data as per the
     * mock data passed in the constructor.
     */
    @Override
    protected void updateData(final DataQueryCallback callback) {
        for (int i = 0; i < Config.CONFERENCE_DAYS.length; i++) {
            /**
             * The key in {@link #mScheduleData} is 1 for the first day, 2 for the second etc
             */
            final int dayId = i + 1;

            // Immediately use cached data if available
            if (mScheduleData.containsKey(dayId)) {
                if (callback != null) {
                    callback.onModelUpdated(this, MyScheduleQueryEnum.SCHEDULE);
                }
            }

            // Update cached data with mock data, simulate delay
            Handler h = new Handler();
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    // Only update day 1 and day 2 with  mock data
                    if (dayId < 3) {
                        updateCache(dayId,
                                dayId == 1 ? mMockScheduleDataDay1 : mMockScheduleDataDay2,
                                callback);
                    }
                }
            };
            h.postDelayed(r, 200);
        }
    }

}
