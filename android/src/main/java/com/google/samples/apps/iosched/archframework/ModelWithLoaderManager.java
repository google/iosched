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

package com.google.samples.apps.iosched.archframework;

import android.app.LoaderManager;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;
import com.google.samples.apps.iosched.session.SessionDetailModel;

import java.util.HashMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.samples.apps.iosched.util.LogUtils.LOGE;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Implementation class for {@link Model}, using the {@link LoaderManager} callbacks to query the
 * data from the {@link com.google.samples.apps.iosched.provider.ScheduleProvider}.
 */
public abstract class ModelWithLoaderManager<Q extends QueryEnum, UA extends UserActionEnum>
        implements Model<Q, UA>, LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * Key to be used in Bundle passed in {@link #onUserAction(UserActionEnum, Bundle)} for a user
     * action that requires running {@link QueryEnum}, specifying its id. The value stored must be
     * an Integer.
     */
    public static final String KEY_RUN_QUERY_ID = "KEY_RUN_QUERY_ID";

    private static final String TAG = makeLogTag(ModelWithLoaderManager.class);

    private LoaderManager mLoaderManager;

    private Q[] mQueries;

    private UA[] mUserActions;

    /**
     * Map of callbacks, using the query as key. This is required because we can't pass on the
     * {@link com.google.samples.apps.iosched.archframework.Model.DataQueryCallback} to the {@link
     * LoaderManager} callbacks.
     * <p/>
     * This is @VisibleForTesting because for integration testing, a fake model is used to allow
     * bypassing the {@link LoaderManager} and pass a mock {@link Cursor} directly to {@link
     * #onLoadFinished(QueryEnum, Cursor)} and adding the callback so events can be fired normally
     * on the callback after the data is read from the cursor.
     */
    @VisibleForTesting
    protected HashMap<Q, DataQueryCallback> mDataQueryCallbacks =
            new HashMap<Q, DataQueryCallback>();

    /**
     * Map of callbacks, using the id of the user action as key. This is required because some user
     * actions launch a data query and we can't pass on the {@link com.google.samples.apps
     * .iosched.archframework.Model.UserActionCallback} to the {@link LoaderManager} callbacks.
     * <p/>
     * When the user action leads to a new query being run, the {@link LoaderManager} callbacks
     * provide us with an Integer id. Therefore, we link an Integer id to a callback, and use a
     * separate map to link the Integer id to a user action {}see {@link
     * #mUserActionsLaunchingQueries}.
     * <p/>
     * This is @VisibleForTesting because for integration testing, a fake model is used to allow
     * bypassing the {@link LoaderManager} and pass a mock {@link Cursor} directly to {@link
     * #onLoadFinished(QueryEnum, Cursor)} and adding the callback so events can be fired normally
     * on the callback after the data is read from the cursor.
     */
    @VisibleForTesting
    protected HashMap<Integer, UserActionCallback> mDataUpdateCallbacks =
            new HashMap<Integer, UserActionCallback>();

    /**
     * Map of user actions that have launched queries, using their id as key. This is used in
     * conjunction with {@link #mDataUpdateCallbacks}, so once the {@link
     * android.app.LoaderManager.LoaderCallbacks#onLoadFinished(Loader, Object)} has fired, the
     * {@link UserActionCallback} that launched that query can be fired.
     * <p/>
     * This is @VisibleForTesting because for integration testing, a fake model is used to allow
     * bypassing the {@link LoaderManager} and pass a mock {@link Cursor} directly to {@link
     * #onLoadFinished(QueryEnum, Cursor)} and adding the callback so events can be fired normally
     * on the callback after the data is read from the cursor.
     */
    @VisibleForTesting
    protected HashMap<Integer, UA> mUserActionsLaunchingQueries = new HashMap<Integer, UA>();

    public ModelWithLoaderManager(Q[] queries, UA[] userActions, LoaderManager loaderManager) {
        mQueries = queries;
        mUserActions = userActions;
        mLoaderManager = loaderManager;
    }

    @Override
    public Q[] getQueries() {
        return mQueries;
    }

    @Override
    public UA[] getUserActions() {
        return mUserActions;
    }

    /**
     * Called when the user has performed an {@code action}, with data in {@code args}.
     * <p/>
     * Add the constants used to store values in the bundle to the Model implementation class as
     * final static protected strings.
     * <p/>
     * If the {@code action} should trigger a new data query, specify the query ID by storing the
     * associated Integer in the {@code args} using {@link #KEY_RUN_QUERY_ID}. The {@code args} will
     * be passed on to the cursor loader so you can pass in extra arguments for your query.
     */
    @Override
    public void deliverUserAction(@NonNull UA action, @Nullable Bundle args,
            @NonNull UserActionCallback callback) {
        checkNotNull(callback);
        checkNotNull(action);
        if (args != null && args.containsKey(KEY_RUN_QUERY_ID)) {
            Object queryId = args.get(KEY_RUN_QUERY_ID);
            if (queryId instanceof Integer) {
                if (isQueryValid((Integer) queryId) && mLoaderManager != null) {
                    mLoaderManager.restartLoader((Integer) queryId, args, this);
                    mDataUpdateCallbacks.put((Integer) queryId, callback);
                    mUserActionsLaunchingQueries.put((Integer) queryId, action);
                } else if (isQueryValid((Integer) queryId) && mLoaderManager == null) {
                    // The loader manager hasn't been initialised because initial queries haven't
                    // been run yet. This happens when a user action is triggered by a change in
                    // shared preferences before the initial queries are loaded. Unlikely to happen
                    // often, but it is a possible race condition and it was triggered in UI
                    // tests. Nothing to do in that case because presenter will run all queries
                    // when it will go through loadInitialQueries.
                } else {
                    callback.onError(action);
                    // Query id should be valid!
                    LOGE(TAG, "onUserAction called with a bundle containing KEY_RUN_QUERY_ID but"
                            + "the value is not a valid query id!");
                }
            } else {
                callback.onError(action);
                // Query id should be an integer!
                LOGE(TAG, "onUserAction called with a bundle containing KEY_RUN_QUERY_ID but"
                        + "the value is not an Integer so it's not a valid query id!");
            }
        } else {
            processUserAction(action, args, callback);
        }
    }

    /**
     * This should be implemented by the feature. Typically, there will be a switch on the {@code
     * action}, a method will be called to update the data, then the callback will be fired.
     *
     * @see SessionDetailModel#processUserAction(UA, @Nullable Bundle, UserActionCallback )
     */
    public abstract void processUserAction(UA action, @Nullable Bundle args,
            UserActionCallback callback);

    @Override
    public void requestData(@NonNull Q query, @NonNull DataQueryCallback callback) {
        checkNotNull(query);
        checkNotNull(callback);
        if (isQueryValid(query)) {
            mLoaderManager.initLoader(query.getId(), null, this);
            mDataQueryCallbacks.put(query, callback);
        } else {
            LOGE(TAG, "Invalid query " + query);
            callback.onError(query);
        }
    }

    private boolean isQueryValid(@NonNull Q query) {
        checkNotNull(query);
        return isQueryValid(query.getId());
    }

    private boolean isQueryValid(int queryId) {
        Q match = (Q) QueryEnumHelper.getQueryForId(queryId, getQueries());
        return match != null;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return createCursorLoader((Q) QueryEnumHelper.getQueryForId(id, mQueries), args);
    }

    /**
     * This should be implemented by the feature. In addition to the {@link
     * QueryEnum#getProjection()}, other information such as sorting order will be needed.
     */
    public abstract Loader<Cursor> createCursorLoader(Q query, Bundle args);

    /**
     * This should be implemented by the feature. It reads the data from the {@code cursor} for the
     * given {@code query}. Typically, there will be a switch on the {@code query}, then a private
     * method will be called to read the data from the cursor.
     */
    public abstract boolean readDataFromCursor(Cursor cursor, Q query);

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Q query = (Q) QueryEnumHelper.getQueryForId(loader.getId(), mQueries);
        onLoadFinished(query, data);
    }

    /**
     * This method is called directly from integration tests to allow us to pass in a mock cursor,
     * so we can stub out different data and thus test the UI fully.
     */
    @VisibleForTesting
    public void onLoadFinished(Q query, Cursor data) {
        boolean success = readDataFromCursor(data, query);
        if (mDataUpdateCallbacks.containsKey(query.getId())
                && mUserActionsLaunchingQueries.containsKey(query.getId())) {
            UserActionCallback callback = mDataUpdateCallbacks.get(query.getId());
            UA userAction = mUserActionsLaunchingQueries.get(query.getId());
            if (success) {
                callback.onModelUpdated(this, userAction);
            } else {
                callback.onError(userAction);
            }
        } else if (mDataQueryCallbacks.containsKey(query) &&
                mDataQueryCallbacks.get(query) != null) {
            DataQueryCallback callback = mDataQueryCallbacks.get(query);
            if (success) {
                callback.onModelUpdated(this, query);
            } else {
                callback.onError(query);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Not used
    }
}
