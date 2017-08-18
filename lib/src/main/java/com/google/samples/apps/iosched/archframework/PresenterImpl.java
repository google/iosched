/*
 * Copyright 2017 Google Inc. All rights reserved.
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

package com.google.samples.apps.iosched.archframework;

import android.os.Bundle;
import android.support.annotation.Nullable;

import com.google.samples.apps.iosched.archframework.Model.DataQueryCallback;
import com.google.samples.apps.iosched.archframework.Model.UserActionCallback;

import static com.google.samples.apps.iosched.util.LogUtils.LOGE;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * This implements the {@link Presenter} interface. This Presenter can interact with more than one
 * {@link UpdatableView}, based on the constructor used to create it. The most common use case for
 * have multiple {@link UpdatableView} controlled by the same presenter is an Activity with tabs,
 * where each view in the tab (typically a {@link android.support.v4.app.Fragment}) is an {@link
 * UpdatableView}.
 * <p/>
 * It requests the model to load its initial data, it listens to events from the {@link
 * UpdatableView}(s) and passes the user actions on to the Model, then it updates the {@link
 * UpdatableView}(s) once the Model has completed its data update.
 */
public class PresenterImpl<M extends Model<Q, UA>, Q extends QueryEnum, UA extends UserActionEnum>
        implements Presenter, UpdatableView.UserActionListener<UA> {

    private static final String TAG = makeLogTag(PresenterImpl.class);

    /**
     * The UI that this Presenter controls.
     */
    @Nullable
    private UpdatableView<M, Q, UA>[] mUpdatableViews;

    /**
     * The Model that this Presenter controls.
     */
    private M mModel;

    /**
     * The queries to load when the {@link android.app.Activity} loading this {@link
     * android.support.v4.app.Fragment} is created.
     */
    private Q[] mInitialQueriesToLoad;

    /**
     * The actions allowed by the presenter.
     */
    private UA[] mValidUserActions;

    /**
     * Use this constructor if this Presenter controls one view only.
     */
    public PresenterImpl(M model, UpdatableView<M, Q, UA> view, UA[] validUserActions,
            Q[] initialQueries) {
        this(model, new UpdatableView[]{view}, validUserActions, initialQueries);
    }

    /**
     * Use this constructor if this Presenter controls more than one view.
     */
    public PresenterImpl(M model, @Nullable UpdatableView<M, Q, UA>[] views, UA[] validUserActions,
            Q[] initialQueries) {
        mModel = model;
        if (views != null) {
            mUpdatableViews = views;
            for (UpdatableView<M, Q, UA> view : mUpdatableViews) {
                view.addListener(this);
            }
        } else {
            LOGE(TAG, "Creating a PresenterImpl with null View");
        }
        mValidUserActions = validUserActions;
        mInitialQueriesToLoad = initialQueries;
    }

    @Override
    public void loadInitialQueries() {
        // Load data queries if any.
        if (mInitialQueriesToLoad != null && mInitialQueriesToLoad.length > 0) {
            for (Q query: mInitialQueriesToLoad) {
                mModel.requestData(query, new DataQueryCallback<Q>() {
                    @Override
                    public void onModelUpdated(Model<Q, ?> model, Q query) {
                        if (mUpdatableViews != null) {
                            for (UpdatableView<M, Q, UA> view : mUpdatableViews) {
                                view.displayData(mModel, query);
                            }
                        } else {
                            LOGE(TAG, "loadInitialQueries(), cannot notify a null view!");
                        }
                    }

                    @Override
                    public void onError(Q query) {
                        if (mUpdatableViews != null) {
                            for (UpdatableView<M, Q, UA> view : mUpdatableViews) {
                                view.displayErrorMessage(query);
                            }
                        } else {
                            LOGE(TAG, "loadInitialQueries(), cannot notify a null view!");
                        }
                    }

                });
            }
        } else {
            // No data query to load, update the view.
            if (mUpdatableViews != null) {
                for (UpdatableView<M, Q, UA> view : mUpdatableViews) {
                    view.displayData(mModel, null);
                }
            } else {
                LOGE(TAG, "loadInitialQueries(), cannot notify a null view!");
            }
        }
    }

    /**
     * Called when the user has performed an {@code action}, with data to be passed as a {@link
     * android.os.Bundle} in {@code args}.
     * <p/>
     * Add the constants used to store values in the bundle to the Model implementation class as
     * final static protected strings.
     * <p/>
     * If the {@code action} should trigger a new data query, specify the query ID by storing the
     * associated Integer in the {@code args} using {@link ModelWithLoaderManager#KEY_RUN_QUERY_ID}.
     * The {@code args} will be passed on to the cursor loader so you can pass in extra arguments
     * for your query.
     */
    @Override
    public void onUserAction(UA action, @Nullable Bundle args) {
        boolean isValid = false;
        if (mValidUserActions != null && action != null) {
            for (UA validAction : mValidUserActions) {
                if (validAction.getId() == action.getId()) {
                    isValid = true;
                    break;
                }
            }
        }
        if (isValid) {
            mModel.deliverUserAction(action, args, new UserActionCallback<UA>() {

                @Override
                public void onModelUpdated(Model<?, UA> model, UA userAction) {
                    if (mUpdatableViews != null) {
                        for (UpdatableView<M, Q, UA> view : mUpdatableViews) {
                            view.displayUserActionResult(mModel, userAction, true);
                        }
                    } else {
                        LOGE(TAG, "onUserAction(), cannot notify a null view!");
                    }
                }

                @Override
                public void onError(UA userAction) {
                    if (mUpdatableViews != null) {
                        for (UpdatableView<M, Q, UA> view : mUpdatableViews) {
                            view.displayUserActionResult(null, userAction, false);
                        }
                    } else {
                        LOGE(TAG, "onUserAction(), cannot notify a null view!");
                    }
                }
            });
        } else {
            if (mUpdatableViews != null) {
                for (UpdatableView<M, Q, UA> view : mUpdatableViews) {
                    view.displayUserActionResult(null, action, false);
                }
                // User action not understood.
                throw new RuntimeException(
                        "Invalid user action " + (action != null ? action.getId() : null)
                                + ". Have you called setValidUserActions on your presenter, with "
                                + "all the UserActionEnum you want to support?");
            } else {
                LOGE(TAG, "onUserAction(), cannot notify a null view!");
            }
        }
    }

    protected M getModel() {
        return mModel;
    }
}
