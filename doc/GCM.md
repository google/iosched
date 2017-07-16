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


# Google Cloud Messaging in IOSched

This app uses Google Cloud Messaging (GCM) to know when to synchronize
the conference data and the user's schedule. For an introduction to GCM,
see [http://developer.android.com/google/gcm/index.html](http://developer.android.com/google/gcm/index.html)

On startup, the application registers the device with the GCM server (see
the MessagingRegistrationWithGCM.registerDevice method). This registration sends two
pieces of data: the device's GCM token and the user's GCM Key. The GCM token is
a core GCM concept, and essentially consists of a long string that
identifies the device for the purposes of sending GCM messages.  The GCM
key, however, is something we use only in this app and is not part of the
normal GCM protocol. More about this soon.

The server can send the client several different types of GCM messages.
When a GCM message arrives, it is processed by GCMMessageListenerService.onMessageReceived.
That method will take the appropriate action depending on the content of
the message. There are several message verbs we recognize:

Message verb | Description
------------ | -----------
test | Prints a message to logcat. Because debugging is fun.
announcement | (deprecated, do not use)
sync_schedule | Causes a sync to happen, which will download new conference data and sync user data. This is normally sent by the server to indicate that there is new conference data available for download.
sync_user | Causes a sync of user data only. This is sent by the server to indicate that the user's data (that is, their set of selected sessions) has been changed by an external agent (for example, the I/O website or another of the user's devices), and therefore needs to be synced.
notification | Shows a notification to the user, optionally with a dialog.  Use sparingly. It can be annoying. See the [Notification command syntax](#notification-command-syntax) section below for more information about the format.

## Notification command syntax

An example of the syntax for the payload in the 'notification' command is
given below.

    {
        "format": "1.0.00",
        "audience": "all",
        "expiry": "2016-07-14T19:40:00Z",
        "title": "Title Goes Here",
        "message": "Message goes here testing 1234 foo bar qux",
        "minVersion": "200",
        "maxVersion": "201",
        "url": "http://www.google.com",
        "dialogTitle": "Dialog Title",
        "dialogText": "Hello! This is a test dialog. Lorem ipsum dolor sit amet."
        "dialogYes" : "Definitely!",
        "dialogNo" : "NO way!"
    }

Field | Required | Description
----- | -------- | -----------
audience | yes | can be "all", "local" (in-person attendees only) or "remote" (remote attendees only)
expiry | yes | indicates when the message expires (if devices get it after that date, they ignore it)
title | yes | title to appear in the notification
message | yes | message to appear in the notification
url | yes | the URL to direct the user to when they click the notification
maxVersion and minVersion | no | allow you to filter what version of the app will receive the notification. Use integer version codes like 200, 201, etc. You can specify only one endpoint (min or max) or both. Both are interpreted as *inclusive*
dialogTitle, dialogText, dialogYes, dialogNo | no | If the dialog fields are present, a dialog will be shown when the notification is clicked. That dialog will have the specified title and message. The message can contain newlines (use actual newlines, not \n), and will automaticaly linkify links. dialogYes can be ommitted for dialogs that do not have a positive action. For example "Dismiss" button only. The positive action (the YES button) will always launch the URL.


## GCM key

The reason we use the concept of a GCM key is that the application has to
synchronize the user's data (sessions selected to be in schedule) with the
Google I/O website.

"GCM key" or "GCM group ID" is not part of the GCM jargon, it's something
specific to this app.

Here is some background around the problem we were trying to solve, so
that you can better understand our design decisions. First of all, there
has to be a common data store that both the app and the website can
access. Furthermore, the website needs to notify the application that the
user's data has changed, triggering the application to do a sync that will
fetch the fresh data. Another factor is that the user might have more than one
Android device, so when they make a modification to their schedule on the
website, the data should immediately update on ALL of their devices.

So these are the problems we had to solve:

1. where to store the data so that both the website and the app could access it
2. how to protect that data so that only the user could access it
3. how to notify all of the user's devices when a change happens on their schedule
4. how to ensure users can't abuse the system sending GCM messages to devices that are not theirs

Items 1) and 2) are solved by using Firebase Realtime Database, where users
could only read-write their own data. For items 3) and 4) we used a randomly
generated UUID, which we call GCM key. Each user has their own unique GCM key.
This key is stored in the Firebase database, along with information about
sessions that were added to a user's schedule, the videos watched by the user,
and information about sessions for which the user provided feedback.

If you look at `AccountUtils`, you will see how this process works.  We first
generate a random key locally; the first time we try to sync with Firebase, we
determine if there's already a key there. If there is, the existing key
overrides the local key.

Note that the app also sends a sync_user message to the server when the user
changes their schedule! This is done to cause a sync on all other
user's devices, so they can reflect that latest change as soon as possible.

## How to send a GCM message

If you've set up your GCM server and enabled GCM on IOSched, you should
be able to push GCM messages to all your users. To do so, you have
to make an HTTP POST request to your GCM server (that is, your
App Engine app that's running the [gcm-server/](../server) code).

Here is a sample POST request:

    POST /send/global/sync_schedule HTTP/1.1
    Host: your-app.appspot.com
    Authorization: key=$ADMINKEY
    Content-Type: application/octet-stream
    Content-Length: 22

    {"sync_jitter":600000}


This would send the "sync_schedule" command to all users.
(replace $ADMINKEY by the admin key you configured in the GCM
server's AuthHelper.java class!)

To make this POST request, you can use one of the many command-line
HTTP utilities such as curl, or use openssl directly. An example:

    $ cat <<END | openssl s_client -connect your-app.appspot.com:443
    POST /send/global/sync_schedule HTTP/1.1
    Host: your-app.appspot.com
    Authorization: key=$ADMINKEY
    Content-Type: application/octet-stream
    Content-Length: 22

    {"sync_jitter":600000}
    END

