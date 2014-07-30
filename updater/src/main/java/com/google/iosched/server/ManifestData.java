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
package com.google.iosched.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.iosched.Config;

import java.text.MessageFormat;
import java.util.regex.Matcher;

public class ManifestData {
  public int minorVersion;
  public int majorVersion;
  public String sessionsFilename;
  public JsonArray dataFiles;

  public void setFromDataFiles(JsonArray files) {
    for (JsonElement file: files) {
      String filename = file.getAsString();
      Matcher matcher = Config.SESSIONS_PATTERN.matcher(filename);
      if (matcher.matches()) {
        sessionsFilename = filename;
        majorVersion = Integer.parseInt(matcher.group(1));
        minorVersion = Integer.parseInt(matcher.group(2));
      } else {
        dataFiles.add(file);
      }
    }
  }

  public void incrementMinorVersion() {
      minorVersion++;
      sessionsFilename = MessageFormat.format(Config.SESSIONS_FORMAT, majorVersion, minorVersion);
  }

}
