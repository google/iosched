// Copyright 2011 Google

/**
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
 * The Google IO Map
 * @constructor
 */
var IoMap = function() {
  var moscone = new google.maps.LatLng(37.78313383211993, -122.40394949913025);

  /** @type {Node} */
  this.mapDiv_ = document.getElementById(this.MAP_ID);

 var ioStyle = [
  {
    'featureType': 'road',
    stylers: [
      { hue: '#00aaff' },
      { gamma: 1.67 },
      { saturation: -24 },
      { lightness: -38 }
    ]
  },{
    'featureType': 'road',
    'elementType': 'labels',
    stylers: [
      { invert_lightness: true }
    ]
  }];

  /** @type {boolean} */
  this.ready_ = false;

  /** @type {google.maps.Map} */
  this.map_ = new google.maps.Map(this.mapDiv_, {
    zoom: 18,
    center: moscone,
    navigationControl: true,
    mapTypeControl: false,
    scaleControl: true,
    mapTypeId: 'io',
    streetViewControl: false
  });

  var style = /** @type {*} */(new google.maps.StyledMapType(ioStyle));
  this.map_.mapTypes.set('io', /** @type {google.maps.MapType} */(style));

  google.maps.event.addListenerOnce(this.map_, 'tilesloaded', function() {
    if (window['MAP_CONTAINER'] !== undefined) {
      window['MAP_CONTAINER']['onMapReady']();
    }
  });

  /** @type {Array.<Floor>} */
  this.floors_ = [];
  for (var i = 0; i < this.LEVELS_.length; i++) {
    this.floors_.push(new Floor(this.map_));
  }

  this.addLevelControl_();
  this.addMapOverlay_();
  this.loadMapContent_();

  this.initLocationHashWatcher_();

  if (!document.location.hash) {
    this.showLevel(1, true);
  }
}

IoMap.prototype = new google.maps.MVCObject;

/**
 * The id of the Element to add the map to.
 *
 * @type {string}
 * @const
 */
IoMap.prototype.MAP_ID = 'map-canvas';


/**
 * The levels of the Moscone Center.
 *
 * @type {Array.<string>}
 * @private
 */
IoMap.prototype.LEVELS_ = ['1', '2', '3'];

/**
 * Location where the tiles are hosted.
 *
 * @type {string}
 * @private
 */
IoMap.prototype.BASE_TILE_URL_ =
  'http://www.gstatic.com/io2010maps/tiles/5/';

/**
 * The minimum zoom level to show the overlay.
 *
 * @type {number}
 * @private
 */
IoMap.prototype.MIN_ZOOM_ = 16;


/**
 * The maximum zoom level to show the overlay.
 *
 * @type {number}
 * @private
 */
IoMap.prototype.MAX_ZOOM_ = 20;

/**
 * The template for loading tiles. Replace {L} with the level, {Z} with the
 * zoom level, {X} and {Y} with respective tile coordinates.
 *
 * @type {string}
 * @private
 */
IoMap.prototype.TILE_TEMPLATE_URL_ = IoMap.prototype.BASE_TILE_URL_ +
    'L{L}_{Z}_{X}_{Y}.png';

/**
 * @type {string}
 * @private
 */
IoMap.prototype.SIMPLE_TILE_TEMPLATE_URL_ =
    IoMap.prototype.BASE_TILE_URL_ + '{Z}_{X}_{Y}.png';

/**
 * The extent of the overlay at certain zoom levels.
 *
 * @type {Object.<string, Array.<Array.<number>>>}
 * @private
 */
IoMap.prototype.RESOLUTION_BOUNDS_ = {
  16: [[10484, 10485], [25328, 25329]],
  17: [[20969, 20970], [50657, 50658]],
  18: [[41939, 41940], [101315, 101317]],
  19: [[83878, 83881], [202631, 202634]],
  20: [[167757, 167763], [405263, 405269]]
};

/**
 * The previous hash to compare against.
 *
 * @type {string?}
 * @private
 */
IoMap.prototype.prevHash_ = null;

/**
 * Initialise the location hash watcher.
 *
 * @private
 */
