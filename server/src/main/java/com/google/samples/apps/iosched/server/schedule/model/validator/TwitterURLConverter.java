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
package com.google.samples.apps.iosched.server.schedule.model.validator;

import com.google.gson.JsonPrimitive;

import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cleans up user input of a Twitter URL inside a JSON object.
 * Output is a working URL of the form http(s)://twitter.com/username
 */
public class TwitterURLConverter extends Converter {

    private static Pattern[] twitterRecognizedPatterns = {
            Pattern.compile("@\\w+")  // @twitterhandle
    };

    private static final Pattern acceptableUrlPattern = Pattern.compile("https?:\\/\\/.+");

    /**
     * Desired output format
     */
    private static final MessageFormat twitterFormat = new MessageFormat("https://twitter.com/{0}");

    /**
     * Takes a user inputted twitter profile as a JsonPrimitive and returns a properly
     * formatted/cleaned URL to that twitter profile.
     */
    @Override
    public JsonPrimitive convert(JsonPrimitive value) {
        if (value == null) {
            return null;
        }
        String str = value.getAsString();
        if (str.isEmpty()) {
            return value;
        }

        // If they didn't enter it as a URL, format it as one.
        for (Pattern p: twitterRecognizedPatterns) {
            Matcher m = p.matcher(str);
            if (m.find()) {
                return new JsonPrimitive(twitterFormat.format(new String[]{m.group(0)}));
            }
        }

        // If URL starts with http/https:
        if (acceptableUrlPattern.matcher(str).matches()) {
            return value;
        }

        // Otherwise, just add https://:
        str = "https://" + str;
        return new JsonPrimitive(str);
    }
}
