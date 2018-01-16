/*
 * Copyright 2018 Google Inc. All rights reserved.
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

package iosched.apps.samples.google.com.shared.model;

/**
 * Describes a speaker at the conference.
 */
interface Speaker {
    /**
     * Unique string identifying this speaker.
     */
    String getId();

    /**
     * Name of this speaker.
     */
    String getName();

    /**
     * Profile photo of this speaker.
     */
    String getImageUrl();

    /**
     * Company this speaker works for.
     */
    String getCompany();

    /**
     * Text describing this speaker in detail.
     */
    String getAbstract();

    /**
     * Full URL of the speaker's G+ profile.
     */
    String getGPlusUrl();

    /**
     * Full URL of the speaker's Twitter profile.
     */
    String getTwitterUrl();
}
