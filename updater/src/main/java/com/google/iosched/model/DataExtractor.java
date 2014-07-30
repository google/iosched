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
package com.google.iosched.model;

import static com.google.iosched.model.DataModelHelper.get;
import static com.google.iosched.model.DataModelHelper.getAsArray;
import static com.google.iosched.model.DataModelHelper.getMapValue;
import static com.google.iosched.model.DataModelHelper.isHashtag;
import static com.google.iosched.model.DataModelHelper.set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.iosched.Config;
import com.google.iosched.model.InputJsonKeys.ExtraSource;
import com.google.iosched.model.InputJsonKeys.VendorAPISource;
import com.google.iosched.model.validator.Converters;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Encapsulation of the rules that maps Vendor data sources to the IOSched data sources.
 *
 */
public class DataExtractor {

  private HashMap<String, JsonObject> videoSessionsById;
  private HashMap<String, JsonObject> speakersById;
  private HashMap<String, JsonObject> categoryToTagMap;
  private HashSet<String> usedSpeakers, usedTags;
  private JsonElement mainCategory;

  public DataExtractor() {
  }

  public JsonObject extractFromDataSources(JsonDataSources sources) {
    usedTags = new HashSet<String>();
    usedSpeakers = new HashSet<String>();

    JsonObject result = new JsonObject();
    result.add(OutputJsonKeys.MainTypes.rooms.name(), extractRooms(sources));
    JsonArray speakers = extractSpeakers(sources);

    JsonArray tags = extractTags(sources);
    result.add(OutputJsonKeys.MainTypes.video_library.name(), extractVideoSessions(sources));

    result.add(OutputJsonKeys.MainTypes.sessions.name(), extractSessions(sources));

    // Remove tags that are not used on any session (b/14419126)
    Iterator<JsonElement> tagsIt = tags.iterator();
    while (tagsIt.hasNext()) {
      JsonElement tag = tagsIt.next();
      String tagName = get(tag.getAsJsonObject(), OutputJsonKeys.Tags.tag).getAsString();
      if (!usedTags.contains(tagName)) {
        tagsIt.remove();
      }
    }

    // Remove speakers that are not used on any session:
    Iterator<JsonElement> it = speakers.iterator();
    while (it.hasNext()) {
      JsonElement el = it.next();
      String id = get(el.getAsJsonObject(), OutputJsonKeys.Speakers.id).getAsString();
      if (!usedSpeakers.contains(id)) {
        it.remove();
      }
    }

    result.add(OutputJsonKeys.MainTypes.speakers.name(), speakers);
    result.add(OutputJsonKeys.MainTypes.tags.name(), tags);
    return result;
  }

  public JsonArray extractRooms(JsonDataSources sources) {
    HashSet<String> ids = new HashSet<String>();
    JsonArray result = new JsonArray();
    JsonDataSource source = sources.getSource(VendorAPISource.MainTypes.rooms.name());
    if (source != null) {
      for (JsonObject origin: source) {
        JsonObject dest = new JsonObject();
        JsonElement originalId = get(origin, VendorAPISource.Rooms.id);
        String id = Config.ROOM_MAPPING.getRoomId(originalId.getAsString());
        if (!ids.contains(id)) {
          String title = Config.ROOM_MAPPING.getTitle(id, get(origin, VendorAPISource.Rooms.name).getAsString());
          set(new JsonPrimitive(id), dest, OutputJsonKeys.Rooms.id);
          set(originalId, dest, OutputJsonKeys.Rooms.original_id);
          set(new JsonPrimitive(title), dest, OutputJsonKeys.Rooms.name);
          result.add(dest);
          ids.add(id);
        }
      }
    }
    return result;
  }