IoMap.prototype.initLocationHashWatcher_ = function() {
  var that = this;
  if ('onhashchange' in window) {
    window.addEventListener('hashchange', function() {
      that.parseHash_();
    }, true);
  } else {
    var that = this
    window.setInterval(function() {
      that.parseHash_();
    }, 100);
  }

  this.parseHash_();
};

/**
 * Called from Android.
 *
 * @param {Number} x A percentage to pan left by.
 */
IoMap.prototype.panLeft = function(x) {
  var div = this.map_.getDiv();
  var left = div.clientWidth * x;
  this.map_.panBy(left, 0);
};
IoMap.prototype['panLeft'] = IoMap.prototype.panLeft;


/**
 * Adds the level switcher to the top left of the map.
 *
 * @private
 */
IoMap.prototype.addLevelControl_ = function() {
  var control = new LevelControl(this, this.LEVELS_).getElement();

  this.map_.controls[google.maps.ControlPosition.TOP_LEFT].push(control);
};

/**
 * Shows a floor based on the content of location.hash.
 *
 * @private
 */
IoMap.prototype.parseHash_ = function() {
  var hash = document.location.hash;

  if (hash == this.prevHash_) {
    return;
  }

  this.prevHash_ = hash;

  var level = 1;

  if (hash) {
    var match = hash.match(/level(\d)(?:\:([\w-]+))?/);
    if (match && match[1]) {
      level = parseInt(match[1], 10);
    }
  }

  this.showLevel(level, true);
};

/**
 * Updates location.hash based on the currently shown floor.
 *
 * @param {string?} opt_hash
 */
IoMap.prototype.setHash = function(opt_hash) {
  var hash = document.location.hash.substring(1);

  if (hash == opt_hash) {
    return;
  }

  if (opt_hash) {
    document.location.hash = opt_hash;
  } else {
    document.location.hash = 'level' + this.get('level');
  }
};
IoMap.prototype['setHash'] = IoMap.prototype.setHash;

/**
 * Called from spreadsheets.
 */
IoMap.prototype.loadSandboxCallback = function(json) {
  var updated = json['feed']['updated']['$t'];

  var contentItems = [];
  var ids = {};

  var entries = json['feed']['entry'];
  for (var i = 0, entry; entry = entries[i]; i++) {
    var item = {};

    item.companyName = entry['gsx$companyname']['$t'];
    item.companyUrl = entry['gsx$companyurl']['$t'];
    var p = entry['gsx$companypod']['$t'];
    item.pod = p;
    p = p.toLowerCase().replace(/\s+/, '');
    item.sessionRoom = p;

    contentItems.push(item);
  };

  this.sandboxItems_ = contentItems;

  this.ready_ = true;

  this.addMapContent_();
};

/**
 * Called from spreadsheets.
 *
 * @param {Object} json The json feed from the spreadsheet.
 */
IoMap.prototype.loadSessionsCallback = function(json) {
  var updated = json['feed']['updated']['$t'];

  var contentItems = [];
  var ids = {};

  var entries = json['feed']['entry'];
  for (var i = 0, entry; entry = entries[i]; i++) {
    var item = {};

    item.sessionDate = entry['gsx$sessiondate']['$t'];
    item.sessionAbstract = entry['gsx$sessionabstract']['$t'];
    item.sessionHashtag = entry['gsx$sessionhashtag']['$t'];
    item.sessionLevel = entry['gsx$sessionlevel']['$t'];
    item.sessionTitle = entry['gsx$sessiontitle']['$t'];
    item.sessionTrack = entry['gsx$sessiontrack']['$t'];
    item.sessionUrl = entry['gsx$sessionurl']['$t'];
    item.sessionYoutubeUrl = entry['gsx$sessionyoutubeurl']['$t'];
    item.sessionTime = entry['gsx$sessiontime']['$t'];
    item.sessionRoom = entry['gsx$sessionroom']['$t'];
    item.sessionTags = entry['gsx$sessiontags']['$t'];
    item.sessionSpeakers = entry['gsx$sessionspeakers']['$t'];

    if (item.sessionDate.indexOf('10') != -1) {
      item.sessionDay = 10;
    } else {
      item.sessionDay = 11;
    }

    var timeParts = item.sessionTime.split('-');
    item.sessionStart = this.convertTo24Hour_(timeParts[0]);
    item.sessionEnd = this.convertTo24Hour_(timeParts[1]);

    contentItems.push(item);
  }

  this.sessionItems_ = contentItems;
};

