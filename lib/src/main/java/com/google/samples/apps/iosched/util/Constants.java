/*
 * Copyright 2017 Google Inc. All rights reserved.
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

package com.google.samples.apps.iosched.util;

/**
 * Constants used by util files.
 */
public class Constants {
    /**
     * This is changed each year to effectively reset certain preferences that should be re-asked
     * each year. Note, res/xml/settings_prefs.xml must be updated when this value is updated.
     */
    public static final String CONFERENCE_YEAR_PREF_POSTFIX = "_2017";
}