  public JsonArray extractTags(JsonDataSources sources) {
    JsonArray result = new JsonArray();
    JsonDataSource source = sources.getSource(VendorAPISource.MainTypes.categories.name());
    JsonDataSource tagCategoryMappingSource = sources.getSource(ExtraSource.MainTypes
        .tag_category_mapping.name());
    JsonDataSource tagsConfSource = sources.getSource(ExtraSource.MainTypes.tag_conf.name());

    categoryToTagMap = new HashMap<String, JsonObject>();

    // Only for checking duplicates.
    HashSet<String> originalTagNames = new HashSet<String>();

    if (source != null) {
      for (JsonObject origin: source) {
        JsonObject dest = new JsonObject();

        // set tag category, looking for parentid in the tag_category_mapping data source
        JsonElement parentId = get(origin, VendorAPISource.Categories.parentid);

        // Ignore categories with null parents, because they are roots (tag categories).
        if (parentId != null && !parentId.getAsString().equals("")) {
          JsonElement category = null;
          if (tagCategoryMappingSource != null) {
            JsonObject categoryMapping = tagCategoryMappingSource.getElementById(parentId.getAsString());
            if (categoryMapping != null) {
              category = get(categoryMapping, ExtraSource.CategoryTagMapping.tag_name);
              JsonPrimitive isDefault = (JsonPrimitive) get(categoryMapping, ExtraSource.CategoryTagMapping.is_default);
              if ( isDefault != null && isDefault.getAsBoolean() ) {
                mainCategory = category;
              }
            }
            set(category, dest, OutputJsonKeys.Tags.category);
          }

          // Ignore categories unrecognized parents (no category)
          if (category == null) {
            continue;
          }

          // Tag name is by convention: "TAGCATEGORY_TAGNAME"
          JsonElement name = get(origin, VendorAPISource.Categories.name);
          JsonElement tagName = new JsonPrimitive(category.getAsString() + "_" +
              Converters.TAG_NAME.convert(name).getAsString());
          JsonElement originalTagName = tagName;

          set(tagName, dest, OutputJsonKeys.Tags.tag);
          set(name, dest, OutputJsonKeys.Tags.name);
          set(origin, VendorAPISource.Categories.id, dest, OutputJsonKeys.Tags.original_id);
          set(origin, VendorAPISource.Categories.description, dest, OutputJsonKeys.Tags._abstract, null);

          if (tagsConfSource != null) {
            JsonObject tagConf = tagsConfSource.getElementById(originalTagName.getAsString());
            if (tagConf != null) {
              set(tagConf, ExtraSource.TagConf.order_in_category, dest, OutputJsonKeys.Tags.order_in_category);
              set(tagConf, ExtraSource.TagConf.color, dest, OutputJsonKeys.Tags.color);
              set(tagConf, ExtraSource.TagConf.hashtag, dest, OutputJsonKeys.Tags.hashtag);
            }
          }

          categoryToTagMap.put(get(origin, VendorAPISource.Categories.id).getAsString(), dest);
          if (originalTagNames.add(originalTagName.getAsString())) {
            result.add(dest);
          }
        }
      }
    }
    return result;
  }

  public JsonArray extractSpeakers(JsonDataSources sources) {
    speakersById = new HashMap<String, JsonObject>();
    JsonArray result = new JsonArray();
    JsonDataSource source = sources.getSource(VendorAPISource.MainTypes.speakers.name());
    if (source != null) {
      for (JsonObject origin: source) {
        JsonObject dest = new JsonObject();
        JsonElement id = get(origin, VendorAPISource.Speakers.id);
        set(id, dest, OutputJsonKeys.Speakers.id);
        set(origin, VendorAPISource.Speakers.name, dest, OutputJsonKeys.Speakers.name, null);
        set(origin, VendorAPISource.Speakers.bio, dest, OutputJsonKeys.Speakers.bio, null);
        set(origin, VendorAPISource.Speakers.companyname, dest, OutputJsonKeys.Speakers.company, null);
        JsonElement originalPhoto = get(origin, VendorAPISource.Speakers.photo);
        if (originalPhoto != null && !"".equals(originalPhoto.getAsString())) {
          // Note that the input for SPEAKER_PHOTO_ID converter is the entity ID. We simply ignore the original
          // photo URL, because that will be processed by an offline cron script, resizing the
          // photos and saving them to a known location with the entity ID as its base name.
          set(origin, VendorAPISource.Speakers.id, dest, OutputJsonKeys.Speakers.thumbnailUrl, Converters.SPEAKER_PHOTO_URL);
        }
        JsonElement info = origin.get(VendorAPISource.Speakers.info.name());
        JsonPrimitive plusUrl = getMapValue(info, InputJsonKeys.VendorAPISource.Speakers.INFO_PUBLIC_PLUS_ID, Converters.GPLUS_URL, null);
        if (plusUrl != null) {
          set(plusUrl, dest, OutputJsonKeys.Speakers.plusoneUrl);
        }
        result.add(dest);
        speakersById.put(id.getAsString(), dest);
      }
    }
    return result;
  }

