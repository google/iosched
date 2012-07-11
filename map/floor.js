/**
 * Creates a new Floor.
 * @constructor
 * @param {google.maps.Map=} opt_map
 */
function Floor(opt_map) {
  /**
   * @type Array.<google.maps.MVCObject>
   */
  this.overlays_ = [];

  /**
   * @type boolean
   */
  this.shown_ = true;

  if (opt_map) {
    this.setMap(opt_map);
  }
}

/**
 * @param {google.maps.Map} map
 */
Floor.prototype.setMap = function(map) {
  this.map_ = map;
};

/**
 * @param {google.maps.MVCObject} overlay  For example, a Marker or MapLabel.
 *    Requires a setMap method.
 */
Floor.prototype.addOverlay = function(overlay) {
  if (!overlay) return;
  this.overlays_.push(overlay);
  overlay.setMap(this.shown_ ? this.map_ : null);
};

/**
 * Sets the map on all the overlays
 * @param {google.maps.Map} map  The map to set.
 */
Floor.prototype.setMapAll_ = function(map) {
  this.shown_ = !!map;
  for (var i = 0, overlay; overlay = this.overlays_[i]; i++) {
    overlay.setMap(map);
  }
};

/**
 * Hides the floor and all associated overlays.
 */
Floor.prototype.hide = function() {
  this.setMapAll_(null);
};

/**
 * Shows the floor and all associated overlays.
 */
Floor.prototype.show = function() {
  this.setMapAll_(this.map_);
};