/**
 * Converts the time in the spread sheet to 24 hour time.
 *
 * @param {string} time The time like 10:42am.
 */
IoMap.prototype.convertTo24Hour_ = function(time) {
  var pm = time.indexOf('pm') != -1;

  time = time.replace(/[am|pm]/ig, '');
  if (pm) {
    var bits = time.split(':');
    var hr = parseInt(bits[0], 10);
    if (hr < 12) {
      time = (hr + 12) + ':' + bits[1];
    }
  }

  return time;
};

/**
 * Loads the map content from Google Spreadsheets.
 *
 * @private
 */
IoMap.prototype.loadMapContent_ = function() {
  // Initiate a JSONP request.
  var that = this;

  // Add a exposed call back function
  window['loadSessionsCallback'] = function(json) {
    that.loadSessionsCallback(json);
  }

  // Add a exposed call back function
  window['loadSandboxCallback'] = function(json) {
    that.loadSandboxCallback(json);
  }

  var key = 'tmaLiaNqIWYYtuuhmIyG0uQ';
  var worksheetIDs = {
    sessions: 'od6',
    sandbox: 'od4'
  };

  var jsonpUrl = 'http://spreadsheets.google.com/feeds/list/' +
      key + '/' + worksheetIDs.sessions + '/public/values' +
      '?alt=json-in-script&callback=loadSessionsCallback';
  var script = document.createElement('script');
  script.setAttribute('src', jsonpUrl);
  script.setAttribute('type', 'text/javascript');
  document.documentElement.firstChild.appendChild(script);

  var jsonpUrl = 'http://spreadsheets.google.com/feeds/list/' +
      key + '/' + worksheetIDs.sandbox + '/public/values' +
      '?alt=json-in-script&callback=loadSandboxCallback';
  var script = document.createElement('script');
  script.setAttribute('src', jsonpUrl);
  script.setAttribute('type', 'text/javascript');
  document.documentElement.firstChild.appendChild(script);
};

/**
 * Called from Android.
 *
 * @param {string} roomId The id of the room to load.
 */
IoMap.prototype.showLocationById = function(roomId) {
  var locations = this.LOCATIONS_;
  for (var level in locations) {
    var levelId = level.replace('LEVEL', '');
    for (var loc in locations[level]) {
      var room = locations[level][loc];
      if (loc == roomId) {
        var pos = new google.maps.LatLng(room.lat, room.lng);
        this.map_.panTo(pos);
        this.map_.setZoom(19);
        this.showLevel(levelId);
        if (room.marker_) {
          room.marker_.setAnimation(google.maps.Animation.BOUNCE);

          // Disable the animation after 5 seconds.
          window.setTimeout(function() {
            room.marker_.setAnimation();
          }, 5000);
        }
        return;
      }
    }
  }
};
IoMap.prototype['showLocationById'] = IoMap.prototype.showLocationById;

/**
 * Called when the level is changed. Hides and shows floors.
 */
IoMap.prototype['level_changed'] = function() {
  var level = this.get('level');

  if (this.infoWindow_) {
    this.infoWindow_.setMap(null);
  }

  for (var i = 1, floor; floor = this.floors_[i - 1]; i++) {
    if (i == level) {
      floor.show();
    } else {
      floor.hide();
    }
  }

  this.setHash('level' + level);
};

/**
 * Shows a particular floor.
 *
 * @param {string} level The level to show.
 * @param {boolean=} opt_force  if true, changes the floor even if it's already
 * the current floor.
 */
IoMap.prototype.showLevel = function(level, opt_force) {
  if (!opt_force && level == this.get('level')) {
    return;
  }

  this.set('level', level);
};
IoMap.prototype['showLevel'] = IoMap.prototype.showLevel;

/**
 * Create a marker with the content item's correct icon.
 *
 * @param {Object} item The content item for the marker.
 * @return {google.maps.Marker} The new marker.
 * @private
 */
