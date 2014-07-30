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
package com.google.samples.apps.iosched.ui.debug.actions;

import android.content.Context;
import android.content.Intent;

import com.google.samples.apps.iosched.ui.debug.DebugAction;

public class ShowAllDriveFilesDebugAction implements DebugAction {

    @Override
    public void run(Context context, final Callback callback) {
        context.startActivity(new Intent(context, ViewFilesInAppFolderActivity.class));
    }

    @Override
    public String getLabel() {
        return "List all files in AppData folder";
    }
}
