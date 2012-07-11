/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.actionbarsherlock.widget;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.database.DataSetObservable;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * <p>
 * This class represents a data model for choosing a component for handing a
 * given {@link Intent}. The model is responsible for querying the system for
 * activities that can handle the given intent and order found activities
 * based on historical data of previous choices. The historical data is stored
 * in an application private file. If a client does not want to have persistent
 * choice history the file can be omitted, thus the activities will be ordered
 * based on historical usage for the current session.
 * <p>
 * </p>
 * For each backing history file there is a singleton instance of this class. Thus,
 * several clients that specify the same history file will share the same model. Note
 * that if multiple clients are sharing the same model they should implement semantically
 * equivalent functionality since setting the model intent will change the found
 * activities and they may be inconsistent with the functionality of some of the clients.
 * For example, choosing a share activity can be implemented by a single backing
 * model and two different views for performing the selection. If however, one of the
 * views is used for sharing but the other for importing, for example, then each
 * view should be backed by a separate model.
 * </p>
 * <p>
 * The way clients interact with this class is as follows:
 * </p>
 * <p>
 * <pre>
 * <code>
 *  // Get a model and set it to a couple of clients with semantically similar function.
 *  ActivityChooserModel dataModel =
 *      ActivityChooserModel.get(context, "task_specific_history_file_name.xml");
 *
 *  ActivityChooserModelClient modelClient1 = getActivityChooserModelClient1();
 *  modelClient1.setActivityChooserModel(dataModel);
 *
 *  ActivityChooserModelClient modelClient2 = getActivityChooserModelClient2();
 *  modelClient2.setActivityChooserModel(dataModel);
 *
 *  // Set an intent to choose a an activity for.
 *  dataModel.setIntent(intent);
 * <pre>
 * <code>
 * </p>
 * <p>
 * <strong>Note:</strong> This class is thread safe.
 * </p>
 *
 * @hide
 */
class ActivityChooserModel extends DataSetObservable {

    /**
     * Client that utilizes an {@link ActivityChooserModel}.
     */
    public interface ActivityChooserModelClient {

        /**
         * Sets the {@link ActivityChooserModel}.
         *
         * @param dataModel The model.
         */
        public void setActivityChooserModel(ActivityChooserModel dataModel);
    }

    /**
     * Defines a sorter that is responsible for sorting the activities
     * based on the provided historical choices and an intent.
     */
    public interface ActivitySorter {

        /**
         * Sorts the <code>activities</code> in descending order of relevance
         * based on previous history and an intent.
         *
         * @param intent The {@link Intent}.
         * @param activities Activities to be sorted.
         * @param historicalRecords Historical records.
         */
        // This cannot be done by a simple comparator since an Activity weight
        // is computed from history. Note that Activity implements Comparable.
        public void sort(Intent intent, List<ActivityResolveInfo> activities,
                List<HistoricalRecord> historicalRecords);
    }

    /**
     * Listener for choosing an activity.
     */
    public interface OnChooseActivityListener {

        /**
         * Called when an activity has been chosen. The client can decide whether
         * an activity can be chosen and if so the caller of
         * {@link ActivityChooserModel#chooseActivity(int)} will receive and {@link Intent}
         * for launching it.
         * <p>
         * <strong>Note:</strong> Modifying the intent is not permitted and
         *     any changes to the latter will be ignored.
         * </p>
         *
         * @param host The listener's host model.
         * @param intent The intent for launching the chosen activity.
         * @return Whether the intent is handled and should not be delivered to clients.
         *
         * @see ActivityChooserModel#chooseActivity(int)
         */
        public boolean onChooseActivity(ActivityChooserModel host, Intent intent);
    }

    /**
     * Flag for selecting debug mode.
     */
    private static final boolean DEBUG = false;

    /**
     * Tag used for logging.
     */
    private static final String LOG_TAG = ActivityChooserModel.class.getSimpleName();

    /**
     * The root tag in the history file.
     */
    private static final String TAG_HISTORICAL_RECORDS = "historical-records";

