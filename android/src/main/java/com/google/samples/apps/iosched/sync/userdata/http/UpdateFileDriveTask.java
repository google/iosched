/**
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

package com.google.samples.apps.iosched.sync.userdata.http;

import java.io.IOException;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

/**
 * Updates a string content to a file on appdata folder.
 *
 */
public class UpdateFileDriveTask extends DriveTask {

    /**
     * Constructs a new task.
     * @param driveService A drive service.
     */
    public UpdateFileDriveTask(Drive driveService) {
        super(driveService);
    }

    /**
     * Executes the request.
     * @param content The new file content.
     * @throws IOException
     */
    public void execute(String content) throws IOException {
        // updates the existing preferences file with
        // the preferences
        File preferences = getOrCreateFile();
        updatePreferencesFile(preferences, content);
    }

}