IoMap.prototype.createContentMarker_ = function(item) {
  if (!item.icon) {
    item.icon = 'generic';
  }

  var image;
  var shadow;
  switch(item.icon) {
    case 'generic':
    case 'info':
    case 'media':
      var image = new google.maps.MarkerImage(
          'marker-' + item.icon + '.png',
          new google.maps.Size(30, 28),
          new google.maps.Point(0, 0),
          new google.maps.Point(13, 26));

      var shadow = new google.maps.MarkerImage(
          'marker-shadow.png',
          new google.maps.Size(30, 28),
          new google.maps.Point(0,0),
          new google.maps.Point(13, 26));
      break;
    case 'toilets':
      var image = new google.maps.MarkerImage(
          item.icon + '.png',
          new google.maps.Size(35, 35),
          new google.maps.Point(0, 0),
          new google.maps.Point(17, 17));
      break;
    case 'elevator':
      var image = new google.maps.MarkerImage(
          item.icon + '.png',
          new google.maps.Size(48, 26),
          new google.maps.Point(0, 0),
          new google.maps.Point(24, 13));
      break;
  }

  var inactive = item.type == 'inactive';

  var latLng = new google.maps.LatLng(item.lat, item.lng);
  var marker = new SmartMarker({
    position: latLng,
    shadow: shadow,
    icon: image,
    title: item.title,
    minZoom: inactive ? 19 : 18,
    clickable: !inactive
  });

  marker['type_'] = item.type;

  if (!inactive) {
    var that = this;
    google.maps.event.addListener(marker, 'click', function() {
      that.openContentInfo_(item);
    });
  }

  return marker;
};

/**
 * Create a label with the content item's title atribute, if it exists.
 *
 * @param {Object} item The content item for the marker.
 * @return {MapLabel?} The new label.
 * @private
 */
IoMap.prototype.createContentLabel_ = function(item) {
  if (!item.title || item.suppressLabel) {
    return null;
  }
  var latLng = new google.maps.LatLng(item.lat, item.lng);
  return new MapLabel({
    'text': item.title,
    'position': latLng,
    'minZoom': item.labelMinZoom || 18,
    'align': item.labelAlign || 'center',
    'fontColor': item.labelColor,
    'fontSize': item.labelSize || 12
  });
}

/**
 * Open a info window a content item.
 *
 * @param {Object} item A content item with content and a marker.
 */
IoMap.prototype.openContentInfo_ = function(item) {
  if (window['MAP_CONTAINER'] !== undefined) {
    window['MAP_CONTAINER']['openContentInfo'](item.room);
    return;
  }

  var sessionBase = 'http://www.google.com/events/io/2011/sessions.html';

  var now = new Date();
  var may11 = new Date('May 11, 2011');
  var day = now < may11 ? 10 : 11;

  var type = item.type;
  var id = item.id;
  var title = item.title;

  var content = ['<div class="infowindow">'];

  var sessions = [];
  var empty = true;

  if (item.type == 'session') {
    if (day == 10) {
      content.push('<h3>' + title + ' - Tuesday May 10</h3>');
    } else {
      content.push('<h3>' + title + ' - Wednesday May 11</h3>');
    }

    for (var i = 0, session; session = this.sessionItems_[i]; i++) {
      if (session.sessionRoom == item.room && session.sessionDay == day) {
        sessions.push(session);
        empty = false;
      }
    }

    sessions.sort(this.sortSessions_);

    for (var i = 0, session; session = sessions[i]; i++) {
      content.push('<div class="session"><div class="session-time">' +
        session.sessionTime + '</div><div class="session-title"><a href="' +
        session.sessionUrl + '">' +
        session.sessionTitle + '</a></div></div>');
    }
  }

  if (item.type == 'sandbox') {
    var sandboxName;
    for (var i = 0, sandbox; sandbox = this.sandboxItems_[i]; i++) {
      if (sandbox.sessionRoom == item.room) {
        if (!sandboxName) {
          sandboxName = sandbox.pod;
          content.push('<h3>' + sandbox.pod + '</h3>');
          content.push('<div class="sandbox">');
        }
        content.push('<div class="sandbox-items"><a href="http://' +
          sandbox.companyUrl + '">' + sandbox.companyName + '</a></div>');
        empty = false;
      }
    }
    content.push('</div>');
    empty = false;
  }

  if (empty) {
    return;
  }

  content.push('</div>');

  var pos = new google.maps.LatLng(item.lat, item.lng);

  if (!this.infoWindow_) {
    this.infoWindow_ = new google.maps.InfoWindow();
  }

  this.infoWindow_.setContent(content.join(''));
  this.infoWindow_.setPosition(pos);
  this.infoWindow_.open(this.map_);
};

