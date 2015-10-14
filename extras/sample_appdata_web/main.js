(function() {

  // go to https://console.developers.google.com/project
  // - create project
  // - APIs & auth -> "APIs" -> activate "Drive API"
  // - APIs & auth -> Credentials -> Create new client ID
  //     Web application
  //     Authorized JS origins: http://yourwebsite.com, http://localhost:8080 (for
  //       debugging)
  //     Authorized redirect URI: http://yourwebsite.com/oauth2callback,
  //       http://localhost:8080/oauth2callback (for debugging)
  //   Copy the Client_ID below. You will NOT need the client secret or other
  //   info. Make sure you run the app from a domain listed in "authorized JS
  //   origins".
//  var CLIENT_ID = "924360940596-00sbvk7bq94fu5i86lv454n81gdo6sso.apps.googleusercontent.com"
  var CLIENT_ID = "237695054204-dk3ta44733ohtelcf4vf3prqgob3iai0.apps.googleusercontent.com";
//  var CLIENT_ID = "285482710859-u0q01fls2sqng5onv5cb8lp48nk2ju1s.apps.googleusercontent.com";
  var SCOPES = [ "https://www.googleapis.com/auth/drive.appdata" ];
  var FILENAME = "user_data.json",
      appDataFile = null;

  /* Executed when the APIs finish loading */
  window.handleClientLoad = function() {
    switchWaitingState(true);
    loadDriveLibrary(function() {
      addListeners();
      authenticate(true);
    });
  }

  function loadDriveLibrary(callback) {
    gapi.client.load('drive', 'v2', callback);
  }

  function authenticate(immediate) {
    gapi.auth.authorize(
      {'client_id': CLIENT_ID,
       'scope': SCOPES.join(' '),
       'immediate': immediate},
       handleAuthResult);
  }

  function setEnabled(isEnabled, element) {
    if (isEnabled) {
      element.removeAttribute('disabled');
    } else {
      element.setAttribute('disabled', true);
    }
  }

  function switchWaitingState(waiting) {
    if (waiting) {
      document.body.classList.add('waiting');
    } else {
      document.body.classList.remove('waiting');
    }
  }

  function switchSignedState(isLoggedIn) {
    switchWaitingState(false);
    setEnabled(!isLoggedIn, document.getElementById('signin'));
    setEnabled(isLoggedIn, document.getElementById('logout'));
    setEnabled(isLoggedIn, document.querySelector('#user_info textarea'));
    setEnabled(isLoggedIn, document.getElementById('savedata'));
    setEnabled(isLoggedIn, document.getElementById('loaddata'));
    setEnabled(isLoggedIn, document.getElementById('listfiles'));
  }

  function handleAuthResult(authResult) {
    if (authResult && authResult['status']['signed_in']) {
      log('Signed in! See console for details');
      console.log('Auth Result', authResult);
      switchSignedState(true);
      loadData();
    } else {
      log('Could not sign in');
      switchSignedState(false);
    }
  }

  function log(m) {
    console.log(m);
    if (typeof(m) !== 'string') {
      m = JSON.stringify(m);
    }
    document.getElementById('_logarea').innerText+=m+'\n';
  }

  function addListeners() {
    var fileContents = document.querySelector('#user_info textarea');

     // Attach a click listener to a button to trigger the flow.
    document.getElementById('signin').addEventListener('click', function() {
      switchWaitingState(true);
      authenticate(false);
    });

    document.getElementById('logout').addEventListener('click', function(e) {
      gapi.auth.signOut();
    });
    document.getElementById('savedata').addEventListener('click', function(e) {
      switchWaitingState(true);
      if (!appDataFile) {
        appDataFile = new AppData(FILENAME);
      }
      var obj = null;
      try {
        obj = JSON.parse(fileContents.value);
      } catch (e) {
        log('Could not parse JSON. See console for details.');
        console.log(e);
        switchWaitingState(false);
        return;
      }
      try {
        appDataFile.writeObject(obj, function(jsonResponse) {
          switchWaitingState(false);
          log('File '+jsonResponse['title']+' saved. See console for details.');
          console.log('WriteData result', jsonResponse);
        });
      } catch (e) {
        switchWaitingState(false);
        console.log(e);
        log('Error while writing content. '+e);
      }
    });
    document.getElementById('loaddata').addEventListener('click', loadData);
    document.getElementById('listfiles').addEventListener('click', listFiles);
  }

  function loadData() {
    var fileContents = document.querySelector('#user_info textarea');
    switchWaitingState(true);
    if (! ('appDataFile' in window)) {
      appDataFile = new AppData(FILENAME);
    }
    appDataFile.readObject(function(obj, rawResponse) {
      switchWaitingState(false);
      console.log('ReadObject raw result', rawResponse);
      if (obj==null) {
        log('file content is null. See console for details.');
      } else {
        log('Content loaded. See console for details.');
        fileContents.value = JSON.stringify(obj, null, 2);
      }
    });
  }


  function listFiles() {
    switchWaitingState(true);
    if (! ('appDataFile' in window)) {
      appDataFile = new AppData(FILENAME);
    }
    appDataFile.listAppDataFiles(function(files) {
      switchWaitingState(false);
      console.log('files in AppData folder: ', files);
      log((files && files.length || 0) + ' files in AppData folder. See console for details');
    });
  }


  switchWaitingState(true);
  var po = document.createElement('script');
  po.type = 'text/javascript'; po.async = true;
  po.src = 'https://apis.google.com/js/client.js?onload=handleClientLoad';
  var s = document.getElementsByTagName('script')[0];
  s.parentNode.insertBefore(po, s);


})();
