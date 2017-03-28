/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.samples.apps.iosched.myio;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.ContentResolverCompat;
import android.support.v4.os.CancellationSignal;
import android.support.v4.os.OperationCanceledException;

import com.google.samples.apps.iosched.model.ScheduleItem;
import com.google.samples.apps.iosched.model.ScheduleItemHelper;
import com.google.samples.apps.iosched.provider.ScheduleContract.Sessions;

import java.util.List;


/**
 * Loader which loads My I/O schedule items.
 * TODO: generalize this so we can make Loaders for other things backed by ContentProvider queries.
 */
public class MyIOScheduleLoader extends AsyncTaskLoader<List<ScheduleItem>> {

    public MyIOScheduleLoader(Context context) {
        super(context);
    }

    private List<ScheduleItem> mItems;
    private ForceLoadContentObserver mObserver;
    private CancellationSignal mCancellationSignal;

    @Override
    public List<ScheduleItem> loadInBackground() {
        synchronized (this) {
            if (isLoadInBackgroundCanceled()) {
                throw new OperationCanceledException();
            }
            mCancellationSignal = new CancellationSignal();
        }
        Cursor cursor = null;
        try {
            cursor = ContentResolverCompat.query(getContext().getContentResolver(),
                    Sessions.CONTENT_MY_SCHEDULE_URI, ScheduleItemHelper.REQUIRED_SESSION_COLUMNS,
                    null, null, Sessions.SORT_BY_TIME, mCancellationSignal);
            if (cursor != null) {
                try {
                    // Ensure the cursor window is filled.
                    cursor.getCount();
                    cursor.registerContentObserver(mObserver);
                } catch (RuntimeException ex) {
                    cursor.close();
                    throw ex;
                }
            }
        } finally {
            synchronized (this) {
                mCancellationSignal = null;
            }
        }

        if (cursor != null) {
            try {
                return cursorToModel(cursor);
            } finally {
                cursor.close();
            }
        }
        return null;
    }

    private List<ScheduleItem> cursorToModel(Cursor cursor) {
        return ScheduleItemHelper.cursorToItems(cursor, getContext());
    }

    @Override
    public void cancelLoadInBackground() {
        super.cancelLoadInBackground();

        synchronized (this) {
            if (mCancellationSignal != null) {
                mCancellationSignal.cancel();
            }
        }
    }

    /* Runs on the UI thread */
    @Override
    public void deliverResult(List<ScheduleItem> data) {
        if (isReset()) {
            // An async query came in while the loader is stopped
            return;
        }

        mItems = data;
        if (isStarted()) {
            super.deliverResult(mItems);
        }
    }

    /**
     * Starts an asynchronous load of the contacts list data. When the result is ready the callbacks
     * will be called on the UI thread. If a previous load has been completed and is still valid
     * the result may be passed to the callbacks immediately.
     *
     * Must be called from the UI thread
     */
    @Override
    protected void onStartLoading() {
        if (mItems != null) {
            deliverResult(mItems);
        }

        if (mObserver == null) {
            mObserver = new ForceLoadContentObserver();
            getContext().getContentResolver().registerContentObserver(
                    Sessions.CONTENT_MY_SCHEDULE_URI, true, mObserver);
        }

        if (takeContentChanged() || mItems == null) {
            forceLoad();
        }
    }

    /**
     * Must be called from the UI thread
     */
    @Override
    protected void onStopLoading() {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    @Override
    protected void onReset() {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();
        if (mObserver != null) {
            getContext().getContentResolver().unregisterContentObserver(mObserver);
            mObserver = null;
        }

        mItems = null;
    }
}
