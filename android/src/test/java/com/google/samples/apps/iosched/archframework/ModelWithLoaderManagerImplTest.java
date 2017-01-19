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
import android.test.suitebuilder.annotation.SmallTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static com.google.samples.apps.iosched.archframework.ArchFrameworkHelperForTest
        .createQueryEnumWithId;
import static com.google.samples.apps.iosched.archframework.ArchFrameworkHelperForTest
        .createUserActionEnumWithId;
import static com.google.samples.apps.iosched.archframework.ModelWithLoaderManager.KEY_RUN_QUERY_ID;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class ModelWithLoaderManagerImplTest {

    @Mock
    private LoaderManager mMockLoaderManager;

    @Mock
    private Model.DataQueryCallback mMockDataQueryCallback;

    @Mock
    private Model.UserActionCallback mMockUserActionCallback;

    @Mock
    private Loader<Cursor> mMockLoaderCursor;

    @Mock
    private Bundle mMockBundle;

    @Rule
    public ExpectedException mThrown = ExpectedException.none();

    private UserActionEnum[] mUserActions;

    private QueryEnum[] mQueries;

    private ModelWithLoaderManagerImpl mModelWithLoaderManagerImpl;

    @Before
    public void setUp() {
        // With one query with id 1 and one user action with id 1
        mQueries = new QueryEnum[1];
        mQueries[0] = createQueryEnumWithId(1);
        mUserActions = new UserActionEnum[1];
        mUserActions[0] = createUserActionEnumWithId(1);
        // A mock loader cursor with id corresponding to the query
        when(mMockLoaderCursor.getId()).thenReturn(mQueries[0].getId());

        mModelWithLoaderManagerImpl = new ModelWithLoaderManagerImpl(mQueries, mUserActions,
                mMockLoaderManager);
    }

    @Test
    public void getQueries_returnsQueries() {
        // When getting queries
        QueryEnum[] queries = mModelWithLoaderManagerImpl.getQueries();

        // Then the returned queries are those passed in the constructor
        assertEquals(mQueries, queries);
    }

    @Test
    public void getUserActions_returnsUserActions() {
        // When getting user actions
        UserActionEnum[] userActions = mModelWithLoaderManagerImpl.getUserActions();

        // Then the returned user actions are those passed in the constructor
        assertEquals(mUserActions, userActions);
    }


    @Test
    public void requestData_withNullQuery_throwsException() {
        // Expected exception
        mThrown.expect(Exception.class);

        // When requesting data with null query
        mModelWithLoaderManagerImpl.requestData(null, mMockDataQueryCallback);
    }

    @Test
    public void deliverUserAction_withNullUserAction_throwsException() {
        // Expected exception
        mThrown.expect(Exception.class);

        // When delivering user action with null user action
        mModelWithLoaderManagerImpl.deliverUserAction(null, null, mMockUserActionCallback);
    }

    @Test
    public void requestData_withNullQueryCallback_throwsException() {
        // Expected exception
        mThrown.expect(Exception.class);

        // When requesting data with null query callback
        mModelWithLoaderManagerImpl.requestData(createQueryEnumWithId(1), null);
    }

    @Test
    public void deliverUserAction_withNullUserActionCallback_throwsException() {
        // Expected exception
        mThrown.expect(Exception.class);

        // When delivering user action with null user action callback
        mModelWithLoaderManagerImpl.deliverUserAction(createUserActionEnumWithId(1), null, null);
    }

    @Test
    public void requestData_withValidQuery_initialisesLoader() {
        // When requesting data with valid query
        mModelWithLoaderManagerImpl.requestData(mQueries[0], mMockDataQueryCallback);

        // Then loader is initialised
        verify(mMockLoaderManager).initLoader(eq(mQueries[0].getId()), any(Bundle.class),
                any(LoaderManager.LoaderCallbacks.class));
    }

    @Test
    public void requestData_withInvalidQuery_callbackUpdatedWithError() {
        // Given an invalid query
        final QueryEnum invalidQuery = createQueryEnumWithId(2);

        // When requesting data with invalid query
        mModelWithLoaderManagerImpl.requestData(invalidQuery, mMockDataQueryCallback);

        // Then the callback is updated with error
        verify(mMockDataQueryCallback).onError(invalidQuery);
    }

    @Test
    public void onLoadFinished_readDataSuccessfully_callbackUpdated() {
        // Reading data is an abstract method, our impl class uses a fake boolean for its
        // implementation to allow us to fully test the concrete methods
        ModelWithLoaderManagerImpl.READ_SUCCESS = true;

        // Given a loader cursor for a valid query
        when(mMockLoaderCursor.getId()).thenReturn(mQueries[0].getId());
        // And a data request for the same valid query
        mModelWithLoaderManagerImpl.requestData(mQueries[0], mMockDataQueryCallback);

        // When the load is finished
        mModelWithLoaderManagerImpl.onLoadFinished(mMockLoaderCursor, null);

        // Then the callback is updated
        verify(mMockDataQueryCallback).onModelUpdated(mModelWithLoaderManagerImpl, mQueries[0]);
    }

    @Test
    public void onLoadFinished_readDataUnsuccessfully_callbackUpdatedWithError() {
        // Reading data is an abstract method, our impl class uses a fake boolean for its
        // implementation to allow us to fully test the concrete methods
        ModelWithLoaderManagerImpl.READ_SUCCESS = false;

        // Given a loader cursor for a valid query
        when(mMockLoaderCursor.getId()).thenReturn(mQueries[0].getId());
        // And a data request for the same valid query
        mModelWithLoaderManagerImpl.requestData(mQueries[0], mMockDataQueryCallback);

        // When the load is finished
        mModelWithLoaderManagerImpl.onLoadFinished(mMockLoaderCursor, null);

        // Then the callback is updated with error
        verify(mMockDataQueryCallback).onError(mQueries[0]);
    }

    @Test
    public void deliverUserAction_withUserActionWithNoQueryBundle_processesUserAction() {
        // Set up spy model
        ModelWithLoaderManagerImpl spyModel = spy(new ModelWithLoaderManagerImpl(mQueries,
                mUserActions, mMockLoaderManager));

        // Given a user action
        UserActionEnum userAction = createUserActionEnumWithId(1);

        // When delivering user action with no query bundle
        spyModel.deliverUserAction(userAction, null, mMockUserActionCallback);

        // Then the model processes the user action
        verify(spyModel).processUserAction(userAction, null, mMockUserActionCallback);
    }

    @Test
    public void deliverUserAction_withUserActionWithQueryBundleWithValidInteger_restartsLoader() {
        // Given a user action and a bundle with a query key with a valid integer value
        UserActionEnum userAction = createUserActionEnumWithId(1);
        when(mMockBundle.containsKey(KEY_RUN_QUERY_ID)).thenReturn(true);
        when(mMockBundle.get(KEY_RUN_QUERY_ID)).thenReturn(mQueries[0].getId());

        // When delivering user action with that query bundle
        mModelWithLoaderManagerImpl
                .deliverUserAction(userAction, mMockBundle, mMockUserActionCallback);

        // Then the loader manager restarts the loader
        verify(mMockLoaderManager).restartLoader(eq(mQueries[0].getId()), eq(mMockBundle),
                any(LoaderManager.LoaderCallbacks.class));
    }

    @Test
    public void
    deliverUserAction_withUserActionWithQueryBundleWithInvalidInteger_callbackUpdatesWithError() {
        // Given a user action and a bundle with a query key with an invalid integer value
        UserActionEnum userAction = createUserActionEnumWithId(1);
        when(mMockBundle.containsKey(KEY_RUN_QUERY_ID)).thenReturn(true);
        when(mMockBundle.get(KEY_RUN_QUERY_ID)).thenReturn(mQueries[0].getId() + 1);

        // When delivering user action with that query bundle
        mModelWithLoaderManagerImpl
                .deliverUserAction(userAction, mMockBundle, mMockUserActionCallback);

        // Then the callback is updated with error
        verify(mMockUserActionCallback).onError(userAction);
    }

    @Test
    public void
    deliverUserAction_withUserActionWithQueryBundleWithoutInteger_callbackUpdatedWithError() {
        // Given a user action and a bundle with a query key with a non integer value
        UserActionEnum userAction = createUserActionEnumWithId(1);
        when(mMockBundle.containsKey(KEY_RUN_QUERY_ID)).thenReturn(true);
        when(mMockBundle.get(KEY_RUN_QUERY_ID)).thenReturn("String");

        // When delivering user action with that query bundle
        mModelWithLoaderManagerImpl
                .deliverUserAction(userAction, mMockBundle, mMockUserActionCallback);

        // Then the callback is updated with error
        verify(mMockUserActionCallback).onError(userAction);
    }

    @Test
    public void
    deliverUserAction_withUserActionWithQueryBundleWithIntegerAndReadDataSuccessfully_callbackUpdated() {
        // Reading data is an abstract method, our impl class uses a fake boolean for its
        // implementation to allow us to fully test the concrete methods
        ModelWithLoaderManagerImpl.READ_SUCCESS = true;

        // Given a user action and a bundle with a query key with a valid integer value
        UserActionEnum userAction = createUserActionEnumWithId(1);
        when(mMockBundle.containsKey(KEY_RUN_QUERY_ID)).thenReturn(true);
        when(mMockBundle.get(KEY_RUN_QUERY_ID)).thenReturn(mQueries[0].getId());
        // And delivering user action with that query bundle
        mModelWithLoaderManagerImpl
                .deliverUserAction(userAction, mMockBundle, mMockUserActionCallback);

        // When the load is finished
        mModelWithLoaderManagerImpl.onLoadFinished(mMockLoaderCursor, null);

        // Then the callback is updated
        verify(mMockUserActionCallback).onModelUpdated(mModelWithLoaderManagerImpl, userAction);
    }

    @Test
    public void
    deliverUserAction_withUserActionWithQueryBundleWithIntegerAndReadDataUnsuccessfully_callbackUpdatedWithError() {
        // Reading data is an abstract method, our impl class uses a fake boolean for its
        // implementation to allow us to fully test the concrete methods
        ModelWithLoaderManagerImpl.READ_SUCCESS = false;

        // Given a user action and a bundle with a query key with a valid integer value
        UserActionEnum userAction = createUserActionEnumWithId(1);
        when(mMockBundle.containsKey(KEY_RUN_QUERY_ID)).thenReturn(true);
        when(mMockBundle.get(KEY_RUN_QUERY_ID)).thenReturn(mQueries[0].getId());
        // And delivering user action with that query bundle
        mModelWithLoaderManagerImpl
                .deliverUserAction(userAction, mMockBundle, mMockUserActionCallback);

        // When the load is finished
        mModelWithLoaderManagerImpl.onLoadFinished(mMockLoaderCursor, null);

        // Then the callback is updated with error
        verify(mMockUserActionCallback).onError(userAction);
    }

    @Test
    public void requestData_loadFinishedTwice_callbackUpdatedTwice() {
        // Reading data is an abstract method, our impl class uses a fake boolean for its
        // implementation to allow us to fully test the concrete methods
        ModelWithLoaderManagerImpl.READ_SUCCESS = true;

        // Given a loader cursor for a valid query
        when(mMockLoaderCursor.getId()).thenReturn(mQueries[0].getId());
        // And a data request for the same valid query
        mModelWithLoaderManagerImpl.requestData(mQueries[0], mMockDataQueryCallback);

        // When the load is finished
        mModelWithLoaderManagerImpl.onLoadFinished(mMockLoaderCursor, null);

        // And the load is finished again
        mModelWithLoaderManagerImpl.onLoadFinished(mMockLoaderCursor, null);

        // Then the callback has been updated twice
        verify(mMockDataQueryCallback, times(2)).onModelUpdated(mModelWithLoaderManagerImpl,
                mQueries[0]);
    }
}
