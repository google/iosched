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

package com.google.samples.apps.iosched.navigation;

import android.content.Context;
import android.test.suitebuilder.annotation.SmallTest;

import com.google.samples.apps.iosched.lib.BuildConfig;
import com.google.samples.apps.iosched.archframework.Model;
import com.google.samples.apps.iosched.navigation.NavigationModel.NavigationItemEnum;
import com.google.samples.apps.iosched.settings.SettingsUtils;
import com.google.samples.apps.iosched.testutils.SettingsMockContext;
import com.google.samples.apps.iosched.util.AccountUtils;
import com.google.samples.apps.iosched.util.RegistrationUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static junit.framework.Assert.assertEquals;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SettingsUtils.class, AccountUtils.class})
@SmallTest
public class NavigationModelTest {

    @Mock
    private Context mMockContext;

    @Mock
    private Model.UserActionCallback mMockUserActionCallback;

    @Mock
    private Model.DataQueryCallback mMockDataQueryCallback;

    private NavigationModel mNavigationModel;

    @Before
    public void setUp() {

        // Create an instance of the model.
        mNavigationModel = new NavigationModel();
    }

    @Test
    public void requestData_LoadItemsForUser_ItemsLoaded() {
        // Given a user attending the conference and logged in
        setUpMockForAttendance(RegistrationUtils.REGSTATUS_REGISTERED);
        setUpMockForLoginStatus(true);
        NavigationItemEnum[] expectedItems = NavigationConfig.filterOutItemsDisabledInBuildConfig(
                NavigationConfig.ITEMS);

        // When the navigation items are requested
        mNavigationModel.requestData(NavigationModel.NavigationQueryEnum.LOAD_ITEMS,
                mMockDataQueryCallback);

        // Then the expected items are loaded into the model
        assertEquals(mNavigationModel.getItems().length, expectedItems.length);
        assertEquals(mNavigationModel.getItems()[0], expectedItems[0]);
    }

    private void setUpMockForAttendance(int attending) {
        SettingsMockContext.initMockContextForAttendingVenueSetting(attending, mMockContext);
    }

    private void setUpMockForLoginStatus(boolean loggedIn) {
        SettingsMockContext.initMockContextForLoggedInStatus(loggedIn, mMockContext);
    }

//    DISABLED: Broken
//    private NavigationItemEnum[] appendDebugIfRequired(NavigationItemEnum[] items) {
//        return BuildConfig.DEBUG ? NavigationConfig.appendItem(items,
//                NavigationModel.NavigationItemEnum.DEBUG) :
//                items;
//    }
}
