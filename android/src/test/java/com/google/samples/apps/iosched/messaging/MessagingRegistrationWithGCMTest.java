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

package com.google.samples.apps.iosched.messaging;

import android.app.Activity;
import android.test.suitebuilder.annotation.SmallTest;
import android.text.TextUtils;

import com.google.android.gcm.GCMRegistrar;
import com.google.samples.apps.iosched.BuildConfig;
import com.google.samples.apps.iosched.gcm.ServerUtilities;
import com.google.samples.apps.iosched.util.AccountUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({GCMRegistrar.class, TextUtils.class, ServerUtilities.class, AccountUtils.class})
@SmallTest
public class MessagingRegistrationWithGCMTest {

    @Mock
    private Activity mMockActivity;

    private MessagingRegistrationWithGCM mMessagingRegistrationWithGCM;

    @Before
    public void setUp() {
        // Create an instance of the class under test
        mMessagingRegistrationWithGCM = spy(new MessagingRegistrationWithGCM(mMockActivity));
    }

    @Test
    public void registerDevice_NoRegistrationId_RegistersWithGCM() {
        // Given an empty registration id
        PowerMockito.mockStatic(ServerUtilities.class);
        PowerMockito.mockStatic(GCMRegistrar.class);
        PowerMockito.mockStatic(TextUtils.class);
        BDDMockito.given(TextUtils.isEmpty(any(String.class))).willReturn(true);

        // When calling to register the device
        mMessagingRegistrationWithGCM.registerDevice();

        // Then registration with GCM is called
        verifyStatic();
        GCMRegistrar.register(mMockActivity, BuildConfig.GCM_SENDER_ID);
        // And registration with server is not called
        verifyStatic(times(0));
        ServerUtilities.register(eq(mMockActivity), any(String.class), any(String.class));

    }

    @Test
    public void registerDevice_RegistrationIdAndRegisteredOnServer_DoesNothing() {
        // Given a non empty registration id
        PowerMockito.mockStatic(GCMRegistrar.class);
        PowerMockito.mockStatic(TextUtils.class);
        BDDMockito.given(TextUtils.isEmpty(any(String.class))).willReturn(false);
        // And an id already registered on server
        setUpAccountName();
        PowerMockito.mockStatic(ServerUtilities.class);
        BDDMockito.given(ServerUtilities.isRegisteredOnServer(eq(mMockActivity), any(String.class)))
                  .willReturn(true);

        // When calling to register the device
        mMessagingRegistrationWithGCM.registerDevice();

        // Then registration with GCM is not called
        verifyStatic(times(0));
        GCMRegistrar.register(mMockActivity, BuildConfig.GCM_SENDER_ID);
        // And registration with server is not called
        verifyStatic(times(0));
        ServerUtilities.register(eq(mMockActivity), any(String.class), any(String.class));

    }

    @Test
    public void registerDevice_RegistrationIdAndNotRegisteredOnServer_RegistersOnServer() {
        // Given a non empty registration id
        PowerMockito.mockStatic(GCMRegistrar.class);
        PowerMockito.mockStatic(TextUtils.class);
        BDDMockito.given(TextUtils.isEmpty(any(String.class))).willReturn(false);
        // And an id isn't registered on server
        setUpAccountName();
        PowerMockito.mockStatic(ServerUtilities.class);
        BDDMockito.given(ServerUtilities.isRegisteredOnServer(eq(mMockActivity), any(String.class)))
                  .willReturn(false);

        // When calling to register the device
        mMessagingRegistrationWithGCM.registerDevice();

        // Then registration with GCM is not called
        verifyStatic(times(0));
        GCMRegistrar.register(mMockActivity, BuildConfig.GCM_SENDER_ID);
        // And async task to register on server is created
        verify(mMessagingRegistrationWithGCM)
                .getGCMRegisterTask(any(String.class), any(String.class));

    }

    private void setUpAccountName() {
        PowerMockito.mockStatic(AccountUtils.class);
        BDDMockito.given(AccountUtils.hasActiveAccount(mMockActivity)).willReturn(true);
        BDDMockito.given(AccountUtils.getActiveAccountName(mMockActivity)).willReturn("name");
    }

}
