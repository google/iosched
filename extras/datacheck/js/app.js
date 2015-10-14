(function(context) {
  var BASE='http://storage.googleapis.com/iosched-updater-dev.appspot.com/';

  function Board(boardEl) {
    this.boardEl = boardEl;
    this.template = boardEl.querySelector("#template");
    this.init();
    this.addListeners();
  }

  Board.prototype.init = function() {
    getJson('__raw_session_data.json', this.handleRawData.bind(this));
    getJson('manifest_v3__qa_.json', function(e) {
      var files=e.target.response["data_files"];
      for (var i=0; i<files.length; i++) {
        if (files[i].match('session_data.*.json')) {
          getJson(files[i], this.handleRawIoSchedData.bind(this));
          return;
        }
      }
      console.log("ERROR: could not find proper data file on "+e.target.response);
    }.bind(this));
  }

  function getJson(filename, callback) {
    var xhr = new XMLHttpRequest();
    xhr.open('GET', BASE+filename);
    xhr.responseType = 'json';
    xhr.onload = callback;
    xhr.send();
  }

  Board.prototype.handleRawIoSchedData = function(e) {
    var data = e.target.response;
    if (data == null || ! ("sessions" in data)) {
      console.log(e);
      window.alert("invalid iosched data, check console");
      return;
    }
    this.ioschedData = {};
    for (var i=0; i<data["sessions"].length; i++) {
      var session = data["sessions"][i];
      this.ioschedData[session["id"]] = session;
    }
  }

  Board.prototype.handleRawData = function(e) {
    var data = e.target.response;
    if (data == null || ! ("topics" in data)) {
      console.log(e);
      window.alert("invalid raw cms data, check console");
      return;
    }
    this.data = data;
    for (var i=0; i<data["topics"].length; i++) {
      var topic = data["topics"][i];
      var cell = template.cloneNode(true);
      cell.removeAttribute("id");
      cell.style.backgroundImage = "url('http://storage.googleapis.com/iosched-updater-dev.appspot.com/images/sessions/w200/"+topic["id"]+".jpg')";
      cell.querySelector(".title").innerText = topic.title;
      var link = cell.querySelector(".session_web");
      link.href = 'https://www.google.com/events/io/schedule/session/'+topic['id'];
      var linkImg = cell.querySelector(".iosched_img");
      linkImg.href = 'http://storage.googleapis.com/iosched-updater-dev.appspot.com/images/sessions/w1000/'+topic["id"]+".jpg";
      var imagesLinks = cell.querySelector(".images_from_cms");
      if ("documents" in topic) for (var d=0; d<topic["documents"].length; d++) {
        var link = document.createElement('a');
        link.innerText = d+1;
        link.href = topic['documents'][d]['url'];
        link.target = '_blank';
        link.type = 'image/jpeg';
        if (d>0) {
          var sep = document.createElement('span');
          sep.innerHTML = "&middot;";
          imagesLinks.appendChild(sep);
        }
        imagesLinks.appendChild(link);
      }
      if (imagesLinks.childElementCount == 0) {
        imagesLinks.innerText = "none";
      }
      // setup data links:
      var cmsData = cell.querySelector(".data_from_cms");
      var ioschedData = cell.querySelector(".data_from_iosched");

      cmsData.addEventListener('click', function(topic, e) {
        this.showData(topic);
        e.preventDefault();
        e.stopPropagation();
      }.bind(this, topic));

      ioschedData.addEventListener('click', function(id, e) {
        this.showData(this.ioschedData[id]);
        e.preventDefault();
        e.stopPropagation();
      }.bind(this, topic["id"]));

      this.boardEl.appendChild(cell);
    }
  };

  Board.prototype.showData = function(data) {
    var dialog = document.getElementById("centerpoint");
    document.body.classList.add("showDialog");
    dialog.style.top = "calc( 50% + "+window.scrollY+"px )";
    document.querySelector("#dialog .source").innerText =
      JSON.stringify(data, null, 2);
  }

  Board.prototype.addListeners = function() {
    document.body.addEventListener('keyup', function(e) {
      if (e.keyCode == 27 ) {
        document.body.classList.remove("showDialog");
      }
    });
        
    document.body.addEventListener('click', function(e) {
      document.body.classList.remove("showDialog");
    });
    document.getElementById('centerpoint').addEventListener('click', function(e) {
      e.stopPropagation();
    });
  }

  window.Board = Board;

})(window);


document.addEventListener('DOMContentLoaded', function() {
  window.board = new Board(document.querySelector('#board'));
});
