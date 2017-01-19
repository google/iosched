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
package com.google.samples.apps.iosched.server.schedule;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Pattern;

public interface Config {

  public final int CONFERENCE_YEAR = 2016;
  public final boolean STAGING = true;

  @SuppressWarnings("deprecation")
  public static final long[][] CONFERENCE_DAYS =
      new long[][] {
          // start and end of day 1
          { new Date(116, Calendar.MAY, 18, 14, 0, 0).getTime(),
                  new Date(116, Calendar.MAY, 19, 5, 0, 0).getTime() },
          // start and end of day 2
          { new Date(116, Calendar.MAY, 19, 14, 0, 0).getTime(),
                  new Date(116, Calendar.MAY, 20, 0, 0, 0).getTime() },
          // start and end of day 3
          { new Date(116, Calendar.MAY, 20, 14, 0, 0).getTime(),
              new Date(116, Calendar.MAY, 21, 0, 0, 0).getTime() },
      };

  public final Pattern SESSIONS_PATTERN = Pattern.compile("session_data_v(\\d+)\\.(\\d+)\\.json");
  public final String SESSIONS_FORMAT = "session_data_v{0,number,integer}.{1,number,integer}.json";
  public final String MANIFEST_FORMAT_VERSION = "iosched-json-v1";

  /**
   * The manifest version is used to name the manifest file (manifest_v{version}.json) and as
   * the major version of the generated session files.
   */
  public final int MANIFEST_VERSION = 1;

  public final String MANIFEST_NAME = "manifest_v"+MANIFEST_VERSION+".json";
  public final String MANIFEST_NAME_STAGING = "manifest_v"+MANIFEST_VERSION+"__qa_.json";

  public final boolean DEBUG_FIX_DATA = false;
  public final boolean SHOW_UNPUBLISHED_DATA=false;

  public final long TIME_TRAVEL_SHIFT = 0; //Used for dogfooding: -29 * (24*60*60*1000L);
  public final String VIDEO_CATEGORY = "f04c9884-9dd8-e411-b87f-00155d5066d7";

  public final String CLOUD_STORAGE_BUCKET = "io2016-bucket-dev";
  public final String CLOUD_STORAGE_BASE_URL = "https://storage.googleapis.com/"+CLOUD_STORAGE_BUCKET+"/";

  // Used when the CMS doesn't have a proper live stream Youtube URL but we still want to
  // have a non-empty URL so that the app will show the "LIVE" indicator.
  public final String VIDEO_LIVESTREAMURL_FOR_EMPTY = "https://google.com/events/io";


  // GCM confs:
  public static final String GCM_SYNC_URL = "/gcm/send/global/sync_schedule";

  public static final String GCM_URL= "https://io2015-dev.appspot.com"+GCM_SYNC_URL;
  // TODO: Define the API key used to allow GCM device registrations.
  public static final String GCM_API_KEY = "UNDEFINED";

  // Email address used for sending emails. Normally this should be a service account.
  public static final String EMAIL_FROM = "io2015-data.google.com@appspot.gserviceaccount.com";

  // API key used for session CMS access
  public static final String CMS_API_KEY = "UNDEFINED";
  // TODO(arthurthompson): Remove CMS_API_CODE since it is no longer used by vendor API.
  public static final String CMS_API_CODE = "UNDEFINED";

  // See context at b/15452185:
  public static final RoomMapping ROOM_MAPPING = new RoomMapping();
  static class RoomMapping {
    private HashMap<String, String> idMapping = new HashMap<String, String>();
    private HashMap<String, String> titleMapping = new HashMap<String, String>();
    private HashMap<String, String> captionsMapping = new HashMap<String, String>();
    public RoomMapping() {
      idMapping.put("e9f3b25f-d4e2-e411-b87f-00155d5066d7", "keynote");
      idMapping.put("6822c256-d4e2-e411-b87f-00155d5066d7", "sandbox");
      idMapping.put("cbce21ff-2cbe-e411-b87f-00155d5066d7", "room1");
      idMapping.put("3c9c1a44-d4e2-e411-b87f-00155d5066d7", "room2");
      idMapping.put("82588250-d4e2-e411-b87f-00155d5066d7", "room3");
      captionsMapping.put("keynote", "http://io-captions.appspot.com/?event=e0&android=t");
      captionsMapping.put("room1", "http://io-captions.appspot.com/?event=e1&android=t");
      captionsMapping.put("room2", "http://io-captions.appspot.com/?event=e2&android=t");
      captionsMapping.put("room3", "http://io-captions.appspot.com/?event=e3&android=t");
      // Currently no title mappings present. But if needed:
      // titleMapping.put("dddsandbox", "Design, Develop, Distribute");
    }
    public String getRoomId(String originalRoomId) {
      if (idMapping.containsKey(originalRoomId)) {
        return idMapping.get(originalRoomId);
      }
      return originalRoomId;
    }
    public String getTitle(String newId, String originalRoomTitle) {
      if (titleMapping.containsKey(newId)) {
        return titleMapping.get(newId);
      }
      return originalRoomTitle;
    }
    public String getCaptions(String newId) {
      if (captionsMapping.containsKey(newId)) {
        return captionsMapping.get(newId);
      }
      return null;
    }
  }
}
