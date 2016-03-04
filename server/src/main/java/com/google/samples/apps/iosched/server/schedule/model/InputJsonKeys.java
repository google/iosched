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
package com.google.samples.apps.iosched.server.schedule.model;

/**
 * Enumeration of VendorAPI Json keys.
 *
 * NOTE: For simplicity' sake, we don't force these enum elements to be
 * uppercase, unlike conventional naming rules, so we can have direct mapping
 * from compilation-time static constants and the actual JSON keys without
 * needing to create an additional (and unnecessary) level of abstraction.
 */
public class InputJsonKeys {

  static public class VendorAPISource {
    static public enum MainTypes {
      rooms, categories, speakers, topics;
    }

    static public enum Rooms {
      Id, Name;
    }

    static public enum Categories {
      Id, Name, ParentId, Description;
    }

    static public enum Speakers {
      Id, Name, Bio, Photo, Info, CompanyName;

      public static String INFO_PUBLIC_PLUS_ID="Google+ Profile";
      public static String INFO_PUBLIC_TWITTER ="Twitter Profile";
    }

    static public enum SpeakersInfo {
      // TODO(arthurthompson): Check if publicPlusId and plusoneUrl are needed. Not used in test data.
      Name, Value; // publicPlusId, plusoneUrl;
    }

    static public enum Topics {
      Id, Title, Description, Start, Finish, CategoryIds, SpeakerIds, Sessions, Documents, Info, Related;

      public static String INFO_VIDEO_URL="Video URL";
      public static String INFO_FEATURED_SESSION="Featured Session";
      public static String INFO_IS_LIVE_STREAM="Is Live Streamed?";
      public static String INFO_HIDDEN_SESSION="Hide from schedule";
      public static String INFO_STREAM_VIDEO_ID="streamedvideoID";
      public static String RELATED_NAME_VIDEO ="video";
      public static String RELATED_NAME_SESSIONS ="related sessions";
    }

    static public enum RelatedTopics {
      Id, Title;
    }

    static public enum Sessions {
      RoomId;
    }
  }


  static public class ExtraSource {
    static public enum MainTypes {
      tag_category_mapping(CategoryTagMapping.category_id.name()),
      tag_conf(TagConf.tag.name());

      private String key;
      private MainTypes() {
        this("Id");
      }
      private MainTypes(String key) {
        this.key = key;
      }
      public String getKey() {
        return key;
      }
    }

    static public enum CategoryTagMapping {
      category_id, tag_name, is_default;
    }

    static public enum TagConf {
      tag, order_in_category, color, hashtag;
    }

    static public enum Map {
      config, tiles, markers;
    }
  }
}
