#!/bin/sh

#
# Copyright 2013 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

#
# This script will launch the Session Livestream Activity with a video playing
# and some placeholder text for session title and session summary. This is
# useful for quickly testing the activity as navigating there via the UI can
# be a bit tricky as the livestream activity is only reachable when a session
# is currently "live" (can be simulated by setting device time or using the
# set_moack_time.sh script).
#
if [[ -z $ADB ]]; then ADB=adb; fi
$ADB shell am start -n com.google.android.apps.iosched/.ui.SessionLivestreamActivity -e com.google.android.iosched.extra.youtube_url 9bZkp7q19f0 -e com.google.android.iosched.extra.title "Session Title" -e com.google.android.iosched.extra.abstract "Session summary goes here." 