    /**
     * The tag for a record in the history file.
     */
    private static final String TAG_HISTORICAL_RECORD = "historical-record";

    /**
     * Attribute for the activity.
     */
    private static final String ATTRIBUTE_ACTIVITY = "activity";

    /**
     * Attribute for the choice time.
     */
    private static final String ATTRIBUTE_TIME = "time";

    /**
     * Attribute for the choice weight.
     */
    private static final String ATTRIBUTE_WEIGHT = "weight";

    /**
     * The default name of the choice history file.
     */
    public static final String DEFAULT_HISTORY_FILE_NAME =
        "activity_choser_model_history.xml";

    /**
     * The default maximal length of the choice history.
     */
    public static final int DEFAULT_HISTORY_MAX_LENGTH = 50;

    /**
     * The amount with which to inflate a chosen activity when set as default.
     */
    private static final int DEFAULT_ACTIVITY_INFLATION = 5;

    /**
     * Default weight for a choice record.
     */
    private static final float DEFAULT_HISTORICAL_RECORD_WEIGHT = 1.0f;

    /**
     * The extension of the history file.
     */
    private static final String HISTORY_FILE_EXTENSION = ".xml";

    /**
     * An invalid item index.
     */
    private static final int INVALID_INDEX = -1;

    /**
     * Lock to guard the model registry.
     */
    private static final Object sRegistryLock = new Object();

    /**
     * This the registry for data models.
     */
    private static final Map<String, ActivityChooserModel> sDataModelRegistry =
        new HashMap<String, ActivityChooserModel>();

    /**
     * Lock for synchronizing on this instance.
     */
    private final Object mInstanceLock = new Object();

    /**
     * List of activities that can handle the current intent.
     */
    private final List<ActivityResolveInfo> mActivites = new ArrayList<ActivityResolveInfo>();

    /**
     * List with historical choice records.
     */
    private final List<HistoricalRecord> mHistoricalRecords = new ArrayList<HistoricalRecord>();

    /**
     * Context for accessing resources.
     */
    private final Context mContext;

    /**
     * The name of the history file that backs this model.
     */
    private final String mHistoryFileName;

    /**
     * The intent for which a activity is being chosen.
     */
    private Intent mIntent;

    /**
     * The sorter for ordering activities based on intent and past choices.
     */
    private ActivitySorter mActivitySorter = new DefaultSorter();

    /**
     * The maximal length of the choice history.
     */
    private int mHistoryMaxSize = DEFAULT_HISTORY_MAX_LENGTH;

    /**
     * Flag whether choice history can be read. In general many clients can
     * share the same data model and {@link #readHistoricalData()} may be called
     * by arbitrary of them any number of times. Therefore, this class guarantees
     * that the very first read succeeds and subsequent reads can be performed
     * only after a call to {@link #persistHistoricalData()} followed by change
     * of the share records.
     */
    private boolean mCanReadHistoricalData = true;

    /**
     * Flag whether the choice history was read. This is used to enforce that
     * before calling {@link #persistHistoricalData()} a call to
     * {@link #persistHistoricalData()} has been made. This aims to avoid a
     * scenario in which a choice history file exits, it is not read yet and
     * it is overwritten. Note that always all historical records are read in
     * full and the file is rewritten. This is necessary since we need to
     * purge old records that are outside of the sliding window of past choices.
     */
    private boolean mReadShareHistoryCalled = false;

    /**
     * Flag whether the choice records have changed. In general many clients can
     * share the same data model and {@link #persistHistoricalData()} may be called
     * by arbitrary of them any number of times. Therefore, this class guarantees
     * that choice history will be persisted only if it has changed.
     */
    private boolean mHistoricalRecordsChanged = true;

    /**
     * Hander for scheduling work on client tread.
     */
    private final Handler mHandler = new Handler();

    /**
     * Policy for controlling how the model handles chosen activities.
     */
    private OnChooseActivityListener mActivityChoserModelPolicy;

