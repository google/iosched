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

# Sync protocol and data format

We introduced a major change in how conference data is synced from the
server: instead of making calls to a specific backend, IOSched now simply
loads data in JSON format from a URL specified in the configuration file.

The sync process starts by making an HTTP GET request to load the manifest
file. The URL is configured in the `MANIFEST_URL` constant in
[Config.java](../android/src/main/java/com/google/samples/apps/iosched/Config.java).

For Google I/O, we used Google Cloud Storage as the hosting service
to host these JSON files. However, if you are running your own event,
you don't necessarily need to use Google Cloud Storage. Any reliable
hosting service should work.

IOSched sends an `If-Modified-Since` HTTP header in the request for
the manifest file, specifying the timestamp of the file as it
was when it last loaded it. If the server replies with HTTP status code
304 (Not Modified), IOSched deems the sync to be complete without
further action, since there was no change to the manifest. If the server
replies with a 200 OK, then the manifest data is parsed. Here is
an example of the manifest file body.

```JSON
{
    "format": "iosched-json-v1",
    "data_files": [
        "past_io_videolibrary_v5.json",
        "experts_v11.json",
        "hashtags_v8.json",
        "blocks_v10.json",
        "map_v11.json",
        "keynote_v10.json",
        "session_data_v2.681.json"
    ]
}
```

Note that each of the entries in the `data_files` array is actually
a pointer to another JSON file, which in turn contains information
about each type of entity. There is nothing special about the names
of the files. They can be any valid file name. We chose to use a
versioning scheme (the `_v<N>.json` suffix) for organization purposes.

> NOTE: these files are cached **by name** on the app. So if the app has
already downloaded `keynote_v10.json` at some point in the past, it
will not download it again. Therefore, if you are using IOSched for your
own event, every time you modify a file in production, make sure to also
change its name. For example, rename it to `keynote_v11.json` (and
update your manifest). In other words, **once published, each file should be
seen as immutable**.

So the general procedure to update one or more files in production is:

1. download foo_v1.json, make your changes, upload foo_v2.json
2. download bar_v7.json, make your changes, upload bar_v8.json
3. download manifest.json, change it to point to foo_v2 and bar_v8, upload it.

> Notice the order: you should always update the manifest file **LAST**,
because when you update it, the files that it refers to should be
available to clients.

The format of each JSON file is:

```JSON
    {
       "<entity_collection>": [
           <entity>,
           <entity>,
           <entity>
       ],
       "<entity_collection>": [
           <entity>,
           <entity>,
           <entity>
       ]
    }
```

That is, each file consists of one or more collections of entities,
organized by type. Example entity collections are: sessions, speakers,
rooms, etc.

When you are writing these files, the exact layout of which collections go
in which files is entirely up to you. For example, in our case we put the
entities for sessions, speakers and rooms all together in
session_data_vN.json, but we might as well have separated it into three
separate JSON files.

Here is an example of a file with a room, a session and a speaker:

```JSON
{
  "rooms": [
    {
        "id": "ROOM1",
        "name": "Room Alpha"
    }
  ],
  "sessions": [
    {
        "id": "SESSION1"
        "description": "A cool session about example data.",
        "title": "Example Data in Action",
        "url": "http://www.example.com",
        "tags": [
            "TYPE_SESSION",
            "TOPIC_ANDROID",
            "TOPIC_CHROME",
            "THEME_DEVELOP",
            "THEME_DESIGN"
        ]
        "startTimestamp": "2014-06-26T22:00:00Z",
        "endTimestamp": "2014-06-26T22:30:00Z",
        "youtubeUrl": "dQw4w9WgXcQ",
        "speakers": [
            "SPEAKER1"
        ],
        "room": "ROOM1",
        "isLivestream": true,
        "captionsUrl": "http://......"
    }
  ],
  "speakers": [
    {
        "id": "SPEAKER1",
        "name": "Example Smith",
        "bio": "Mr. Example Smith is a great speaker.",
        "plusoneUrl":  
            "https://plus.google.com/12345677890123456789012",
        "thumbnailUrl":
            "https://example.com/..."
    }
  ]
}
```

Notice that all three collections ("rooms", "speakers" and "sessions")
are in a single file, but it be would equally valid to have them in
different files.

> **IMPORTANT**: if more than one file specifies the same entity collection,
IOSched considers the UNION of all entities in those collections.
This means that you can have multiple files that specify sessions,
and all of them will be processed to form a single collection of
sessions.


## Bootstrap data

When the user runs the app for the first time, they expect to see
data. However, if we relied only on the sync mechanism to bring data
into the app, a first-time user would stare at a blank screen while
waiting for a sync to happen, and this would be a bad user experience.

This is why IOSched ships with preloaded "bootstrap data", which
is essentially a preloaded offline snapshot of the JSON data. This
data is parsed by the app and saved to the database on first execution.

