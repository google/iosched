# Sample App Data Web App

This folder contains a sample of a simple website that reads and writes user data
to cloud using Google Drive JavaScript SDK. It's an example for the developers of the Google I/O
website. Below is the authoritative definition of the format definition of the User Data stored in
app data and a description of flow.

## Data format

The IDs of starred sessions, viewed videos and sessions that have had feedback submitted will be
synced by the JavaScript library and the Android app in a
[Google Drive AppData](https://developers.google.com/drive/web/appdata) folder shared with the web
and the Android app.

The data is saved to a file named `user_data.json` that contains JSON with the following attributes:

* Attribute name: `gcm_key`
  - Format: String
  - Optional for Web clients / Mandatory for Android app
  - Description: The Google Cloud Messaging authorization key. This attribute is added when the file is first read or created by an Android apps.
* Attribute name: `feedback_submitted_sessions`
  - Format: Array of String
  - Optional
  - Description: Contains the list of the IDs of the sessions for which feedback have been submitted.
* Attribute name: `starred_sessions`
  - Format: Array of String
  - Optional
  - Description:  Contains the list of the IDs of the sessions that have been starred.
* Attribute name: `viewed_videos`
  - Format: Array of String
  - Optional
  - Description: Contains the list of the IDs of the videos that have been already viewed by the user.

Here is a full example of data in `user_data.json`:

    {
        "gcm_key": "12345sdfsdf",
        "feedback_submitted_sessions": [
            "sessionid1", "sessionid2", "sessionid4"...
        ],
        "starred_sessions": [
            "sessionid1", "sessionid2", "sessionid3"...
        ],
        "viewed_videos": [
            "videoid1", "videoid2", "videoid3"...
        ]
    }

Some JSON attributes are optional. Therefore this is also a correct `user_data.json` file:

    {
        "gcm_key": "12345sdfsdf",
        "viewed_videos": [
            "videoid1", "videoid2", "videoid3"...
        ]
    }

The file should not be blank though as it should be at least a JSON Object with a `gcm_key`
attribute and value.


## Logic / Flow


Whenever the AppData file is updated, the updater must send a Google Cloud Messaging notifications
using the key (`gcm_key`) provided in the file and to the following URL:
`https://iosched-gcm-dev.appspot.com/send/self/sync_user`
The only exception is for Web client: If the `gcm_key` is not in the file then this means that there are
no Android apps to notify so pushing to GCM is skipped.
Android apps must create a `gcm_key` and add it to the AppData file if there is none.
