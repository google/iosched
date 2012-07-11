/**
 * @license
 *
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @fileoverview SmartMarker.
 *
 * @author Chris Broadfoot (cbro@google.com)
 */

/**
 * A google.maps.Marker that has some smarts about the zoom levels it should be
 * shown.
 *
 * Options are the same as google.maps.Marker, with the addition of minZoom and
 * maxZoom. These zoom levels are inclusive. That is, a SmartMarker with
 * a minZoom and maxZoom of 13 will only be shown at zoom level 13.
 * @constructor
 * @extends google.maps.Marker
 * @param {Object=} opts  Same as MarkerOptions, plus minZoom and maxZoom.
 */
function SmartMarker(opts) {
  var marker = new google.maps.Marker;

  // default min/max Zoom - shows the marker all the time.
  marker.setValues({
    'minZoom': 0,
    'maxZoom': Infinity
  });

  // the current listener (if any), triggered on map zoom_changed
  var mapZoomListener;
  google.maps.event.addListener(marker, 'map_changed', function() {
    if (mapZoomListener) {
      google.maps.event.removeListener(mapZoomListener);
    }
    var map = marker.getMap();
    if (map) {
      var listener = SmartMarker.newZoomListener_(marker);
      mapZoomListener = google.maps.event.addListener(map, 'zoom_changed',
          listener);

      // Call the listener straight away. The map may already be initialized,
      // so it will take user input for zoom_changed to be fired.
      listener();
    }
  });
  marker.setValues(opts);
  return marker;
}
window['SmartMarker'] = SmartMarker;

/**
 * Creates a new listener to be triggered on 'zoom_changed' event.
 * Hides and shows the target Marker based on the map's zoom level.
 * @param {google.maps.Marker} marker  The target marker.
 * @return Function
 */
SmartMarker.newZoomListener_ = function(marker) {
  var map = marker.getMap();
  return function() {
    var zoom = map.getZoom();
    var minZoom = Number(marker.get('minZoom'));
    var maxZoom = Number(marker.get('maxZoom'));
    marker.setVisible(zoom >= minZoom && zoom <= maxZoom);
  };
};