  public JsonArray extractSessions(JsonDataSources sources) {
    if (videoSessionsById == null) {
      throw new IllegalStateException("You need to extract video sessions before attempting to extract sessions");
    }
    if (categoryToTagMap == null) {
      throw new IllegalStateException("You need to extract tags before attempting to extract sessions");
    }

    JsonArray result = new JsonArray();
    JsonDataSource source = sources.getSource(VendorAPISource.MainTypes.topics.name());
    if (source != null) {
      for (JsonObject origin: source) {
        if (isVideoSession(origin)) {
          // Sessions with the Video tag are processed as video library content
          continue;
        }
        if (isHiddenSession(origin)) {
          // Sessions with a "Hidden from schedule" flag should be ignored
          continue;
        }
        JsonElement title = get(origin, VendorAPISource.Topics.title);
        // Since the CMS returns an empty keynote as a session, we need to ignore it
        if (title != null && title.isJsonPrimitive() && "keynote".equalsIgnoreCase(title.getAsString())) {
          continue;
        }
        JsonObject dest = new JsonObject();
        set(origin, VendorAPISource.Topics.id, dest, OutputJsonKeys.Sessions.id);
        set(origin, VendorAPISource.Topics.id, dest, OutputJsonKeys.Sessions.url, Converters.SESSION_URL);
        set(origin, VendorAPISource.Topics.title, dest, OutputJsonKeys.Sessions.title, null);
        set(origin, VendorAPISource.Topics.description, dest, OutputJsonKeys.Sessions.description, null);
        set(origin, VendorAPISource.Topics.start, dest, OutputJsonKeys.Sessions.startTimestamp, Converters.DATETIME);
        set(origin, VendorAPISource.Topics.finish, dest, OutputJsonKeys.Sessions.endTimestamp, Converters.DATETIME);

        JsonElement documents = get(origin, VendorAPISource.Topics.documents);
        if (documents != null && documents.isJsonArray() && documents.getAsJsonArray().size()>0) {
          // Note that the input for SessionPhotoURL is the entity ID. We simply ignore the original
          // photo URL, because that will be processed by an offline cron script, resizing the
          // photos and saving them to a known location with the entity ID as its base name.
          set(origin, VendorAPISource.Topics.id, dest, OutputJsonKeys.Sessions.photoUrl, Converters.SESSION_PHOTO_URL);
        }

        setVideoPropertiesInSession(origin, dest);
        setRelatedVideos(origin, dest);

        JsonElement mainTag = null;
        JsonElement hashtag = null;
        JsonElement mainTagColor = null;
        JsonArray categories= origin.getAsJsonArray(VendorAPISource.Topics.categoryids.name());
        JsonArray tags = new JsonArray();
        for (JsonElement category: categories) {
          JsonObject tag = categoryToTagMap.get(category.getAsString());
          if (tag != null) {
            JsonElement tagName = get(tag, OutputJsonKeys.Tags.tag);
            tags.add(tagName);
            usedTags.add(tagName.getAsString());

            if (mainTag == null) {
              // check if the tag is from a "default" category. For example, if THEME is the default
              // category, all sessions will have a "mainTag" property set to the first tag of type THEME
              JsonElement tagCategory = get(tag, OutputJsonKeys.Tags.category); // THEME, TYPE or TOPIC
              if (tagCategory.equals(mainCategory)) {
                mainTag = tagName;
                mainTagColor = get(tag, OutputJsonKeys.Tags.color);
              }
              if (hashtag == null && isHashtag(tag)) {
                hashtag = get(tag, OutputJsonKeys.Tags.hashtag);
                if (hashtag == null || hashtag.getAsString() == null || hashtag.getAsString().isEmpty()) {
                  // If no hashtag set in the tagsconf file, we will convert the tagname to find one:
                  hashtag = new JsonPrimitive(get(tag, OutputJsonKeys.Tags.name, Converters.TAG_NAME)
                          .getAsString().toLowerCase());
                }
              }
            }
          }
        }
        set(tags, dest, OutputJsonKeys.Sessions.tags);
        if (mainTag != null) {
          set(mainTag, dest, OutputJsonKeys.Sessions.mainTag);
        }
        if (mainTagColor != null) {
          set(mainTagColor, dest, OutputJsonKeys.Sessions.color);
        }
        if (hashtag != null) {
          set(hashtag, dest, OutputJsonKeys.Sessions.hashtag);
        }

        JsonArray speakers = getAsArray(origin, VendorAPISource.Topics.speakerids);
        if (speakers != null) for (JsonElement speaker: speakers) {
            String speakerId = speaker.getAsString();
            usedSpeakers.add(speakerId);
        }
        set(speakers, dest, OutputJsonKeys.Sessions.speakers);

        JsonArray sessions= origin.getAsJsonArray(VendorAPISource.Topics.sessions.name());
        if (sessions != null && sessions.size()>0) {
          String roomId = get(sessions.get(0).getAsJsonObject(), VendorAPISource.Sessions.roomid).getAsString();
          roomId = Config.ROOM_MAPPING.getRoomId(roomId);
          set(new JsonPrimitive(roomId), dest, OutputJsonKeys.Sessions.room);

          // captions URL is set based on the session room, so keep it here.
          String captionsURL = Config.ROOM_MAPPING.getCaptions(roomId);
          if (captionsURL != null) {
            set(new JsonPrimitive(captionsURL), dest, OutputJsonKeys.Sessions.captionsUrl);
          }
        }

        result.add(dest);
      }
    }
    return result;
  }