    /**
     * Gets the data model backed by the contents of the provided file with historical data.
     * Note that only one data model is backed by a given file, thus multiple calls with
     * the same file name will return the same model instance. If no such instance is present
     * it is created.
     * <p>
     * <strong>Note:</strong> To use the default historical data file clients should explicitly
     * pass as file name {@link #DEFAULT_HISTORY_FILE_NAME}. If no persistence of the choice
     * history is desired clients should pass <code>null</code> for the file name. In such
     * case a new model is returned for each invocation.
     * </p>
     *
     * <p>
     * <strong>Always use difference historical data files for semantically different actions.
     * For example, sharing is different from importing.</strong>
     * </p>
     *
     * @param context Context for loading resources.
     * @param historyFileName File name with choice history, <code>null</code>
     *        if the model should not be backed by a file. In this case the activities
     *        will be ordered only by data from the current session.
     *
     * @return The model.
     */
    public static ActivityChooserModel get(Context context, String historyFileName) {
        synchronized (sRegistryLock) {
            ActivityChooserModel dataModel = sDataModelRegistry.get(historyFileName);
            if (dataModel == null) {
                dataModel = new ActivityChooserModel(context, historyFileName);
                sDataModelRegistry.put(historyFileName, dataModel);
            }
            dataModel.readHistoricalData();
            return dataModel;
        }
    }

    /**
     * Creates a new instance.
     *
     * @param context Context for loading resources.
     * @param historyFileName The history XML file.
     */
    private ActivityChooserModel(Context context, String historyFileName) {
        mContext = context.getApplicationContext();
        if (!TextUtils.isEmpty(historyFileName)
                && !historyFileName.endsWith(HISTORY_FILE_EXTENSION)) {
            mHistoryFileName = historyFileName + HISTORY_FILE_EXTENSION;
        } else {
            mHistoryFileName = historyFileName;
        }
    }

    /**
     * Sets an intent for which to choose a activity.
     * <p>
     * <strong>Note:</strong> Clients must set only semantically similar
     * intents for each data model.
     * <p>
     *
     * @param intent The intent.
     */
    public void setIntent(Intent intent) {
        synchronized (mInstanceLock) {
            if (mIntent == intent) {
                return;
            }
            mIntent = intent;
            loadActivitiesLocked();
        }
    }

    /**
     * Gets the intent for which a activity is being chosen.
     *
     * @return The intent.
     */
    public Intent getIntent() {
        synchronized (mInstanceLock) {
            return mIntent;
        }
    }

    /**
     * Gets the number of activities that can handle the intent.
     *
     * @return The activity count.
     *
     * @see #setIntent(Intent)
     */
    public int getActivityCount() {
        synchronized (mInstanceLock) {
            return mActivites.size();
        }
    }

    /**
     * Gets an activity at a given index.
     *
     * @return The activity.
     *
     * @see ActivityResolveInfo
     * @see #setIntent(Intent)
     */
    public ResolveInfo getActivity(int index) {
        synchronized (mInstanceLock) {
            return mActivites.get(index).resolveInfo;
        }
    }

    /**
     * Gets the index of a the given activity.
     *
     * @param activity The activity index.
     *
     * @return The index if found, -1 otherwise.
     */
    public int getActivityIndex(ResolveInfo activity) {
        List<ActivityResolveInfo> activities = mActivites;
        final int activityCount = activities.size();
        for (int i = 0; i < activityCount; i++) {
            ActivityResolveInfo currentActivity = activities.get(i);
            if (currentActivity.resolveInfo == activity) {
                return i;
            }
        }
        return INVALID_INDEX;
    }

