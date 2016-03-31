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

package com.google.samples.apps.iosched.testutils;

import android.database.Cursor;
import android.os.Handler;
import android.support.annotation.NonNull;

import com.google.samples.apps.iosched.archframework.Model;
import com.google.samples.apps.iosched.archframework.ModelWithLoaderManager;
import com.google.samples.apps.iosched.archframework.QueryEnum;

import java.util.HashMap;

/**
 * Provides method to override loader manager mechanism.
 */
public class StubModelHelper<Q extends QueryEnum> {

    /**
     * Overrides the loader manager mechanism of the {@code model}, if the {@code query} is found as
     * a key of the {@code fakeData}.
     *
     * @param query     The query to load data for
     * @param callback  The callback associated with the request
     * @param fakeData  A map of {@link Cursor}s associated with a {@link QueryEnum}
     * @param callbacks All the query callbacks of the {@code model}
     * @param model     The model instance in which to  override the loader manager
     */
    public void overrideLoaderManager(@NonNull final Q query,
            @NonNull final Model.DataQueryCallback callback,
            final HashMap<QueryEnum, Cursor> fakeData,
            HashMap<Q, Model.DataQueryCallback> callbacks,
            final ModelWithLoaderManager model) {

        // Add the callback so it gets fired properly
        callbacks.put(query, callback);

        Handler h = new Handler();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                // Call onLoadFinished with stub cursor and query
                if (fakeData.containsKey(query)) {
                    model.onLoadFinished(query, fakeData.get(query));
                }
            }
        };

        // Delayed to ensure the UI is ready, because it will fire the callback to update the view
        // very quickly
        h.postDelayed(r, 5);
    }
}
