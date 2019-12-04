Android Dev Summit app (ADSSched)
======================
The Android Dev Summit app (adssched) is a fork of the Google I/O app (iosched) and was used as the official conference app for the Android Dev Summit in October 2019, held in Sunnyvale, California.

Read IOSched's [README](https://github.com/google/iosched) for a general description of the features and architecture.

# Differences with IOSched
- Added an Instant App flavor.
- Added Notifications.
- No reservations feature. The only user data stored on Firestore is the list of starred sessions and FCM tokens.
- No map tab. Replaced by the Agenda that was moved from the Schedule.


# Copyright

    Copyright 2019 Google Inc. All rights reserved.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
