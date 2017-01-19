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
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;
import com.google.samples.apps.iosched.BuildConfig;
import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.archframework.Model;
import com.google.samples.apps.iosched.archframework.QueryEnum;
import com.google.samples.apps.iosched.archframework.UserActionEnum;
import com.google.samples.apps.iosched.model.ScheduleHelper;
import com.google.samples.apps.iosched.model.ScheduleItem;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.settings.SettingsUtils;
import com.google.samples.apps.iosched.util.AnalyticsHelper;
import com.google.samples.apps.iosched.util.ParserUtils;
import com.google.samples.apps.iosched.util.ThrottledContentObserver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.LOGE;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

public class MyScheduleModel implements Model<MyScheduleModel.MyScheduleQueryEnum,
        MyScheduleModel.MyScheduleUserActionEnum> {

    private static final String TAG = makeLogTag(MyScheduleModel.class);

    public static final int PRE_CONFERENCE_DAY_ID = 0;

    /**
     * Used for user action {@link MyScheduleUserActionEnum#SESSION_SLOT}
     */
    public static final String SESSION_URL_KEY = "SESSION_URL_KEY";

    /**
     * Used for user action {@link MyScheduleUserActionEnum#FEEDBACK}
     */
    public static final String SESSION_ID_KEY = "SESSION_ID_KEY";

    /**
     * Used for user action {@link MyScheduleUserActionEnum#FEEDBACK}
     */
    public static final String SESSION_TITLE_KEY = "SESSION_TITLE_KEY";

    /**
     * The key of {@link #mScheduleData} is the index of the day in the conference, starting at 1
     * for the first day of the conference, using {@link #PRE_CONFERENCE_DAY_ID} for the
     * preconference day, if any.
     */
    protected HashMap<Integer, ArrayList<ScheduleItem>> mScheduleData =
            new HashMap<Integer, ArrayList<ScheduleItem>>();

    // The ScheduleHelper is responsible for feeding data in a format suitable to the Adapter.
    private ScheduleHelper mScheduleHelper;

    private Context mContext;

    protected DataQueryCallback mScheduleDataQueryCallback;

    /**
     * @param scheduleHelper
     * @param context        Should be an Activity context
     */
    public MyScheduleModel(ScheduleHelper scheduleHelper, Context context) {
        mContext = context;
        mScheduleHelper = scheduleHelper;
    }

    /**
     * Initialises the pre conference data and data observers. This is not called from the
     * constructor, to allow for unit tests to bypass this (as this uses Android methods not
     * available in unit tests).
     *
     * @return the Model it can be chained with the constructor
     */
    public MyScheduleModel initStaticDataAndObservers() {
        if (showPreConferenceData(mContext)) {
            preparePreConferenceDayAdapter();
        }

        addDataObservers();
        return this;
    }

    /**
     * This method is an ad-hoc implementation of the pre conference day, which contains an item to
     * pick up the badge at registration desk
     */
    private void preparePreConferenceDayAdapter() {
        ScheduleItem item = new ScheduleItem();
        item.title = mContext.getString(R.string.my_schedule_badgepickup);
        item.startTime = ParserUtils.parseTime(BuildConfig.PRECONFERENCE_DAY_START);
        item.endTime = ParserUtils.parseTime(BuildConfig.PRECONFERENCE_DAY_END);
        item.type = ScheduleItem.BREAK;
        item.room =
                item.subtitle = mContext.getString(R.string.my_schedule_badgepickup_description);
        item.sessionType = ScheduleItem.SESSION_TYPE_MISC;
        mScheduleData.put(PRE_CONFERENCE_DAY_ID, new ArrayList<ScheduleItem>(Arrays.asList(item)));
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener mPrefChangeListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
                    LOGD(TAG, "sharedpreferences key " + key + " changed, maybe reloading data.");
                    if (SettingsUtils.PREF_LOCAL_TIMES.equals(key)) {
                        // mPrefChangeListener is observing as soon as the model is created but
                        // mScheduleDataQueryCallback is only created when the view has requested
                        // some data. There is a tiny amount of time when mPrefChangeListener is
                        // active but mScheduleDataQueryCallback is null. This was observed when
                        // going to MySchedule screen straight after the welcome flow.
                        if (mScheduleDataQueryCallback != null) {
                            mScheduleDataQueryCallback.onModelUpdated(MyScheduleModel.this,
                                    MyScheduleQueryEnum.SCHEDULE);
                        } else {
                            LOGE(TAG, "sharedpreferences key " + key +
                                    " changed, but null schedule data query callback, cannot " +
                                    "inform model is updated");
                        }
                    } else if (BuildConfig.PREF_ATTENDEE_AT_VENUE.equals(key)) {
                        updateData(mScheduleDataQueryCallback);
                    }
                }
            };

    /**
     * Observe changes on base uri and in shared preferences
     */
    private void addDataObservers() {
        mContext.getContentResolver().registerContentObserver(
                ScheduleContract.BASE_CONTENT_URI, true, mObserver);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        sp.registerOnSharedPreferenceChangeListener(mPrefChangeListener);
    }

    private void removeDataObservers() {
        mContext.getContentResolver().unregisterContentObserver(mObserver);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        sp.unregisterOnSharedPreferenceChangeListener(mPrefChangeListener);
    }

    /**
     * Visible for classes extending this model, so UI tests can be written to simulate the system
     * firing this observer.
     */
    @VisibleForTesting
    protected final ThrottledContentObserver mObserver = new ThrottledContentObserver(
            new ThrottledContentObserver.Callbacks() {
                @Override
                public void onThrottledContentObserverFired() {
                    LOGD(TAG, "content may be changed, reloading data");
                    updateData(mScheduleDataQueryCallback);
                }
            });

    @Override
    public MyScheduleQueryEnum[] getQueries() {
        return MyScheduleQueryEnum.values();
    }

    @Override
    public MyScheduleUserActionEnum[] getUserActions() {
        return MyScheduleUserActionEnum.values();
    }

    /**
     * @param day The day of the conference, starting at 1 for the first day
     * @return the list of items, or an empty list if the day isn't found
     */
    public ArrayList<ScheduleItem> getConferenceDataForDay(int day) {
        if (mScheduleData.containsKey(day)) {
            return mScheduleData.get(day);
        } else {
            return new ArrayList<>();
        }
    }

    public static boolean showPreConferenceData(Context context) {
        return SettingsUtils.isAttendeeAtVenue(context);
    }

    @Override
    public void deliverUserAction(final MyScheduleUserActionEnum action, @Nullable Bundle args,
            final UserActionCallback callback) {
        switch (action) {
            case RELOAD_DATA:
                DataQueryCallback queryCallback = new DataQueryCallback() {
                    @Override
                    public void onModelUpdated(Model model, QueryEnum query) {
                        callback.onModelUpdated(MyScheduleModel.this, action);
                    }

                    @Override
                    public void onError(QueryEnum query) {
                        callback.onError(action);
                    }
                };
                if (mScheduleDataQueryCallback == null) {
                    mScheduleDataQueryCallback = queryCallback;
                }
                updateData(queryCallback);
                break;
            case SESSION_SLOT:
                if (args == null || !args.containsKey(SESSION_URL_KEY)) {
                    callback.onError(action);
                } else {
                    String uriStr = args.getString(SESSION_URL_KEY);

                    // ANALYTICS EVENT: Select a slot on My Agenda
                    // Contains: URI indicating session ID or time interval of slot
                    AnalyticsHelper.sendEvent("My Schedule", "selectslot", uriStr);

                    // No need to notify presenter, nothing to do
                }
                break;
            case FEEDBACK:
                if (args == null || !args.containsKey(SESSION_ID_KEY)
                        || !args.containsKey(SESSION_TITLE_KEY)) {
                    callback.onError(action);
                } else {
                    String title = args.getString(SESSION_TITLE_KEY);
                    String id = args.getString(SESSION_ID_KEY);

                    // ANALYTICS EVENT: Click on the "Send Feedback" action from Schedule page.
                    // Contains: The session title.
                    AnalyticsHelper.sendEvent("My Schedule", "Feedback", title);

                    // No need to notify presenter, nothing to do
                }
                break;
            case REDRAW_UI:
                // We use cached data
                callback.onModelUpdated(this, action);
                break;
            default:
                break;
        }
    }

    @Override
    public void requestData(@NonNull MyScheduleQueryEnum query,
            @NonNull DataQueryCallback callback) {
        checkNotNull(query);
        checkNotNull(callback);
        switch (query) {
            case SCHEDULE:
                mScheduleDataQueryCallback = callback;
                updateData(mScheduleDataQueryCallback);
                break;
            default:
                callback.onError(query);
                break;
        }
    }

    @Override
    public void cleanUp() {
        removeDataObservers();
    }

    /**
     * This updates the data, by calling {@link ScheduleHelper#getScheduleDataAsync
     * (LoadScheduleDataListener, long, long)} for each day. It is protected and not private, to
     * allow us to extend this class and use mock data in UI tests (refer {@code
     * StubMyScheduleModel} in {@code androidTest}).
     */
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

            // Update cached data
            mScheduleHelper.getScheduleDataAsync(
                    new LoadScheduleDataListener() {
                        @Override
                        public void onDataLoaded(ArrayList<ScheduleItem> scheduleItems) {
                            updateCache(dayId, scheduleItems, callback);
                        }
                    },
                    Config.CONFERENCE_DAYS[i][0], Config.CONFERENCE_DAYS[i][1]);
        }
    }

    /**
     * This updates the cached data for the day with id {@code dayId} with {@code scheduleItems}
     * then notifies the {@code callback}.It is protected and not private, to allow us to extend
     * this class and use mock data in UI tests (refer {@code StubMyScheduleModel} in {@code
     * androidTest}).
     */
    protected void updateCache(int dayId, ArrayList<ScheduleItem> scheduleItems,
            DataQueryCallback callback) {
        mScheduleData.put(dayId, scheduleItems);
        if (callback != null) {
            callback.onModelUpdated(MyScheduleModel.this,
                    MyScheduleQueryEnum.SCHEDULE);
        }
    }

    public enum MyScheduleQueryEnum implements QueryEnum {
        SCHEDULE(0, null);

        private int id;

        private String[] projection;

        MyScheduleQueryEnum(int id, String[] projection) {
            this.id = id;
            this.projection = projection;
        }

        @Override
        public int getId() {
            return id;
        }

        @Override
        public String[] getProjection() {
            return projection;
        }

    }

    public enum MyScheduleUserActionEnum implements UserActionEnum {
        RELOAD_DATA(1),
        // Click on a row in the schedule, it opens the session or a list of available sessions
        SESSION_SLOT(2),
        FEEDBACK(3),
        REDRAW_UI(4);

        private int id;

        MyScheduleUserActionEnum(int id) {
            this.id = id;
        }

        @Override
        public int getId() {
            return id;
        }

    }

    public interface LoadScheduleDataListener {
        void onDataLoaded(ArrayList<ScheduleItem> scheduleItems);
    }

}