    /**
     * Chooses a activity to handle the current intent. This will result in
     * adding a historical record for that action and construct intent with
     * its component name set such that it can be immediately started by the
     * client.
     * <p>
     * <strong>Note:</strong> By calling this method the client guarantees
     * that the returned intent will be started. This intent is returned to
     * the client solely to let additional customization before the start.
     * </p>
     *
     * @return An {@link Intent} for launching the activity or null if the
     *         policy has consumed the intent.
     *
     * @see HistoricalRecord
     * @see OnChooseActivityListener
     */
    public Intent chooseActivity(int index) {
        ActivityResolveInfo chosenActivity = mActivites.get(index);

        ComponentName chosenName = new ComponentName(
                chosenActivity.resolveInfo.activityInfo.packageName,
                chosenActivity.resolveInfo.activityInfo.name);

        Intent choiceIntent = new Intent(mIntent);
        choiceIntent.setComponent(chosenName);

        if (mActivityChoserModelPolicy != null) {
            // Do not allow the policy to change the intent.
            Intent choiceIntentCopy = new Intent(choiceIntent);
            final boolean handled = mActivityChoserModelPolicy.onChooseActivity(this,
                    choiceIntentCopy);
            if (handled) {
                return null;
            }
        }

        HistoricalRecord historicalRecord = new HistoricalRecord(chosenName,
                System.currentTimeMillis(), DEFAULT_HISTORICAL_RECORD_WEIGHT);
        addHisoricalRecord(historicalRecord);

        return choiceIntent;
    }

    /**
     * Sets the listener for choosing an activity.
     *
     * @param listener The listener.
     */
    public void setOnChooseActivityListener(OnChooseActivityListener listener) {
        mActivityChoserModelPolicy = listener;
    }

    /**
     * Gets the default activity, The default activity is defined as the one
     * with highest rank i.e. the first one in the list of activities that can
     * handle the intent.
     *
     * @return The default activity, <code>null</code> id not activities.
     *
     * @see #getActivity(int)
     */
    public ResolveInfo getDefaultActivity() {
        synchronized (mInstanceLock) {
            if (!mActivites.isEmpty()) {
                return mActivites.get(0).resolveInfo;
            }
        }
        return null;
    }

    /**
     * Sets the default activity. The default activity is set by adding a
     * historical record with weight high enough that this activity will
     * become the highest ranked. Such a strategy guarantees that the default
     * will eventually change if not used. Also the weight of the record for
     * setting a default is inflated with a constant amount to guarantee that
     * it will stay as default for awhile.
     *
     * @param index The index of the activity to set as default.
     */
    public void setDefaultActivity(int index) {
        ActivityResolveInfo newDefaultActivity = mActivites.get(index);
        ActivityResolveInfo oldDefaultActivity = mActivites.get(0);

        final float weight;
        if (oldDefaultActivity != null) {
            // Add a record with weight enough to boost the chosen at the top.
            weight = oldDefaultActivity.weight - newDefaultActivity.weight
                + DEFAULT_ACTIVITY_INFLATION;
        } else {
            weight = DEFAULT_HISTORICAL_RECORD_WEIGHT;
        }

        ComponentName defaultName = new ComponentName(
                newDefaultActivity.resolveInfo.activityInfo.packageName,
                newDefaultActivity.resolveInfo.activityInfo.name);
        HistoricalRecord historicalRecord = new HistoricalRecord(defaultName,
                System.currentTimeMillis(), weight);
        addHisoricalRecord(historicalRecord);
    }

    /**
     * Reads the history data from the backing file if the latter
     * was provided. Calling this method more than once before a call
     * to {@link #persistHistoricalData()} has been made has no effect.
     * <p>
     * <strong>Note:</strong> Historical data is read asynchronously and
     *       as soon as the reading is completed any registered
     *       {@link DataSetObserver}s will be notified. Also no historical
     *       data is read until this method is invoked.
     * <p>
     */
    private void readHistoricalData() {
        synchronized (mInstanceLock) {
            if (!mCanReadHistoricalData || !mHistoricalRecordsChanged) {
                return;
            }
            mCanReadHistoricalData = false;
            mReadShareHistoryCalled = true;
            if (!TextUtils.isEmpty(mHistoryFileName)) {
                /*AsyncTask.*/SERIAL_EXECUTOR.execute(new HistoryLoader());
            }
        }
    }

