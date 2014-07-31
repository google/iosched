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

public class Speaker {
    public String id;
    public String publicPlusId;
    public String bio;
    public String name;
    public String company;
    public String plusoneUrl;
    public String thumbnailUrl;

    public String getImportHashcode() {
        StringBuilder sb = new StringBuilder();
        sb.append("id").append(id == null ? "" : id)
                .append("publicPlusId").append(publicPlusId == null ? "" : publicPlusId)
                .append("bio").append(bio == null ? "" : bio)
                .append("name").append(name == null ? "" : name)
                .append("company").append(company== null ? "" : company)
                .append("plusoneUrl").append(plusoneUrl == null ? "" : plusoneUrl)
                .append("thumbnailUrl").append(thumbnailUrl == null ? "" : thumbnailUrl);
        return HashUtils.computeWeakHash(sb.toString());
    }
}


