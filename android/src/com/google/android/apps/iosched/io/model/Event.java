/*
 * Copyright 2012 Google Inc.
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

package com.google.android.apps.iosched.io.model;

import com.google.gson.annotations.SerializedName;

public class Event {

    public String room;
    public String end_date;
    public String level;
    public String[] track;
    public String start_time;
    public String title;
    @SerializedName("abstract")
    public String _abstract;
    public String start_date;
    public String attending;
    public String has_streaming;
    public String end_time;
    public String livestream_url;
    public String[] youtube_url;
    public String id;
    public String tags;
    public String[] speaker_id;
    public String[] prereq;
}
