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
package com.google.iosched;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Pattern;

public interface Config {

  public final int CONFERENCE_YEAR = 2014;

  @SuppressWarnings("deprecation")
  public static final long[][] CONFERENCE_DAYS =
      new long[][] {
          // start and end of day 1
          { new Date(114, Calendar.JUNE, 25, 7, 0, 0).getTime(),
            new Date(114, Calendar.JUNE, 26, 6, 59, 59).getTime() },
          // start and end of day 2
          { new Date(114, Calendar.JUNE, 26, 7, 0, 0).getTime(),
            new Date(114, Calendar.JUNE, 27, 6, 59, 59).getTime() }
      };

  public final Pattern SESSIONS_PATTERN = Pattern.compile("session_data_v(\\d+)\\.(\\d+)\\.json");
  public final String SESSIONS_FORMAT = "session_data_v{0,number,integer}.{1,number,integer}.json";
  public final String MANIFEST_FORMAT_VERSION = "iosched-json-v1";

  /**
   * The manifest version is used to name the manifest file (manifest_v{version}.json) and as
   * the major version of the generated session files.
   */
  public final int MANIFEST_VERSION = 3;

  public final String MANIFEST_NAME = "manifest_v"+MANIFEST_VERSION+".json";
  public final String MANIFEST_NAME_STAGING = "manifest_v"+MANIFEST_VERSION+"__qa_.json";

  public final long TIME_TRAVEL_SHIFT = 0; //Used for dogfooding: -29 * (24*60*60*1000L);

  // Video sessions are handled differently, they go to the Video library
  // instead of the normal session list. 
  public final String VIDEO_CATEGORY = "ID_OF_THE_CATEGORY_THAT_MAKES_A_VIDEO_SESSION";

  public final String CLOUD_STORAGE_BUCKET = "YOUR_APPENGINE_PROJECTNAME.appspot.com";
  public final String CLOUD_STORAGE_BASE_URL = "http://storage.googleapis.com/"+CLOUD_STORAGE_BUCKET+"/";

  // Used when the CMS doesn't have a proper live stream Youtube URL but we still want to
  // have a non-empty URL so that the app will show the "LIVE" indicator.
  public final String VIDEO_LIVESTREAMURL_FOR_EMPTY = "https://google.com/events/io";

  public static final String EMAIL_TO_SEND_UPDATE_ERRORS = "YOUREMAIL@PLEASECHANGE.ME";

  public static final String SESSION_BASE_URL = "https://www.google.com/events/io/schedule/session/";

  public static final String PHOTO_BASE_URL = "http://storage.googleapis.com/YOUR_APPENGINE_PROJECTNAME.appspot.com/images/";


  // GCM confs:
  public static final String GCM_SYNC_URL = "/send/global/sync_schedule";

  public static final String GCM_PROD= "https://YOUR_GCM_PROD_SERVER"+GCM_SYNC_URL;
  public static final String GCM_DEV = "https://YOUR_GCM_DEV_SERVER"+GCM_SYNC_URL;
  public static final String GCM_API_KEY_PROD = "ADMIN_KEY_TO_YOUR_PROD_GCM_SERVER";
  public static final String GCM_API_KEY_DEV = "ADMIN_KEY_TO_YOUR_DEV_GCM_SERVER";

  public static final RoomMapping ROOM_MAPPING = new RoomMapping();
  static class RoomMapping {
    private HashMap<String, String> idMapping = new HashMap<String, String>();
    private HashMap<String, String> titleMapping = new HashMap<String, String>();
    private HashMap<String, String> captionsMapping = new HashMap<String, String>();
    public RoomMapping() {
      idMapping.put("ROOM_ID_THAT_NEEDS_TO_BE_CHANGED_1", "NEW_ROOM_ID_1");
      idMapping.put("ROOM_ID_THAT_NEEDS_TO_BE_CHANGED_2", "NEW_ROOM_ID_2");
      titleMapping.put("NEW_ROOM_ID_1", "This is a nice title for Room 1");
      titleMapping.put("NEW_ROOM_ID_2", "This is a nice title for Room 2");
      captionsMapping.put("NEW_ROOM_ID_1", "http://CAPTIONS_SERVER.appspot.com/?room=NEW_ROOM_ID_1");
      captionsMapping.put("NEW_ROOM_ID_2", "http://CAPTIONS_SERVER.appspot.com/?room=NEW_ROOM_ID_2");
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
