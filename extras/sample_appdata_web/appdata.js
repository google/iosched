(function(context) {

  var GCM_URL = 'https://iosched-gcm-dev.appspot.com/send/self/sync_user';
  var METADATA_DOWNLOAD_URL = 'downloadUrl';
  var METADATA_FILE_ID = 'id';
  var DEBUG = true;

  function AppData(filename) {
    this.filename = filename;
    this.fileDownloadURL = null;
    this.fileID = null;
    this.initialized = false;
  }


  /**
   * Update existing file or create a new one in the Application Data folder.
   * Warning: existing file will be replaced. Please, add logic to always
   * read the existing content right before writing.
   *
   * If the input object is not in the expected format, an Error is thrown.
   *
   * Note: We did not implement optimistic locking (versioning) because
   * simultaneous changes on multiple devices and same account are not
   * expected to happen frequently. In the future, we should add an
   * incremental "version" field that would block updates if they are based
   * on an older version, forcing the read-before-write behavior.
   *
   * @param {object} obj to be stringified and written. Cannot be null.
   * @param {Function} callback Function to call when the request is complete.
   */

  AppData.prototype.writeObject = function(obj, callback) {
    if (!this.isValidData(obj)) {
      throw new Error("Data is not in a valid format. Expected "+
          "something like {\"starred_sessions\": [\"string_session_id1\","+
          " \"string_session_id2\"]} but found "+JSON.stringify(obj));
    }
    this._maybeInit(this._writeObjectImpl, [obj, callback]);
  }


  /**
   * Read remote content from AppData folder. No validation is applied to
   * the contents read.
   */
  AppData.prototype.readObject = function(callback) {
    this._maybeInit(this._readObjectImpl, [callback]);
  }


  /**
   * Validate the argument and returns false if it is not in the format
   * expected by writeObject method.
   */
  AppData.prototype.isValidData = function(obj) {
    return obj!=null
      && typeof(obj)==='object'
      && 'starred_sessions' in obj
      && 'viewed_videos' in obj
      && 'feedback_submitted_sessions' in obj
      && 'gcm_key' in obj
      && Array.isArray(obj['starred_sessions'])
      && Array.isArray(obj['viewed_videos'])
      && Array.isArray(obj['feedback_submitted_sessions'])
      && typeof(obj['gcm_key'])==='string'
      && obj['starred_sessions'].every(function(item) {
        return typeof(item)==='string';
      })
      && obj['viewed_videos'].every(function(item) {
        return typeof(item)==='string';
      })
      && obj['feedback_submitted_sessions'].every(function(item) {
        return typeof(item)==='string';
      });
  }


  /**
   * Returns all files in the AppData folder. This method is mostly used
   * for debug purposes
   */
  AppData.prototype.listAppDataFiles = function(callback) {
    var retrievePageOfFiles = function(request, result) {
      request.execute(function(resp) {
        if (DEBUG) {
          console.debug('found files in appfolder:', resp.items);
        }
        for (var i=0; resp.items && i<resp.items.length; i++) {
          result.push(resp.items[i]);
        }
        var nextPageToken = resp.nextPageToken;
        if (nextPageToken) {
          request = gapi.client.drive.files.list({'pageToken': nextPageToken});
          retrievePageOfFiles(request, result);
        } else {
          callback(result);
        }
      }.bind(this));
    }.bind(this);
    var initialRequest = gapi.client.drive.files.list({
      'q': '\'appdata\' in parents'
    });
    retrievePageOfFiles(initialRequest, []);
  }



  //  ------- End of public methods


  // The methods below should not be used by external clients, since their
  // signatures might change in the future.

  AppData.prototype._maybeInit = function(callback, callbackParams) {
    if (!this.initialized) {
      this._fetchMostRecent(function(metadata) {
        this._extractMetadata(metadata);
        this.initialized = true;
        callback.apply(this, callbackParams);
      }.bind(this));
    } else {
      callback.apply(this, callbackParams);
    }
  }


  AppData.prototype._readObjectImpl = function(callback) {
    if (!this.fileDownloadURL) {
      callback(null);
    } else {
      var accessToken = gapi.auth.getToken().access_token;
      var xhr = new XMLHttpRequest();
      xhr.open('GET', this.fileDownloadURL);
      xhr.setRequestHeader('Authorization', 'Bearer ' + accessToken);
      xhr.onload = function(r) {
        var rObj = null;
        try {
          rObj = JSON.parse(r.target.responseText);
        } catch (e) {
          rObj = { 'error': 'Could not parse JSON. '+e };
        }
        callback(rObj, {
          'response': r.target.response, 'responseType': r.target.responseType,
          'status': r.target.status, 'statusText': r.target.statusText } );
      };
      xhr.onerror = function(e) {
        callback(null, e);
      };
      xhr.send();
    }
  }


  AppData.prototype._extractMetadata = function(metadata) {
    if (metadata) {
      this.fileID = metadata[METADATA_FILE_ID];
      this.fileDownloadURL = metadata[METADATA_DOWNLOAD_URL];
    }
  };

  function _compareRFC3339Dates(date1, date2) {
    return date1<date2?-1:(date1>date2?1:0);
  }

  /**
   * List all files with a given name contained in the Application Data folder.
   * Google Drive allows more than one file with the same name. This function
   * first fetchs all files with the given name and then return the metadata
   * of the most recent one, in case there is more than one.
   *
   * @param {String} filename to search for. Prefix is not supported.
   * @param {Function} callback Function to call when the request is complete.
   */
  AppData.prototype._fetchMostRecent = function(callback) {
    var retrievePageOfFiles = function(request, result) {
      request.execute(function(resp) {
        if (DEBUG) {
          console.debug('found files with title', resp.items);
        }
        for (var i=0; resp.items && i<resp.items.length; i++) {
          var item = resp.items[i];
          // checking for title=="filename" should not be necessary,
          // but an apparent error on Drive forces us to use "contains"
          // instead of "==" in the query.
          if (result == null ||
              (item['title'] == this.filename &&
                _compareRFC3339Dates(
                  result['modifiedDate'], item['modifiedDate']) < 0)) {
            result = item;
          }
        }
        var nextPageToken = resp.nextPageToken;
        if (nextPageToken) {
          request = gapi.client.drive.files.list({'pageToken': nextPageToken});
          retrievePageOfFiles(request, result);
        } else {
          callback(result);
        }
      }.bind(this));
    }.bind(this);

    var initialRequest = gapi.client.drive.files.list({
      // The query could be more precise by using "=" instead of "contains",
      // but at this moment, using "title=" clause throws an error.
      'q': '\'appdata\' in parents and title contains \''+this.filename+'\''
    });
    retrievePageOfFiles(initialRequest, null);
  }

  AppData.prototype._writeObjectImpl = function(obj, callback) {
    if (obj == null) {
      if (DEBUG) {
        console.log("obj is null. Ignoring");
      }
      return;
    }
    const boundary = '-------314159265358979323846';
    const delimiter = "\r\n--" + boundary + "\r\n";
    const close_delim = "\r\n--" + boundary + "--";

    var fileExists = ( this.fileID != null );

    var metadata;
    if (!fileExists) {
      var metadata = {
        'title': this.filename,
        'mimeType': 'application/json',
        'parents': [{'id': 'appdata' }]
      };
    } else {
      metadata = {};
    }

    // convert JSON to base64 to avoid transmission/handling issues with
    // special characters
    var base64Data = btoa(JSON.stringify(obj));
    var multipartRequestBody =
        delimiter +
        'Content-Type: application/json\r\n\r\n' +
        JSON.stringify(metadata) +
        delimiter +
        'Content-Type: application/json\r\n' +
        'Content-Transfer-Encoding: base64\r\n' +
        '\r\n' +
        base64Data +
        close_delim;


    var path = '/upload/drive/v2/files' + (fileExists ? '/'+this.fileID : '')
    var request = gapi.client.request({
        'path': path,
        'method': fileExists ? 'PUT' : 'POST',
        'params': {'uploadType': 'multipart'},
        'headers': {
          'Content-Type': 'multipart/mixed; boundary="' + boundary + '"'
        },
        'body': multipartRequestBody});

    request.execute(function(file) {
      this._extractMetadata(file);
      this._sendGCMPing(obj);
      callback(file);
    }.bind(this));
  }


  function extractGcmGroupId(obj) {
    if (obj==null || typeof(obj)!='object' || ! ('gcm_key' in obj)
      || typeof(obj['gcm_key'])!='string') {
        return null;
    }
    return obj['gcm_key']);
  }

  AppData.prototype._sendGCMPing = function(obj) {
    try {
      var gcmGroupId = extractGcmGroupId(obj);
      if (gcmGroupId == null) {
        return;
      }
      var xhr = new XMLHttpRequest();
      xhr.open('POST', GCM_URL);
      xhr.setRequestHeader("Authorization", "key="+gcmGroupId);
      xhr.send("{'sync_jitter': 100}");
    } catch (e) {
      console.log("Could not notify other devices.");
    }
  }

  context.AppData = AppData;

})(window);

