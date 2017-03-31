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
package com.google.samples.apps.iosched.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.os.CancellationSignal;
import android.support.v4.os.OperationCanceledException;


public class CursorModelLoader<D> extends AsyncTaskLoader<D> {

    private D mData;
    private CursorTransform<D> mCursorTransform;

    private ForceLoadContentObserver mObserver;
    private CancellationSignal mCancellationSignal;

    public interface CursorTransform<D> {

        /**
         * Called when a query needs to be performed. This is called from a background thread.
         * Typically you would implement this as
         * <pre>
         * return ContentResolverCompat.query(loader.getContext().getContentResolver(),
         *         uri, projection, selection, selectionArgs, sortOrder,
         *         cancellationSignal);
         * </pre>
         *
         * @param loader The loader performing background work
         * @param cancellationSignal A CancellationSignal to pass to ContentResolver.query()
         * @return The query result
         */
        Cursor performQuery(@NonNull CursorModelLoader<D> loader,
                @NonNull CancellationSignal cancellationSignal);

        /**
         * Called when the query result needs to be transformed into the specified data type.
         * This is called from a background thread.
         *
         * @param loader The loader performing background work
         * @param cursor The cursor that was loaded. Implementors do not need to close this cursor.
         * @return The transformed data
         */
        D cursorToModel(@NonNull CursorModelLoader<D> loader, @NonNull Cursor cursor);

        /**
         * Called when the loader wants a Uri for the purpose of registering a ContentObserver.
         *
         * @param loader The loader requesting a content Uri
         * @return A Uri to observe for changes. If not null, a ContentObserver is registered so
         * that changes automatically cause a reload of the data
         */
        Uri getObserverUri(@NonNull CursorModelLoader<D> loader);
    }

    public CursorModelLoader(Context context, CursorTransform<D> cursorTransform) {
        super(context);
        mCursorTransform = cursorTransform;
    }

    @Override
    public D loadInBackground() {
        synchronized (this) {
            if (isLoadInBackgroundCanceled()) {
                throw new OperationCanceledException();
            }
            mCancellationSignal = new CancellationSignal();
        }
        Cursor cursor = null;
        try {
            cursor = mCursorTransform.performQuery(this, mCancellationSignal);
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
                return mCursorTransform.cursorToModel(this, cursor);
            } finally {
                cursor.close();
            }
        }
        return null;
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
    public void deliverResult(D data) {
        if (isReset()) {
            // An async query came in while the loader is stopped
            return;
        }

        mData = data;
        if (isStarted()) {
            super.deliverResult(mData);
        }
    }

    @Override
    protected void onStartLoading() {
        if (mData != null) {
            deliverResult(mData);
        }

        if (mObserver == null) {
            Uri uri = mCursorTransform.getObserverUri(this);
            if (uri != null) {
                mObserver = new ForceLoadContentObserver();
                getContext().getContentResolver().registerContentObserver(uri, true, mObserver);
            }
        }

        if (takeContentChanged() || mData == null) {
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

        mData = null;
    }
}