    private static final SerialExecutor SERIAL_EXECUTOR = new SerialExecutor();

    private static class SerialExecutor implements Executor {
        final LinkedList<Runnable> mTasks = new LinkedList<Runnable>();
        Runnable mActive;

        public synchronized void execute(final Runnable r) {
            mTasks.offer(new Runnable() {
                public void run() {
                    try {
                        r.run();
                    } finally {
                        scheduleNext();
                    }
                }
            });
            if (mActive == null) {
                scheduleNext();
            }
        }

        protected synchronized void scheduleNext() {
            if ((mActive = mTasks.poll()) != null) {
                mActive.run();
            }
        }
    }

    /**
     * Persists the history data to the backing file if the latter
     * was provided. Calling this method before a call to {@link #readHistoricalData()}
     * throws an exception. Calling this method more than one without choosing an
     * activity has not effect.
     *
     * @throws IllegalStateException If this method is called before a call to
     *         {@link #readHistoricalData()}.
     */
    private void persistHistoricalData() {
        synchronized (mInstanceLock) {
            if (!mReadShareHistoryCalled) {
                throw new IllegalStateException("No preceding call to #readHistoricalData");
            }
            if (!mHistoricalRecordsChanged) {
                return;
            }
            mHistoricalRecordsChanged = false;
            mCanReadHistoricalData = true;
            if (!TextUtils.isEmpty(mHistoryFileName)) {
                /*AsyncTask.*/SERIAL_EXECUTOR.execute(new HistoryPersister());
            }
        }
    }

    /**
     * Sets the sorter for ordering activities based on historical data and an intent.
     *
     * @param activitySorter The sorter.
     *
     * @see ActivitySorter
     */
    public void setActivitySorter(ActivitySorter activitySorter) {
        synchronized (mInstanceLock) {
            if (mActivitySorter == activitySorter) {
                return;
            }
            mActivitySorter = activitySorter;
            sortActivities();
        }
    }

    /**
     * Sorts the activities based on history and an intent. If
     * a sorter is not specified this a default implementation is used.
     *
     * @see #setActivitySorter(ActivitySorter)
     */
    private void sortActivities() {
        synchronized (mInstanceLock) {
            if (mActivitySorter != null && !mActivites.isEmpty()) {
                mActivitySorter.sort(mIntent, mActivites,
                        Collections.unmodifiableList(mHistoricalRecords));
                notifyChanged();
            }
        }
    }

    /**
     * Sets the maximal size of the historical data. Defaults to
     * {@link #DEFAULT_HISTORY_MAX_LENGTH}
     * <p>
     *   <strong>Note:</strong> Setting this property will immediately
     *   enforce the specified max history size by dropping enough old
     *   historical records to enforce the desired size. Thus, any
     *   records that exceed the history size will be discarded and
     *   irreversibly lost.
     * </p>
     *
     * @param historyMaxSize The max history size.
     */
    public void setHistoryMaxSize(int historyMaxSize) {
        synchronized (mInstanceLock) {
            if (mHistoryMaxSize == historyMaxSize) {
                return;
            }
            mHistoryMaxSize = historyMaxSize;
            pruneExcessiveHistoricalRecordsLocked();
            sortActivities();
        }
    }

    /**
     * Gets the history max size.
     *
     * @return The history max size.
     */
    public int getHistoryMaxSize() {
        synchronized (mInstanceLock) {
            return mHistoryMaxSize;
        }
    }

    /**
     * Gets the history size.
     *
     * @return The history size.
     */
    public int getHistorySize() {
        synchronized (mInstanceLock) {
            return mHistoricalRecords.size();
        }
    }

    /**
     * Adds a historical record.
     *
     * @param historicalRecord The record to add.
     * @return True if the record was added.
     */
    private boolean addHisoricalRecord(HistoricalRecord historicalRecord) {
        synchronized (mInstanceLock) {
            final boolean added = mHistoricalRecords.add(historicalRecord);
            if (added) {
                mHistoricalRecordsChanged = true;
                pruneExcessiveHistoricalRecordsLocked();
                persistHistoricalData();
                sortActivities();
            }
            return added;
        }
    }