/**
 * A custom sort function to sort the sessions by start time.
 *
 * @param {string} a SessionA<enter description here>.
 * @param {string} b SessionB.
 * @return {boolean} True if sessionA is after sessionB.
 */
IoMap.prototype.sortSessions_ = function(a, b) {
  var aStart = parseInt(a.sessionStart.replace(':', ''), 10);
  var bStart = parseInt(b.sessionStart.replace(':', ''), 10);

  return aStart > bStart;
};

/**
 * Adds all overlays (markers, labels) to the map.
 */
IoMap.prototype.addMapContent_ = function() {
  if (!this.ready_) {
    return;
  }

  for (var i = 0, level; level = this.LEVELS_[i]; i++) {
    var floor = this.floors_[i];
    var locations = this.LOCATIONS_['LEVEL' + level];
    for (var roomId in locations) {
      var room = locations[roomId];
      if (room.room == undefined) {
        room.room = roomId;
      }

      if (room.type != 'label') {
        var marker = this.createContentMarker_(room);
        floor.addOverlay(marker);
        room.marker_ = marker;
      }

      var label = this.createContentLabel_(room);
      floor.addOverlay(label);
    }
  }
};

/**
 * Gets the correct tile url for the coordinates and zoom.
 *
 * @param {google.maps.Point} coord The coordinate of the tile.
 * @param {Number} zoom The current zoom level.
 * @return {string} The url to the tile.
 */
IoMap.prototype.getTileUrl = function(coord, zoom) {
  // Ensure that the requested resolution exists for this tile layer.
  if (this.MIN_ZOOM_ > zoom || zoom > this.MAX_ZOOM_) {
    return '';
  }

  // Ensure that the requested tile x,y exists.
  if ((this.RESOLUTION_BOUNDS_[zoom][0][0] > coord.x ||
       coord.x > this.RESOLUTION_BOUNDS_[zoom][0][1]) ||
      (this.RESOLUTION_BOUNDS_[zoom][1][0] > coord.y ||
       coord.y > this.RESOLUTION_BOUNDS_[zoom][1][1])) {
    return '';
  }

  var template = this.TILE_TEMPLATE_URL_;

  if (16 <= zoom && zoom <= 17) {
    template = this.SIMPLE_TILE_TEMPLATE_URL_;
  }

  return template
      .replace('{L}', /** @type string */(this.get('level')))
      .replace('{Z}', /** @type string */(zoom))
      .replace('{X}', /** @type string */(coord.x))
      .replace('{Y}', /** @type string */(coord.y));
};

/**
 * Add the floor overlay to the map.
 *
 * @private
 */
IoMap.prototype.addMapOverlay_ = function() {
  var that = this;
  var overlay = new google.maps.ImageMapType({
    getTileUrl: function(coord, zoom) {
      return that.getTileUrl(coord, zoom);
    },
    tileSize: new google.maps.Size(256, 256)
  });

  google.maps.event.addListener(this, 'level_changed', function() {
    var overlays = that.map_.overlayMapTypes;
    if (overlays.length) {
      overlays.removeAt(0);
    }
    overlays.push(overlay);
  });
};

/**
 * All the features of the map.
 * @type {Object.<string, Object.<string, Object.<string, *>>>}
 */