  public JsonArray extractVideoSessions(JsonDataSources sources) {
    videoSessionsById = new HashMap<String, JsonObject>();
    if (categoryToTagMap == null) {
      throw new IllegalStateException("You need to extract tags before attempting to extract video sessions");
    }
    if (speakersById == null) {
      throw new IllegalStateException("You need to extract speakers before attempting to extract video sessions");
    }

    JsonArray result = new JsonArray();
    JsonDataSource source = sources.getSource(VendorAPISource.MainTypes.topics.name());
    if (source != null) {
      for (JsonObject origin: source) {

        if (!isVideoSession(origin)) {
          continue;
        }
        if (isHiddenSession(origin)) {
          // Sessions with a "Hidden from schedule" flag should be ignored
          continue;
        }

        JsonObject dest = new JsonObject();

        JsonPrimitive vid = setVideoForVideoSession(origin, dest);

        JsonElement id = get(origin, VendorAPISource.Topics.id);
        // video library id must be the Youtube video id
        set(vid, dest, OutputJsonKeys.VideoLibrary.id);
        set(origin, VendorAPISource.Topics.title, dest, OutputJsonKeys.VideoLibrary.title, null);
        set(origin, VendorAPISource.Topics.description, dest, OutputJsonKeys.VideoLibrary.desc, null);
        set(new JsonPrimitive(Config.CONFERENCE_YEAR), dest, OutputJsonKeys.VideoLibrary.year);


        JsonElement videoTopic = null;
        JsonArray categories= origin.getAsJsonArray(VendorAPISource.Topics.categoryids.name());
        for (JsonElement category: categories) {
          JsonObject tag = categoryToTagMap.get(category.getAsString());
          if (tag != null) {
            if (isHashtag(tag)) {
              videoTopic = get(tag, OutputJsonKeys.Tags.name);
              // by definition, the first tag that can be a hashtag (usually a TOPIC) is considered the video tag
              break;
            }
          }
        }
        if (videoTopic != null) {
          set(videoTopic, dest, OutputJsonKeys.VideoLibrary.topic);
        }

        // Concatenate speakers:
        JsonArray speakers = getAsArray(origin, VendorAPISource.Topics.speakerids);
        StringBuilder sb = new StringBuilder();
        if (speakers != null) for (int i=0; i<speakers.size(); i++) {
          String speakerId = speakers.get(i).getAsString();
          usedSpeakers.add(speakerId);
          JsonObject speaker = speakersById.get(speakerId);
          if (speaker != null) {
            sb.append(get(speaker, OutputJsonKeys.Speakers.name).getAsString());
            if (i<speakers.size()-1) sb.append(", ");
          }
        }
        set(new JsonPrimitive(sb.toString()), dest, OutputJsonKeys.VideoLibrary.speakers);
        videoSessionsById.put(id.getAsString(), dest);
        result.add(dest);
      }
    }
    return result;
  }

  private boolean isVideoSession(JsonObject sessionObj) {
    JsonArray tags= sessionObj.getAsJsonArray(VendorAPISource.Topics.categoryids.name());
    for (JsonElement category: tags) {
      if (Config.VIDEO_CATEGORY.equals(category.getAsString())) {
        return true;
      }
    }
    return false;
  }

  private boolean isHiddenSession(JsonObject sessionObj) {
    JsonPrimitive hide = getMapValue(
        get(sessionObj, VendorAPISource.Topics.info),
        InputJsonKeys.VendorAPISource.Topics.INFO_HIDDEN_SESSION,
        Converters.BOOLEAN, null);
    if (hide != null && hide.isBoolean() && hide.getAsBoolean()) {
      return true;
    }
    return false;
  }