    /**
     * Prunes older excessive records to guarantee {@link #mHistoryMaxSize}.
     */
    private void pruneExcessiveHistoricalRecordsLocked() {
        List<HistoricalRecord> choiceRecords = mHistoricalRecords;
        final int pruneCount = choiceRecords.size() - mHistoryMaxSize;
        if (pruneCount <= 0) {
            return;
        }
        mHistoricalRecordsChanged = true;
        for (int i = 0; i < pruneCount; i++) {
            HistoricalRecord prunedRecord = choiceRecords.remove(0);
            if (DEBUG) {
                Log.i(LOG_TAG, "Pruned: " + prunedRecord);
            }
        }
    }

    /**
     * Loads the activities.
     */
    private void loadActivitiesLocked() {
        mActivites.clear();
        if (mIntent != null) {
            List<ResolveInfo> resolveInfos =
                mContext.getPackageManager().queryIntentActivities(mIntent, 0);
            final int resolveInfoCount = resolveInfos.size();
            for (int i = 0; i < resolveInfoCount; i++) {
                ResolveInfo resolveInfo = resolveInfos.get(i);
                mActivites.add(new ActivityResolveInfo(resolveInfo));
            }
            sortActivities();
        } else {
            notifyChanged();
        }
    }

    /**
     * Represents a record in the history.
     */
    public final static class HistoricalRecord {

        /**
         * The activity name.
         */
        public final ComponentName activity;

        /**
         * The choice time.
         */
        public final long time;

        /**
         * The record weight.
         */
        public final float weight;

        /**
         * Creates a new instance.
         *
         * @param activityName The activity component name flattened to string.
         * @param time The time the activity was chosen.
         * @param weight The weight of the record.
         */
        public HistoricalRecord(String activityName, long time, float weight) {
            this(ComponentName.unflattenFromString(activityName), time, weight);
        }

        /**
         * Creates a new instance.
         *
         * @param activityName The activity name.
         * @param time The time the activity was chosen.
         * @param weight The weight of the record.
         */
        public HistoricalRecord(ComponentName activityName, long time, float weight) {
            this.activity = activityName;
            this.time = time;
            this.weight = weight;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((activity == null) ? 0 : activity.hashCode());
            result = prime * result + (int) (time ^ (time >>> 32));
            result = prime * result + Float.floatToIntBits(weight);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            HistoricalRecord other = (HistoricalRecord) obj;
            if (activity == null) {
                if (other.activity != null) {
                    return false;
                }
            } else if (!activity.equals(other.activity)) {
                return false;
            }
            if (time != other.time) {
                return false;
            }
            if (Float.floatToIntBits(weight) != Float.floatToIntBits(other.weight)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("[");
            builder.append("; activity:").append(activity);
            builder.append("; time:").append(time);
            builder.append("; weight:").append(new BigDecimal(weight));
            builder.append("]");
            return builder.toString();
        }
    }

    /**
     * Represents an activity.
     */
    public final class ActivityResolveInfo implements Comparable<ActivityResolveInfo> {

        /**
         * The {@link ResolveInfo} of the activity.
         */
        public final ResolveInfo resolveInfo;

        /**
         * Weight of the activity. Useful for sorting.
         */
        public float weight;

        /**
         * Creates a new instance.
         *
         * @param resolveInfo activity {@link ResolveInfo}.
         */
        public ActivityResolveInfo(ResolveInfo resolveInfo) {
            this.resolveInfo = resolveInfo;
        }

        @Override
        public int hashCode() {
            return 31 + Float.floatToIntBits(weight);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            ActivityResolveInfo other = (ActivityResolveInfo) obj;
            if (Float.floatToIntBits(weight) != Float.floatToIntBits(other.weight)) {
                return false;
            }
            return true;
        }

