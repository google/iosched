/**
 * Creates a new level control.
 * @constructor
 * @param {IoMap} iomap  the IO map controller.
 * @param {Array.<string>} levels  the levels to create switchers for.
 */
function LevelControl(iomap, levels) {
  var that = this;
  this.iomap_ = iomap;
  this.el_ = this.initDom_(levels);

  google.maps.event.addListener(iomap, 'level_changed', function() {
    that.changeLevel_(iomap.get('level'));
  });
}

/**
 * Gets the DOM element for the control.
 * @return {Element}
 */
LevelControl.prototype.getElement = function() {
  return this.el_;
};

/**
 * Creates the necessary DOM for the control.
 * @return {Element}
 */
LevelControl.prototype.initDom_ = function(levelDefinition) {
  var controlDiv = document.createElement('DIV');
  controlDiv.setAttribute('id', 'levels-wrapper');

  var levels = document.createElement('DIV');
  levels.setAttribute('id', 'levels');
  controlDiv.appendChild(levels);

  var levelSelect = this.levelSelect_ = document.createElement('DIV');
  levelSelect.setAttribute('id', 'level-select');

  levels.appendChild(levelSelect);

  this.levelDivs_ = [];

  var that = this;
  for (var i = 0, level; level = levelDefinition[i]; i++) {
    var div = document.createElement('DIV');
    div.innerHTML = 'Level ' + level;
    div.setAttribute('id', 'level-' + level);
    div.className = 'level';
    levels.appendChild(div);
    this.levelDivs_.push(div);

    google.maps.event.addDomListener(div, 'click', function(e) {
      var id = e.currentTarget.getAttribute('id');
      var level = parseInt(id.replace('level-', ''), 10);
      that.iomap_.setHash('level' + level);
    });
  }

  controlDiv.index = 1;
  return controlDiv;
};

/**
 * Changes the highlighted level in the control.
 * @param {number} level  the level number to select.
 */
LevelControl.prototype.changeLevel_ = function(level) {
  if (this.currentLevelDiv_) {
    this.currentLevelDiv_.className =
        this.currentLevelDiv_.className.replace(' level-selected', '');
  }

  var h = 25;
  if (window['ioEmbed']) {
    h = (window.screen.availWidth > 600) ? 34 : 24;
  }

  this.levelSelect_.style.top = 9 + ((level - 1) * h) + 'px';

  var div = this.levelDivs_[level - 1];

  div.className += ' level-selected';

  this.currentLevelDiv_ = div;
};