IoMap.prototype.LOCATIONS_ = {
  'LEVEL1': {
    'northentrance': {
      lat: 37.78381535905965,
      lng: -122.40362226963043,
      title: 'Entrance',
      type: 'label',
      labelColor: '#006600',
      labelAlign: 'left',
      labelSize: 10
    },
    'eastentrance': {
      lat: 37.78328434094279,
      lng: -122.40319311618805,
      title: 'Entrance',
      type: 'label',
      labelColor: '#006600',
      labelAlign: 'left',
      labelSize: 10
    },
    'lunchroom': {
      lat: 37.783112633669575,
      lng: -122.40407556295395,
      title: 'Lunch Room'
    },
    'restroom1a': {
      lat: 37.78372420652042,
      lng: -122.40396961569786,
      icon: 'toilets',
      type: 'inactive',
      title: 'Restroom',
      suppressLabel: true
    },
    'elevator1a': {
      lat: 37.783669090977035,
      lng: -122.40389987826347,
      icon: 'elevator',
      type: 'inactive',
      title: 'Elevators',
      suppressLabel: true
    },
    'gearpickup': {
      lat: 37.78367863020862,
      lng: -122.4037617444992,
      title: 'Gear Pickup',
      type: 'label'
    },
    'checkin': {
      lat: 37.78334369645064,
      lng: -122.40335404872894,
      title: 'Check In',
      type: 'label'
    },
    'escalators1': {
      lat: 37.78353872135503,
      lng: -122.40336209535599,
      title: 'Escalators',
      type: 'label',
      labelAlign: 'left'
    }
  },
  'LEVEL2': {
    'escalators2': {
      lat: 37.78353872135503,
      lng: -122.40336209535599,
      title: 'Escalators',
      type: 'label',
      labelAlign: 'left',
      labelMinZoom: 19
    },
    'press': {
      lat: 37.78316774962791,
      lng: -122.40360751748085,
      title: 'Press Room',
      type: 'label'
    },
    'restroom2a': {
      lat: 37.7835334217721,
      lng: -122.40386635065079,
      icon: 'toilets',
      type: 'inactive',
      title: 'Restroom',
      suppressLabel: true
    },
    'restroom2b': {
      lat: 37.78250317562106,
      lng: -122.40423113107681,
      icon: 'toilets',
      type: 'inactive',
      title: 'Restroom',
      suppressLabel: true
    },
    'elevator2a': {
      lat: 37.783669090977035,
      lng: -122.40389987826347,
      icon: 'elevator',
      type: 'inactive',
      title: 'Elevators',
      suppressLabel: true
    },
    '1': {
      lat: 37.78346240732338,
      lng: -122.40415401756763,
      icon: 'media',
      title: 'Room 1',
      type: 'session',
      room: '1'
    },
   '2': {
      lat: 37.78335005596647,
      lng: -122.40431495010853,
      icon: 'media',
      title: 'Room 2',
      type: 'session',
      room: '2'
    },
    '3': {
      lat: 37.783215446097124,
      lng: -122.404490634799,
      icon: 'media',
      title: 'Room 3',
      type: 'session',
      room: '3'
    },
    '4': {
      lat: 37.78332461789977,
      lng: -122.40381203591824,
      icon: 'media',
      title: 'Room 4',
      type: 'session',
      room: '4'
    },
    '5': {
      lat: 37.783186828219335,
      lng: -122.4039850383997,
      icon: 'media',
      title: 'Room 5',
      type: 'session',
      room: '5'
    },
    '6': {
      lat: 37.783013000871364,
      lng: -122.40420497953892,
      icon: 'media',
      title: 'Room 6',
      type: 'session',
      room: '6'
    },
    '7': {
      lat: 37.7828783903882,
      lng: -122.40438133478165,
      icon: 'media',
      title: 'Room 7',
      type: 'session',
      room: '7'
    },
    '8': {
      lat: 37.78305009820564,
      lng: -122.40378588438034,
      icon: 'media',
      title: 'Room 8',
      type: 'session',
      room: '8'
    },
    '9': {
      lat: 37.78286673120095,
      lng: -122.40402393043041,
      icon: 'media',
      title: 'Room 9',
      type: 'session',
      room: '9'
    },
    '10': {
      lat: 37.782719401312626,
      lng: -122.40420028567314,
      icon: 'media',
      title: 'Room 10',
      type: 'session',
      room: '10'
    },
    'appengine': {
      lat: 37.783362774996625,
      lng: -122.40335941314697,
      type: 'sandbox',
      icon: 'generic',
      labelMinZoom: 19,
      labelAlign: 'right',
      title: 'App Engine'
    },
    'chrome': {
      lat: 37.783730566003555,
      lng: -122.40378990769386,
      type: 'sandbox',
      icon: 'generic',
      title: 'Chrome'
    },
    'googleapps': {
      lat: 37.783303419504094,
      lng: -122.40320384502411,
      type: 'sandbox',
      icon: 'generic',
      labelMinZoom: 19,
      title: 'Apps'
    },
    'geo': {
      lat: 37.783365954753805,
      lng: -122.40314483642578,
      type: 'sandbox',
      icon: 'generic',
      labelMinZoom: 19,
      title: 'Geo'
    },
    'accessibility': {
      lat: 37.783414711013485,
      lng: -122.40342646837234,
      type: 'sandbox',
      icon: 'generic',
      labelMinZoom: 19,
      labelAlign: 'right',
      title: 'Accessibility'
    },
    'developertools': {
      lat: 37.783457107734876,
      lng: -122.40347877144814,
      type: 'sandbox',
      icon: 'generic',
      labelMinZoom: 19,
      labelAlign: 'right',
      title: 'Dev Tools'
    },
    'commerce': {
      lat: 37.78349102509448,
      lng: -122.40351900458336,
      type: 'sandbox',
      icon: 'generic',
      labelMinZoom: 19,
      labelAlign: 'right',
      title: 'Commerce'
    },
    'youtube': {
      lat: 37.783537661438515,
      lng: -122.40358605980873,
      type: 'sandbox',
      icon: 'generic',
      labelMinZoom: 19,
      labelAlign: 'right',
      title: 'YouTube'
    },
    'officehoursfloor2a': {
      lat: 37.78249045644304,
      lng: -122.40410104393959,
      title: 'Office Hours',
      type: 'label'
    },
    'officehoursfloor2': {
      lat: 37.78266852473624,
      lng: -122.40387573838234,
      title: 'Office Hours',
      type: 'label'
    },
    'officehoursfloor2b': {
      lat: 37.782844472747406,
      lng: -122.40365579724312,
      title: 'Office Hours',
      type: 'label'
    }
  },
  'LEVEL3': {
    'escalators3': {
      lat: 37.78353872135503,
      lng: -122.40336209535599,
      title: 'Escalators',
      type: 'label',
      labelAlign: 'left'
    },
    'restroom3a': {
      lat: 37.78372420652042,
      lng: -122.40396961569786,
      icon: 'toilets',
      type: 'inactive',
      title: 'Restroom',
      suppressLabel: true
    },
    'restroom3b': {
      lat: 37.78250317562106,
      lng: -122.40423113107681,
      icon: 'toilets',
      type: 'inactive',
      title: 'Restroom',
      suppressLabel: true
    },
    'elevator3a': {
      lat: 37.783669090977035,
      lng: -122.40389987826347,
      icon: 'elevator',
      type: 'inactive',
      title: 'Elevators',
      suppressLabel: true
    },
    'keynote': {
      lat: 37.783250423488326,
      lng: -122.40417748689651,
      icon: 'media',
      title: 'Keynote',
      type: 'label'
    },
    '11': {
      lat: 37.78283069370135,
      lng: -122.40408763289452,
      icon: 'media',
      title: 'Room 11',
      type: 'session',
      room: '11'
    },
    'googletv': {
      lat: 37.7837125474666,
      lng: -122.40362092852592,
      type: 'sandbox',
      icon: 'generic',
      title: 'Google TV'
    },
    'android': {
      lat: 37.783530242022124,
      lng: -122.40358874201775,
      type: 'sandbox',
      icon: 'generic',
      title: 'Android'
    },
    'officehoursfloor3': {
      lat: 37.782843412820846,
      lng: -122.40365579724312,
      title: 'Office Hours',
      type: 'label'
    },
    'officehoursfloor3a': {
      lat: 37.78267170452323,
      lng: -122.40387842059135,
      title: 'Office Hours',
      type: 'label'
    }
  }
};


google.maps.event.addDomListener(window, 'load', function() {
  window['IoMap'] = new IoMap();
});