        public int compareTo(ActivityResolveInfo another) {
             return  Float.floatToIntBits(another.weight) - Float.floatToIntBits(weight);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("[");
            builder.append("resolveInfo:").append(resolveInfo.toString());
            builder.append("; weight:").append(new BigDecimal(weight));
            builder.append("]");
            return builder.toString();
        }
    }

    /**
     * Default activity sorter implementation.
     */
    private final class DefaultSorter implements ActivitySorter {
        private static final float WEIGHT_DECAY_COEFFICIENT = 0.95f;

        private final Map<String, ActivityResolveInfo> mPackageNameToActivityMap =
            new HashMap<String, ActivityResolveInfo>();

        public void sort(Intent intent, List<ActivityResolveInfo> activities,
                List<HistoricalRecord> historicalRecords) {
            Map<String, ActivityResolveInfo> packageNameToActivityMap =
                mPackageNameToActivityMap;
            packageNameToActivityMap.clear();

            final int activityCount = activities.size();
            for (int i = 0; i < activityCount; i++) {
                ActivityResolveInfo activity = activities.get(i);
                activity.weight = 0.0f;
                String packageName = activity.resolveInfo.activityInfo.packageName;
                packageNameToActivityMap.put(packageName, activity);
            }

            final int lastShareIndex = historicalRecords.size() - 1;
            float nextRecordWeight = 1;
            for (int i = lastShareIndex; i >= 0; i--) {
                HistoricalRecord historicalRecord = historicalRecords.get(i);
                String packageName = historicalRecord.activity.getPackageName();
                ActivityResolveInfo activity = packageNameToActivityMap.get(packageName);
                if (activity != null) {
                    activity.weight += historicalRecord.weight * nextRecordWeight;
                    nextRecordWeight = nextRecordWeight * WEIGHT_DECAY_COEFFICIENT;
                }
            }

            Collections.sort(activities);

            if (DEBUG) {
                for (int i = 0; i < activityCount; i++) {
                    Log.i(LOG_TAG, "Sorted: " + activities.get(i));
                }
            }
        }
    }

    /**
     * Command for reading the historical records from a file off the UI thread.
     */
    private final class HistoryLoader implements Runnable {

