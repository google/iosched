/*
 * Copyright (c) 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.samples.apps.iosched.info.faq;

public class FaqInfo {
    private CharSequence stayInformedDescription;
    private CharSequence contentFormatsDescription;
    private CharSequence liveStreamRecordingsDescription;
    private CharSequence attendanceProTipsDescription;

    public CharSequence getStayInformedDescription() {
        return stayInformedDescription;
    }

    public void setStayInformedDescription(CharSequence stayInformedDescription) {
        this.stayInformedDescription = stayInformedDescription;
    }

    public CharSequence getContentFormatsDescription() {
        return contentFormatsDescription;
    }

    public void setContentFormatsDescription(CharSequence contentFormatsDescription) {
        this.contentFormatsDescription = contentFormatsDescription;
    }

    public CharSequence getLiveStreamRecordingsDescription() {
        return liveStreamRecordingsDescription;
    }

    public void setLiveStreamRecordingsDescription(CharSequence liveStreamRecordingsDescription) {
        this.liveStreamRecordingsDescription = liveStreamRecordingsDescription;
    }

    public CharSequence getAttendanceProTipsDescription() {
        return attendanceProTipsDescription;
    }

    public void setAttendanceProTipsDescription(CharSequence attendanceProTipsDescription) {
        this.attendanceProTipsDescription = attendanceProTipsDescription;
    }
}
