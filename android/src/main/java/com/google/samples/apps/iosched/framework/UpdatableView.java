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
package com.google.samples.apps.iosched.framework;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;

/**
 * An UpdatableView is a UI class, often a {@link android.app.Fragment}, that provides a
 * {@link Presenter} an interface through which to control it  (MVP architectural pattern).
 * It is parametrised by the {@link Model} class.
 * <p />
 * The {@link Presenter} registers itself as a {@link UserActionListener} with
 * {@link #addListener(UserActionListener)}, so that it can trigger an update on the {@link Model}
 * when the user performs an action on the UpdatableView. After the data is updated, the
 * {@link Presenter} updates the UpdatableView by calling {@link #displayData(Object, QueryEnum)}
 * or {@link #displayErrorMessage(QueryEnum)} so the UpdatableView can redraw itself with the
 * updated data.
 * <p />
 * The UpdatableView belongs to an {@link android.app.Activity} that typically has been started
 * with an {@link android.content.Intent} specifying at least one Data URI, used for loading the
 * initial data into the {@link Model}.
 */
public interface UpdatableView<M> {

    /**
     * Updates the view based on data in the model.
     *
     * @param model The updated model.
     * @param query The query that has triggered the model update. This is so not the full view has
     *              to be updated but only specific elements of the view, depending on the query.
     */
    public void displayData(M model, QueryEnum query);

    /**
     * Displays error message resulting from a query not succeeding.
     *
     * @param query The query that resulted in an error.
     */
    public void displayErrorMessage(QueryEnum query);

    /**
     * Data URI representing the data displayed in this view. Complex views may use data from
     * different queries / Data URI.
     *
     * @param query The query for which the URI should be returned.
     */
    public Uri getDataUri(QueryEnum query);

    public Context getContext();

    public void addListener(UserActionListener listener);

    /**
     * A listener for events fired off by a {@link Model}
     */
    interface UserActionListener {

        /**
         * Called when the user has performed an {@code action}, with data to be passed
         * as a {@link android.os.Bundle} in {@code args}.
         * <p />
         * Add the constants used to store values in the bundle to the Model implementation class
         * as final static protected strings.
         */
        public void onUserAction(UserActionEnum action, @Nullable Bundle args);
    }
}