You can find this file in [res/raw/bootstrap.json](../android/src/main/res/raw).
It is simply a text file with a combined snapshot of the JSON files on the server.


## Data format

Below is the documentation about the format of each type of entity
supported by IOSched.

### Rooms

Rooms are places where sessions can happen.

```JSON
{
  "rooms": [
    <room>,
    <room>,
    <room>,
    ...
  ]
}
```
Where each `<room>` has this format: 
```JSON
{
    "id": "ROOM1",
    "name": "Room Alpha"
}
```

### Blocks

Blocks are intervals of time in which sessions or other activities
of interest can happen. Blocks are used EXCLUSIVELY in the My 
Schedule screen to show the user what are the major time blocks
of the event. For example, breakfast and lunch are blocks.

```JSON
{
  "blocks": [
    <block>,
    <block>,
    <block>,
    ...
  ]
}
```

Example `<block>` that represents a break/meal:

```JSON
{
    "title": "Lunch",
    "subtitle": "Cafe, Level 1", 
    "type": "break", 
    "start": "2014-06-25T18:30:00.000Z",
    "end": "2014-06-25T20:00:00.000Z"
}
```

Example `<block>` that represents a `free` block:

```JSON
{
    "title": "",
    "type": "free",
    "subtitle": "",
    "start": "2014-06-25T18:00:00.000Z",
    "end": "2014-06-25T19:00:00.000Z"
}
```

This type of block is of type `free` because it shows as a free
block in the app's screen My Schedule, and allows the user to click on it
and view sessions that starts on the block's interval. Every session start time
should lie in an interval where there is at least one free block, otherwise
the user won't be able to add them to their schedule from the My Schedule screen.
Overlapping `free` blocks are properly handled in the Android app.

### Sessions

```JSON
{
  "sessions": [
    <session>,
    <session>,
    <session>,
    ...
  ]
}
```
Where each `<session>` has this format:

```JSON
{
   "id": "SESSION123"
   "url": "https://...."
   "title": "Web Components in Action",
   "description": "Web components are cool.",
   "tags": [
       "TYPE_SESSION",
       "TOPIC_ANDROID",
       "TOPIC_CHROME",
       "THEME_DEVELOP",
       "THEME_DESIGN"
   ]
   "mainTag": "TOPIC_ANDROID",
   "startTimestamp": "2014-06-25T22:10:00Z",
   "endTimestamp": "2014-06-25T22:55:00Z"
   "photoUrl": "https://...../photo.jpg",
   "youtubeUrl": "https://youtu.be/YCUZ01yFtsM",
   "speakers": [
       "SPEAKER123",
       "SPEAKER456"
   ],
   "room": "ROOM123",
   "isLivestream": true,
   "captionsUrl": "http://......",
   "color": "#607d8b",
   "hashtag": "webcomponents"
}
```

The session URL is the URL of the session on the web. This is also
used as the +1 URL when the user +1's the session.  The session tags are
not arbitrary, they have to be defined in the "tags" collection. The
start end end timestamps are always given in this format, in the UTC
timezone. The photo URL is the URL of the photo that illustrates
the session. The captions URL is the URL of a page to show the
livestream captions for this session. Set to "" if captions are not
available. Color is branding color that shows in the session
details screen, and the hashtag is the Google+ hashtag that gets
automatically added when the user clicks the "Social" button on the
session details screen.

If isLivestream is set to true, this session is livestreamed, and
the youtubeUrl indicates the Youtube livestream URL to view it.
If the session is not livestreamed, then youtubeUrl indicates
the URL of the video recording of the session, if one is available.

    
### Speakers

A speaker is a presenter of a session. Each session can have
zero or more presenters. Sessions with at least one presenter
are more fun.

```JSON
{
  "speakers": [
    <speaker>,
    <speaker>,
    <speaker>,
    ...
  ]
}
```

Example of speaker:

```JSON
{
    "id": "SPEAKER123",
    "name": "Reto Meier",
    "bio": "Reto is the lead of the Scalable Developer Advocacy team",
    "company": "Google",
    "thumbnailUrl": "http://..../reto.jpg",
    "plusoneUrl": "https://plus.google.com/+RetoMeier"
}
```

### Tags

Tags are what you expect tags to be. They help classify sessions.
In IOSched, a session is tagged by TOPIC, THEME and TYPE.
Examples of TOPIC tags: "Android", "Chrome", etc. Examples of
THEME tags: "Design", "Develop", "Distribute". And some examples
of TYPE tags are: "Session", "Codelab", "Office hours", etc. 
The "tags" entity in the data specifies all the possible tags and the
categories they belong to.

```JSON
{
  "tags": [
    <tag>,
    <tag>,
    <tag>,
    ...
  ]
}
```

Example of tag representing the "Android" topic:

```JSON
{
    "category": "TOPIC",
    "tag": "TOPIC_ANDROID",
    "name": "Android",
    "abstract": "",
    "order_in_category": 1,
    "color": "#558b2f"
}
```

