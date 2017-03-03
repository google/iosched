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

package com.google.samples.apps.iosched;

import org.junit.Test;

import android.app.Activity;
import android.app.Application;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class AndroidUnitTest {

    @Test
    public void dummyUnitTestWithMockito() {
        Activity activity = mock(Activity.class);
        assertThat(activity, notNullValue());
        Application app = mock(Application.class);
        when(activity.getApplication()).thenReturn(app);
        assertThat(app, is(equalTo(activity.getApplication())));

        verify(activity).getApplication();
        verifyNoMoreInteractions(activity);
    }
}
