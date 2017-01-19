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

import android.os.Bundle;
import android.test.suitebuilder.annotation.SmallTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static com.google.samples.apps.iosched.archframework.ArchFrameworkHelperForTest
        .createQueryEnumWithId;
import static com.google.samples.apps.iosched.archframework.ArchFrameworkHelperForTest
        .createUserActionEnumWithId;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class PresenterImplTest {

    @Mock
    private Model mMockModel;

    @Mock
    private UpdatableView mMockUpdatableView1;

    @Mock
    private UpdatableView mMockUpdatableView2;

    private UpdatableView[] mMockUpdatableViews;

    @Captor
    private ArgumentCaptor<Model.DataQueryCallback> mDataQueryCallbackCaptor;

    @Captor
    private ArgumentCaptor<Model.UserActionCallback> mUserActionCallbackCaptor;

    @Rule
    public ExpectedException mThrown = ExpectedException.none();

    private UserActionEnum[] mUserActions;

    private QueryEnum[] mQueries;

    private PresenterImpl mPresenterImpl;

    @Before
    public void setUp() {
        // A presenter with one query with id 1 and one user action with id 1
        mQueries = new QueryEnum[1];
        mQueries[0] = createQueryEnumWithId(1);
        mUserActions = new UserActionEnum[1];
        mUserActions[0] = createUserActionEnumWithId(1);

        // A presenter with 2 views
        mMockUpdatableViews = new UpdatableView[]{mMockUpdatableView1, mMockUpdatableView2};

        mPresenterImpl = new PresenterImpl(mMockModel, mMockUpdatableViews, mUserActions,
                mQueries);
    }

    @Test
    public void loadInitialQueries_emptyArray_displayDataImmediately() {
        // Given an empty query array
        mQueries = new QueryEnum[0];
        mPresenterImpl = new PresenterImpl(mMockModel, mMockUpdatableViews, mUserActions,
                mQueries);

        // When loading initial queries
        mPresenterImpl.loadInitialQueries();

        // Then the views are updated immediately
        verify(mMockUpdatableView1).displayData(mMockModel, null);
        verify(mMockUpdatableView2).displayData(mMockModel, null);
    }

    @Test
    public void loadInitialQueries_nullArray_displayDataImmediately() {
        // Given a null query array
        mQueries = null;
        mPresenterImpl = new PresenterImpl(mMockModel, mMockUpdatableViews, mUserActions,
                mQueries);

        // When loading initial queries
        mPresenterImpl.loadInitialQueries();

        // Then the views are updated immediately
        verify(mMockUpdatableView1).displayData(mMockModel, null);
        verify(mMockUpdatableView2).displayData(mMockModel, null);
    }

    @Test
    public void loadInitialQueries_oneItemInArray_modelRequestDataSuccessful_ViewIsUpdated() {
        // Given a query array with one query and the model update request is successful

        // When loading initial queries
        mPresenterImpl.loadInitialQueries();

        // Then a data request of the model is called for the query and callback is captured
        verify(mMockModel).requestData(eq(mQueries[0]), mDataQueryCallbackCaptor.capture());

        // When the callback is successful
        mDataQueryCallbackCaptor.getValue()
                                .onModelUpdated(mMockModel, mQueries[0]); // Trigger callback

        // Then the views are updated
        verify(mMockUpdatableView1).displayData(mMockModel, mQueries[0]);
        verify(mMockUpdatableView2).displayData(mMockModel, mQueries[0]);
    }

    @Test
    public void
    loadInitialQueries_oneItemInArray_modelRequestDataUnsuccessful_ViewIsUpdatedWithError() {
        // Given a query array with one query and the model update request is unsuccessful

        // When loading initial queries
        mPresenterImpl.loadInitialQueries();

        // Then a data request of the model is called for the query and callback is captured
        verify(mMockModel).requestData(eq(mQueries[0]), mDataQueryCallbackCaptor.capture());

        // When the callback is unsuccessful
        mDataQueryCallbackCaptor.getValue().onError(mQueries[0]); // Trigger callback

        // Then the views are updated with error
        verify(mMockUpdatableView1).displayErrorMessage(mQueries[0]);
        verify(mMockUpdatableView2).displayErrorMessage(mQueries[0]);
    }

    @Test
    public void loadInitialQueries_threeItemsInArray_modelRequestDataForEachQuery() {
        // Given a query array with 3 queries
        mQueries = new QueryEnum[3];
        mQueries[0] = createQueryEnumWithId(1);
        mQueries[1] = createQueryEnumWithId(2);
        mQueries[2] = createQueryEnumWithId(3);
        mPresenterImpl = new PresenterImpl(mMockModel, mMockUpdatableViews, mUserActions,
                mQueries);

        // When loading initial queries
        mPresenterImpl.loadInitialQueries();

        // Then a data request of the model is called for each query
        verify(mMockModel).requestData(eq(mQueries[0]), any(Model.DataQueryCallback.class));
        verify(mMockModel).requestData(eq(mQueries[1]), any(Model.DataQueryCallback.class));
        verify(mMockModel).requestData(eq(mQueries[2]), any(Model.DataQueryCallback.class));
    }

    @Test
    public void onUserAction_validUserAction_deliveryUserActionSuccessful_ViewIsUpdated() {
        // Given a user action array with 1 action

        // When calling user action with that user action and bundle
        Bundle bundle = new Bundle();
        mPresenterImpl.onUserAction(mUserActions[0], bundle);

        // Then delivering user action to the model is called for the user action and bundle and
        // callback is captured
        verify(mMockModel).deliverUserAction(eq(mUserActions[0]), eq(bundle),
                mUserActionCallbackCaptor.capture());

        // When the callback is successful
        mUserActionCallbackCaptor.getValue()
                                 .onModelUpdated(mMockModel, mUserActions[0]); // Trigger callback

        // Then the views are updated
        verify(mMockUpdatableView1).displayUserActionResult(mMockModel, mUserActions[0], true);
        verify(mMockUpdatableView2).displayUserActionResult(mMockModel, mUserActions[0], true);
    }

    @Test
    public void
    onUserAction_validUserAction_deliveryUserActionUnsuccessful_ViewIsUpdatedWithError() {
        // Given a user action array with 1 action

        // When calling user action with that user action and bundle
        Bundle bundle = new Bundle();
        mPresenterImpl.onUserAction(mUserActions[0], bundle);

        // Then delivering user action to the model is called for the user action and bundle and
        // callback is captured
        verify(mMockModel).deliverUserAction(eq(mUserActions[0]), eq(bundle),
                mUserActionCallbackCaptor.capture());

        // When the callback is unsuccessful
        mUserActionCallbackCaptor.getValue().onError(mUserActions[0]); // Trigger callback

        // Then the views are updated with error
        verify(mMockUpdatableView1).displayUserActionResult(null, mUserActions[0], false);
        verify(mMockUpdatableView2).displayUserActionResult(null, mUserActions[0], false);
    }
}
