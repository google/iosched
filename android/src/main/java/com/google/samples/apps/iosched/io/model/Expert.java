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

package com.google.samples.apps.iosched.io.model;

import com.google.samples.apps.iosched.util.HashUtils;

public class Expert {
    public String id;
    public String plusId;
    public String bio;
    public String name;
    public String title;
    public String url;
    public String imageUrl;
    public String country;
    public String city;
    public boolean attending;

    public String getImportHashCode() {
        StringBuilder sb = new StringBuilder();
        sb.append("id").append(id == null ? "" : id)
                .append("plusId").append(plusId == null ? "" : plusId)
                .append("bio").append(bio == null ? "" : bio)
                .append("name").append(name == null ? "" : name)
                .append("title").append(title == null ? "" : title)
                .append("url").append(url == null ? "" : url)
                .append("imageUrl").append(imageUrl == null ? "" : imageUrl)
                .append("country").append(country == null ? "" : country)
                .append("city").append(city == null ? "" : city)
                .append("attending").append(attending);
        return HashUtils.computeWeakHash(sb.toString());
    }
}