  private boolean isLivestreamed(JsonObject sessionObj) {
    // data generated after the end of the conference should never have livestream URLs
    long endOfConference = Config.CONFERENCE_DAYS[Config.CONFERENCE_DAYS.length-1][1];
    if (System.currentTimeMillis() > endOfConference ) {
      return false;
    }
    JsonPrimitive livestream = getMapValue(
        get(sessionObj, VendorAPISource.Topics.info),
        InputJsonKeys.VendorAPISource.Topics.INFO_IS_LIVE_STREAM,
        null, null);
    return livestream != null && "true".equalsIgnoreCase(livestream.getAsString());
  }

  private void setVideoPropertiesInSession(JsonObject origin, JsonObject dest) {
    boolean isLivestream = isLivestreamed(origin);
    set(new JsonPrimitive(isLivestream), dest, OutputJsonKeys.Sessions.isLivestream);

    JsonPrimitive vid = null;

    if (isLivestream) {
      vid = getVideoFromTopicInfo(origin, InputJsonKeys.VendorAPISource.Topics.INFO_STREAM_VIDEO_ID,
          Config.VIDEO_LIVESTREAMURL_FOR_EMPTY);
    } else {
      vid = getMapValue(
          get(origin, VendorAPISource.Topics.info), VendorAPISource.Topics.INFO_VIDEO_URL,
          Converters.YOUTUBE_URL, null);
    }
    if (vid != null && !vid.getAsString().isEmpty()) {
      set(vid, dest, OutputJsonKeys.Sessions.youtubeUrl);
    }
  }

  private JsonPrimitive getVideoFromTopicInfo(JsonObject origin, String sourceInfoKey, String defaultVideoUrl) {
    JsonPrimitive result = null;

    JsonPrimitive vid = getMapValue(
        get(origin, VendorAPISource.Topics.info), sourceInfoKey, null, defaultVideoUrl);
    if (!vid.getAsString().isEmpty()) {
      result = vid;
    }
    return (result == null && defaultVideoUrl != null) ? new JsonPrimitive(defaultVideoUrl) : result;
  }

  private JsonPrimitive  setVideoForVideoSession(JsonObject origin, JsonObject dest) {
    JsonPrimitive vid = getVideoFromTopicInfo(origin,
        InputJsonKeys.VendorAPISource.Topics.INFO_VIDEO_URL, null);
    if (vid != null) {
      set(vid, dest, OutputJsonKeys.VideoLibrary.vid);
      JsonPrimitive thumbnail = new JsonPrimitive("http://img.youtube.com/vi/" + vid.getAsString() + "/hqdefault.jpg");
      set(thumbnail, dest, OutputJsonKeys.VideoLibrary.thumbnailUrl);
    }
    return vid;
  }

  private void setRelatedVideos(JsonObject origin, JsonObject dest) {
    JsonArray related = getAsArray(origin, VendorAPISource.Topics.related);
    if (related == null) {
      return;
    }
    for (JsonElement el: related) {
      if (!el.isJsonObject()) {
        continue;
      }
      JsonObject obj = el.getAsJsonObject();
      if (!obj.has("name") || !obj.has("values")) {
        continue;
      }

      if (InputJsonKeys.VendorAPISource.Topics.RELATED_NAME_VIDEO.equals(
          obj.getAsJsonPrimitive("name").getAsString())) {

        JsonElement values = obj.get("values");
        if (!values.isJsonArray()) {
          continue;
        }

        // As per the data specification, related content is formatted as
        // "video1 title1\nvideo2 title2\n..."
        StringBuilder relatedContentStr = new StringBuilder();
        for (JsonElement value: values.getAsJsonArray()) {
          String relatedSessionId = value.getAsString();
          JsonObject relatedVideo = videoSessionsById.get(relatedSessionId);
          if (relatedVideo != null) {
            JsonElement vid = get(relatedVideo, OutputJsonKeys.VideoLibrary.vid);
            JsonElement title = get(relatedVideo, OutputJsonKeys.VideoLibrary.title);
            if (vid != null && title != null) {
              relatedContentStr.append(vid.getAsString()).append(" ")
                .append(title.getAsString()).append("\n");
            }
          }
        }
        set(new JsonPrimitive(relatedContentStr.toString()),
            dest, OutputJsonKeys.Sessions.relatedContent);
      }
    }
  }

}
