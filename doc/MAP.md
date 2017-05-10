# Map data in iosched

The maps feature in the iosched app uses the Google Maps Android API v2. It is
using data sent from a data source together with the scheduling data. This
allows the map data (ie. names of rooms, location of markers and labels) to be
updated on the fly by updating the remote data and pushing it to the device.

## Maps json data definition

Data for the map is defined in the file bootstrap\_data.json within the
“**map**” tag:

```
"map": [
       {
          "type": "FeatureCollection",
          "features": [
             {
               "type": "Feature",
               "properties": {
                  "id": "room1",
                  "title": "Session 1",
                  "type": "SESSION"
               },
               "geometry": {
                  "type": "Point",
                  "coordinates": [
                    -122.079488,
                    37.426041
                  ]
               }
             },
             {
               "type": "Feature",
               "properties": {
                  "id": "room2",
                  "title": "Session 2",
                  "type": "SESSION"
               },
               "geometry": {
                  "type": "Point",
                  "coordinates": [
                    -122.079000,
                    37.426045
                  ]
               }
             },
             {
               "type": "Feature",
               "properties": {
                  "id": "e32adaab-37e5-e411-b87f-00155d5066d7",
                  "title": "Codelabs",
                  "type": "CODELAB"
               },
               "geometry": {
                  "type": "Point",
                  "coordinates": [
                    -122.079300,
                    37.426450
                  ]
               }
             },
             {
               "type": "Feature",
               "properties": {
                  "id": "9cf51a8f-ace3-e411-b87f-00155d5066d7",
                  "title": "Design Talk",
                  "type": "SANDBOX"
               },
               "geometry": {
                  "type": "Point",
                  "coordinates": [
                    -122.078758,
                    37.427689
                  ]
               }
             },
             {
               "type": "Feature",
               "properties": {
                  "id": "earnengage",
                  "title": "Earn +\nEngage",
                  "description": "Optional Subtitle",
                  "tag": "tag1",
                  "type": "LABEL"
               },
               "geometry": {
                  "type": "Point",
                  "coordinates": [
                    -122.079155,
                    37.427519
                  ]
               }
             },
             {
               "type": "Feature",
               "properties": {
                  "id": "Restroom1",
                  "title": "Restroom",
                  "type": "ICON_RESTROOM"
               },
               "geometry": {
                  "type": "Point",
                  "coordinates": [
                    -122.079754,
                    37.423868
                  ]
               }
             }
             ]
            },
	    {
               "tiles": {
                  "0": {
                      "filename": "floor0-2016-2.svg",
                      "url": ""
                   }
                } 
            }
],
```

### Top level:

*   “**markers**”: GeoJson feature collection containing a list of Points, which
    are represented as markers on a GoogleMap.
*   “**tiles**”: Array of tile files per floor. The SVG file is rendered through
    a tile overlay on top of the map. (The real-world coordinates where the file
    is rendered is currently hardcoded in the app - this will be moved out into
    this file too.) Tiles are also grouped per floor.

*Note: The current implementation does not contain an option to switch floors.*

### Markers

A **marker** is an object that sits *on top of* the map, this includes markers
for rooms and text labels. All markers are represented as geojson which is rendered
through the android maps utility library. Each GeoJSON point has additional properties
aside from their type and geometry.

*   “**id**”: unique identifier for this marker. Id values cannot be repeated.
    If this marker is of the “SESSION” type, the identifier should map to an
    entry in the “rooms” list and associated sessions.
