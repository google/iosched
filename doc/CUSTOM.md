    Copyright 2014 Google Inc. All rights reserved.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

# Customizing IOSched for your own conference

To customize the I/O app for your own conference, you will need to:

1. Set up a server that will host your conference data files.

2. Set up a server to host the session/speaker/etc images (can be the
same as #1)

3. Set up the build and your Google Cloud Console project as described
in [BUILDING.md](BUILDING.md)

4. Decide if you will use a GCM server (optional). If so, you will have to
set it up (see below).

5. Edit Config.java settings as appropriate for your conference

6. Generate bootstrap JSON data (which should be a recent snapshot of the
conference data JSON files in your server) and save it to
res/raw/bootstrap_data.json. More information about format in
[SYNC.md](SYNC.md)

7. Modify the app to add your own icons, colors, conference name, etc.

Here are a few more details about each step.

## Hosting the conference data files

These files are in the IOSched JSON format (documented in [SYNC.md](SYNC.md))
and describe the sessions, speakers, rooms, etc, for your conference.
These files can be hosted on any reliable hosting service where the files
can be accessed by plain HTTP. As described in [SYNC.md](SYNC.md), there is a
significant performance gain if the server correctly handles the
If-Modified-Since header. As a suggestion, you can try storing your
files in Google Cloud Storage.

During development, you can host these files in a test HTTP server, or
even on someone's local machine. For production, of course, you will
want a robust server that can handle the QPS you expect to get during
the conference.

## Hosting the images

In IOSched, sessions and speakers can have images to illustrate them,
and these images must be hosted somewhere. This can be the same server
as the one that's serving the conference data, or a different one, at
your choice. You can obtain a significant performance gain if you store
your image files using the URL format described in [IMAGES.md](IMAGES.md),
since this will allow the app to download images at the appropriate
resolution as needed.

## Set up Config.java
The Config.java file is located at:

   [android/src/main/java/com/google/samples/apps/iosched/Config.java](../android/src/main/java/com/google/samples/apps/iosched/Config.java)

This file controls most of the event-specific aspects of IOSched.
Make sure to edit this file to reflect the configuration you need
for your event. Read the comments on the file for details about
what each configuration parameter means.

## GCM setup

To set up the GCM server (optional), you can start with the code 
available in [gcm](../server/src/main/java/com/google/samples/apps/iosched/server/gcm). In particular:

1. Make up your own keys in AuthHelper.java. There are keys that
   allow registration and message sending.

2. Host the GCM server on App Engine

3. Enter the GCM configuration parameters in the client app's
   Config.java (GCM_* constants).

Using a GCM server means you, as the admin, can push GCM messages
to all your users. Read [GCM.md](GCM.md) for more information about the
syntax of messages and how to send them.

Remember that each GCM message has a cost in terms of battery life and
network usage for your user. You don't want all your conference
attendees to run out of battery midway through the conference because
you were sending GCM messages to them every 10 seconds!

Also, IOSched allows you to pop up a notification or dialog box with a
GCM message. You should also use that feature sparingly while running
your conference. Excessive notifications are a bad user experience!