       public void run() {
            FileInputStream fis = null;
            try {
                fis = mContext.openFileInput(mHistoryFileName);
            } catch (FileNotFoundException fnfe) {
                if (DEBUG) {
                    Log.i(LOG_TAG, "Could not open historical records file: " + mHistoryFileName);
                }
                return;
            }
            try {
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(fis, null);

                int type = XmlPullParser.START_DOCUMENT;
                while (type != XmlPullParser.END_DOCUMENT && type != XmlPullParser.START_TAG) {
                    type = parser.next();
                }

                if (!TAG_HISTORICAL_RECORDS.equals(parser.getName())) {
                    throw new XmlPullParserException("Share records file does not start with "
                            + TAG_HISTORICAL_RECORDS + " tag.");
                }

                List<HistoricalRecord> readRecords = new ArrayList<HistoricalRecord>();

                while (true) {
                    type = parser.next();
                    if (type == XmlPullParser.END_DOCUMENT) {
                        break;
                    }
                    if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                        continue;
                    }
                    String nodeName = parser.getName();
                    if (!TAG_HISTORICAL_RECORD.equals(nodeName)) {
                        throw new XmlPullParserException("Share records file not well-formed.");
                    }

                    String activity = parser.getAttributeValue(null, ATTRIBUTE_ACTIVITY);
                    final long time =
                        Long.parseLong(parser.getAttributeValue(null, ATTRIBUTE_TIME));
                    final float weight =
                        Float.parseFloat(parser.getAttributeValue(null, ATTRIBUTE_WEIGHT));

                    HistoricalRecord readRecord = new HistoricalRecord(activity, time,
                            weight);
                    readRecords.add(readRecord);

                    if (DEBUG) {
                        Log.i(LOG_TAG, "Read " + readRecord.toString());
                    }
                }

                if (DEBUG) {
                    Log.i(LOG_TAG, "Read " + readRecords.size() + " historical records.");
                }

                synchronized (mInstanceLock) {
                    Set<HistoricalRecord> uniqueShareRecords =
                        new LinkedHashSet<HistoricalRecord>(readRecords);

                    // Make sure no duplicates. Example: Read a file with
                    // one record, add one record, persist the two records,
                    // add a record, read the persisted records - the
                    // read two records should not be added again.
                    List<HistoricalRecord> historicalRecords = mHistoricalRecords;
                    final int historicalRecordsCount = historicalRecords.size();
                    for (int i = historicalRecordsCount - 1; i >= 0; i--) {
                        HistoricalRecord historicalRecord = historicalRecords.get(i);
                        uniqueShareRecords.add(historicalRecord);
                    }

                    if (historicalRecords.size() == uniqueShareRecords.size()) {
                        return;
                    }

                    // Make sure the oldest records go to the end.
                    historicalRecords.clear();
                    historicalRecords.addAll(uniqueShareRecords);

                    mHistoricalRecordsChanged = true;

                    // Do this on the client thread since the client may be on the UI
                    // thread, wait for data changes which happen during sorting, and
                    // perform UI modification based on the data change.
                    mHandler.post(new Runnable() {
                        public void run() {
                            pruneExcessiveHistoricalRecordsLocked();
                            sortActivities();
                        }
                    });
                }
            } catch (XmlPullParserException xppe) {
                Log.e(LOG_TAG, "Error reading historical recrod file: " + mHistoryFileName, xppe);
            } catch (IOException ioe) {
                Log.e(LOG_TAG, "Error reading historical recrod file: " + mHistoryFileName, ioe);
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException ioe) {
                        /* ignore */
                    }
                }
            }
        }
    }

    /**
     * Command for persisting the historical records to a file off the UI thread.
     */
    private final class HistoryPersister implements Runnable {

        public void run() {
            FileOutputStream fos = null;
            List<HistoricalRecord> records = null;

            synchronized (mInstanceLock) {
                records = new ArrayList<HistoricalRecord>(mHistoricalRecords);
            }

            try {
                fos = mContext.openFileOutput(mHistoryFileName, Context.MODE_PRIVATE);
            } catch (FileNotFoundException fnfe) {
                Log.e(LOG_TAG, "Error writing historical recrod file: " + mHistoryFileName, fnfe);
                return;
            }

            XmlSerializer serializer = Xml.newSerializer();

            try {
                serializer.setOutput(fos, null);
                serializer.startDocument("UTF-8", true);
                serializer.startTag(null, TAG_HISTORICAL_RECORDS);

                final int recordCount = records.size();
                for (int i = 0; i < recordCount; i++) {
                    HistoricalRecord record = records.remove(0);
                    serializer.startTag(null, TAG_HISTORICAL_RECORD);
                    serializer.attribute(null, ATTRIBUTE_ACTIVITY, record.activity.flattenToString());
                    serializer.attribute(null, ATTRIBUTE_TIME, String.valueOf(record.time));
                    serializer.attribute(null, ATTRIBUTE_WEIGHT, String.valueOf(record.weight));
                    serializer.endTag(null, TAG_HISTORICAL_RECORD);
                    if (DEBUG) {
                        Log.i(LOG_TAG, "Wrote " + record.toString());
                    }
                }

                serializer.endTag(null, TAG_HISTORICAL_RECORDS);
                serializer.endDocument();

                if (DEBUG) {
                    Log.i(LOG_TAG, "Wrote " + recordCount + " historical records.");
                }
            } catch (IllegalArgumentException iae) {
                Log.e(LOG_TAG, "Error writing historical recrod file: " + mHistoryFileName, iae);
            } catch (IllegalStateException ise) {
                Log.e(LOG_TAG, "Error writing historical recrod file: " + mHistoryFileName, ise);
            } catch (IOException ioe) {
                Log.e(LOG_TAG, "Error writing historical recrod file: " + mHistoryFileName, ioe);
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        /* ignore */
                    }
                }
            }
        }
    }
}