Notice that we indicate that this tag belongs to the "TOPIC"
category, which is to say this tag represents the topic of a session.

Example of tag representing the "Develop" theme:

```JSON
{
    "category": "THEME",
    "tag": "THEME_DEVELOP",
    "name": "Distribute",
    "abstract": "",
    "order_in_category": 2
}
```

And finally, here is an example of the tag that represents the
"Office Hours" session type:

```JSON
{
    "category": "TYPE",
    "tag": "TYPE_OFFICEHOURS",
    "name": "Office Hours",
    "abstract": "",
    "order_in_category": 6
}
```

The *order_in_category* parameter is used when showing lists of tags,
and it indicates the relative ordering of tags in the same category.
So when we are showing a list of session types, "Office Hours" will
appear after any tag with *order_in_category* lower than 6, and before
any other tag with *order_in_category* bigger than 6.


### Experts

Experts are the Google Developer Experts that appear in the 
"Experts" screen of the app.

```JSON
{
  "experts": [
    <expert>,
    <expert>,
    <expert>,
    ...
  ]
}
```

Example of expert:

```JSON
{
    "id": "EXPERT123",
    "name": "Bruno Oliveira", 
    "attending": true, 
    "bio": "Lorem ipsum dolor sit amet", 
    "city": "Sao Paulo, SP", 
    "country": "Brazil", 
    "imageUrl": "https://....../photo.jpg",
    "plusId": "+BrunoOliveira",
    "url": "https://plus.google.com/+BrunoOliveira"
} 
```

### Hashtags

Hashtags are the hashtags that appear in the "Social" screen.

```JSON
{
  "hashtags": [
    <hashtag>,
    <hashtag>,
    <hashtag>,
    ....
  ]
}
```

Example hashtag:

```JSON
{
    "color": "#ff8a65",
    "description": "Experience the magic of I/O remotely",
    "name": "#io14extended",
    "order": 11
}
```

### Videos

Videos are links to Youtube content that appear in the Video Library
screen.

```JSON
{
  "video_library": [
    <video>,
    <video>,
    <video>,
    ...
  ]
}
```
Example video:

```JSON
{
    "year": "2013",
    "title": "What's New in Android Developer Tools",
    "desc": "A summary of new features for Android developers",
    "vid": "lmv1dTnhLH4",
    "id": "lmv1dTnhLH4",
    "thumbnailUrl": "http://img.youtube.com/vi/lmv1dTnhLH4/hqdefault.jpg",
    "topic": "Android",
    "speakers": "Xavier Ducrohet, Tor Norbye"
}
```
`vid` is the Youtube video ID of this video.

> NOTE: due to a bug in the Android app, "id" must always be set to the same as "vid".

### Map

The map data is different from the others because it's not a
collection of entities. Rather, it is a single JSON object
formatted as follows:

```JSON
{
  "map": [
  {
     "config": {
         "enableMyLocation": "false"
     }, 
     "markers": {
         "0": [
             {
                  "id": "ROOM8", 
                  "lat": 37.78280631538293, 
                  "lng": -122.40401487797499, 
                  "title": "Room 8", 
                  "type": "SESSION"
             }, 
             ....
         ],
         "1": [
             //...more markers...
         ],
         "2": [
             //...more markers...
         ],
     },
     "tiles": {
         "0": {
             "filename": "floor2-2015-v1.svg",
             "url": ""
         }, 
         "1": {
             "filename": "floor1-2015-v1.svg",
             "url": ""
         }, 
         "2": {
             "filename": "floor0-2015-v1.svg",
             "url": ""
         }
     }
  }
  ]
}
```

`enableMyLocation` indicates whether indoor location should be enabled
in the indoor map. Each marker in the `markers` array represents
one of the markers on the map, with ID, latitute, longitude,
title and type. Markers are organized per floor (hence the 
"0", "1" and "2" keys).

To indicate the location of a room, use this marker format:

```JSON
{
     "id": "ROOM8", 
     "lat": 37.78280631538293, 
     "lng": -122.40401487797499, 
     "title": "Room 8", 
     "type": "SESSION"
}
```

To put a label on the map without making it a marker, simply
set the "type" to "LABEL":

```JSON
{
    "id": "gearpickup", 
    "lat": 37.78331825838168, 
    "lng": -122.40340635180475, 
    "title": "Gear Pickup", 
    "type": "label"
}
```

Other supported types are "PLAIN" and "MISC", which differ by a special icon in its info display
(but do not display a list of scheduled sessions).
"SESSION" rooms include a list of its scheduled sessions (without their icons).
"SANDBOX" rooms include a list of its scheduled sessions (including their icons).

The "tiles" dictionary indicate what SVG file to use as the map
overlay for each floor. The optional "url" parameter indicates a
location where to download the file from, in case the file is not pre-loaded
on the app (as is the case in the example).



