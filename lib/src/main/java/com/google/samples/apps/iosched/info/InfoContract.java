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
package com.google.samples.apps.iosched.info;

import com.google.samples.apps.iosched.info.about.AboutInfo;
import com.google.samples.apps.iosched.info.event.EventInfo;
import com.google.samples.apps.iosched.info.travel.TravelInfo;

public class InfoContract {

    interface View {
        void setPresenter(InfoContract.Presenter presenter);
        void showEventInfo(EventInfo eventInfo);
        void showTravelInfo(TravelInfo travelInfo);
        void showAboutInfo(AboutInfo aboutInfo);
    }

    interface Presenter {
        void initEventInfo();

        void initTravelInfo();

        void initAboutInfo();
    }
}

