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
 * Enumeration of IOSched Json keys.
 *
 * NOTE: For simplicity' sake, we don't force these enum elements to be
 * uppercase, unlike conventional naming rules, so we can have direct mapping
 * from compilation-time static constants and the actual JSON keys without
 * needing to create an additional (and unnecessary) level of abstraction.
 */
public class OutputJsonKeys {

  static public enum MainTypes {
    rooms, blocks, tags, speakers, sessions, search_suggestions, map, video_library;
  }

  static public enum Rooms {
    id, name, floor, original_id;
  }

  static public enum Blocks {
    title, subtitle, type, start, end;
  }

  static public enum Tags {
    tag, category, name, order_in_category, color, _abstract, original_id, hashtag, photoUrl;
  }

  static public enum Speakers {
    id, name, bio, company, thumbnailUrl, publicPlusId, plusoneUrl, twitterUrl;
  }

  static public enum Sessions {
    id, title, description, startTimestamp, endTimestamp, tags, mainTag,
    hashtag, isFeatured, isLivestream, youtubeUrl, captionsUrl, speakers, room, photoUrl, color,
    relatedSessions, relatedContent, url;
  }

  static public enum RelatedContent {
    id, title;
  }

  static public enum Map {
    config, tiles, markers;
  }

  static public enum VideoLibrary {
    id, year, title, desc, vid, thumbnailUrl, topic, speakers;
  }
}
