/*
 * Copyright 2015 Google Inc. All rights reserved.
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

package com.google.samples.apps.iosched.map.util;


import com.google.android.gms.maps.model.Marker;

/**
 * A structure to store information about a Marker.
 */
public class MarkerModel {

    // Marker types
    public static final int TYPE_INACTIVE = 0;
    public static final int TYPE_SESSION = 1;
    public static final int TYPE_PLAIN = 2;
    public static final int TYPE_LABEL = 3;
    public static final int TYPE_CODELAB = 4;
    public static final int TYPE_SANDBOX = 5;
    public static final int TYPE_OFFICEHOURS = 6;
    public static final int TYPE_MISC = 7;
    public static final int TYPE_VENUE = 8;
    public static final int TYPE_ICON = 9;

    public String id;
    public int floor;
    public int type;
    public String label;
    public Marker marker;

    public MarkerModel(String id, int floor, int type, String label, Marker marker) {
        this.id = id;
        this.floor = floor;
        this.type = type;
        this.label = label;
        this.marker = marker;
    }
}