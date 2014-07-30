/*
 * Copyright 2014 Google Inc. All rights reserved.
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

package com.google.samples.apps.iosched.ui.debug;

import android.content.Context;

/**
 * Simple generic interface around debug actions.
 * Debug actions that implement this interface can be easily added as buttons to the
 * DebugActionRunnerFragment and have their output status, timing and message
 * logged into the log area.
 */
public interface DebugAction {
    void run(Context context, Callback callback);
    String getLabel();

    public interface Callback {
        void done(boolean success, String message);
    }
}