*   “**type**”: the type of the marker. Allowed values are:
    *   `SESSION`: A room that contains sessions.** **When selected, the map
        will display any upcoming sessions on screen. ** For this to work, the
        id must map to the room attribute for session entries and the
        **top-level “rooms” tag. (This means that the “id” of a marker that
        describes a session room must match an entry in “rooms” which lists all
        rooms in which sessions are held.)
    *   `CHAT`: Similar to `SESSION`, but displays the room icon next to each
        session.
    *   `LABEL`: text rendered on top of the map, can contain line breaks (“\n”)
    *   `ICON\_XYZ`: Displays an icon on the map. XYZ refers to a drawable with
        the name “map\_marker\_xyz”. (See MapUtils.java#getDrawableForIconType).
        When clicked, its title and optional description are shown in the bottom bar.
        The icon is not changed when clicked.
    *   `PLAIN`, `OFFICEHOURS`, `MISC`, `SANDBOX`, `CODELAB` : Clickable marker that
        displays its title and an icon when selected. (See
        MapUtils.java#getRoomIcon).
    *   `FIRSTSESSION`: Displays the title and only the description of
        the very first session scheduled in this room.
*   **“title”**: Name of the room displayed on screen when selected. For labels,
    this is displayed directly on the map.
*   **“description”**: Optional. For clickable marker types (e.g. `PLAIN`), this
    string is displayed together with the title when clicked.

### Tiles

Tiles are rendered as overlays on top of the map. The SVG files specified here
are mapped to real world coordinates and automatically rendered and cached on
the map.

*   “**filename**”: Name of the svg file. If this file exists within the
    “assets/maptiles” directory in the app, this file is used. If the file does
    not exist in the assets (and it has not been cached previously), it will be
    downloaded from the url specified in the “url” property. Note that the
    filename is used to identify versions of the map, ie. if the map tiles
    change, the filename must be changed as well to ensure previously cached
    tiles are discarded and the new SVG file is downloaded if necessary.
*   “**url**”: URL to the SVG file. If it has not been preloaded in the
    “assets/maptiles” directory it will be downloaded and cached from this
    location. Note that the filename is used to identify the file once it has
    been loaded.

The SVG overlay is mapped to a location on the map based on “world coordinates”.
These coordinates can be generated through the map tile tool available in
docs/map-tile-coordinates.

#### Updating map tiles

The SVG file must be exported as very basic SVG, because the renderer does not
support all advanced features. Use “SVG 1.1” with in-line display properties
without CSS. It is best to simplify the file as much as possible, using only
basic shapes and simple groupings. Make sure to remove all whitespace around the
edges.

#### Mapping tiles to map coordinates

Export a png file of the tiles and save it into the docs/map-tile-coordinates
directory. Next, edit the docs/map-tile-coordinates/map-coordinates-tool.html to
reference your exported image. Look for `new google.maps.GroundOverlay`. Don’t
forget to add a [Google Maps API
key](https://developers.google.com/maps/documentation/javascript/get-api-key) at
bottom of the file.

Next, open the file in your favourite browser and use the controls to resize and
move the overlay.

The tool displays the *world coordinates* required by iosched. Add these to your
`gradle.properties` file for the `map_floorplan_nw`, `map_floorplan_ne` and
`map_floorplan_se` properties.

## Preparing new map data

The I/O map editor (built from the ‘mapEditor’ gradle build flavor) enables you
to quickly iterate and update on the maps json definition. **Iosched and the map
editor can not be installed side-by-side. You may have to uninstall iosched
first.**

Every time the map editor is launched, bootstrap data is read from external
storage on the device and loaded within the app. Remote data updates are
disabled.

**Note:** The first time you launch the I/O map editor you need to walk through
a few prompts. Make sure to select that you are attending in-person and sign in
with an account. Once signed in, close the app and restart it. You should now
see the map editor.

The workflow might be as follows:

1.  Add all required properties to the `gradle.properties` file, including the
    Google Maps API key. Build and install the I/O map editor.
2.  Copy the file `bootstrap_data.json` to the sdcard/external storage of the
    device, for example through adb: `adb push bootstrap_data.json /sdcard/`.
    Ensure it is still named “boostrap_data.json”
3.  Start the I/O map editor, a few seconds after the map is loaded the map data
    is refreshed with the values loaded from your updated file.
4.  Change the data in the `bootstrap_data.json` file and upload it back to the
    device.
5.  Restart the app, observe the changes.
6.  Repeat 4-5.

**Note that the tiles cannot be updated in the map editor app. **Only the marker
data can be changed by reloading the app. The editor needs to be rebuilt and
reinstalled for new icons and tiles to be loaded.

### Draggable markers

The I/O map editor contains an option to make all markers ‘draggable’, which
means they can be moved around the map.

Toggle the checkbox and long-press on a marker (including labels and other
marker types) to enable dragging.

The new location of the dragged marker is displayed on screen and printed out to
the logcat (run `adb logcat`).

### Adding new locations

Simply add new entries into the “`map` -> `features`” list in the json
data file.

Use a tool like http://http://geojson.io/ to find the geojson representation of a 
marker on the approximate location. Then, add it to the `bootstrap_data.json` file (see above) and
use the ‘draggable’ feature in the map editor to move it to the correct
location. Copy the lat/long values from the logging output (or from the screen)
and update its lat/lng properties in the json file.
