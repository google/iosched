// Copyright (c) 2017 Google Inc.
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

// Cloud Functions for Firebase is used to process session reservation requests.
//
// Clients are permitted to write requests into the queue. RTDB Rules are used
// to restrict clients to writing one request at a time.
//
// Each written request triggers a function that processes the request. Once the
// request is processed it is removed from the queue allowing clients to submit
// more requests.

var functions = require('firebase-functions');

// firebase-admin module is used to perform RTDB updates while processing
// reservation requests.
const admin = require('firebase-admin');
admin.initializeApp(functions.config().firebase);

// Amount of time (in millis) before the start of a session required for
// reservations to be allowed.
const RES_CUT_OFF = 1800000;

const CLASH_NONE = 0;
const CLASH_WITH_OTHER = 1;
const CLASH_WITH_SAME = 2;

const RESERVATION_RESERVED = 'reserved';
const RESERVATION_DENIED_NO_SPACE = 'denied no space';
const RESERVATION_CLOSED = 'reservations closed';
const RESERVATION_RETURNED = 'returned';
const RESERVATION_RETURN_FAILED = 'unable to complete return';
const RESERVATION_FAILED = 'unable to complete reservation';
const RESERVATION_DENIED_CLASHING = 'denied other session clashes';
const CLASH_CHECK_FAILED = 'clash check failed';

/**
 * This function listens to the request queue. When a request is written to the
 * queue by a client, this function executes.
 *
 * This function assumes that requests are submitted of the form:
 * /queue/<user id> : <session id>
 */
exports.processRequest = functions.database.ref('/queue/{uid}').onWrite(event => {
  if (event.data.val() == null) {
    return;
  }

  const sid = event.data.val();
  const uid = event.params.uid;

  // Check that session is valid
  return admin.database().ref('/sessions').child(sid).once('value').then(function(snapshot) {
    if (snapshot.val() == null) {
      console.log('session with session id: ' + sid + ' not found so removing request');
      return admin.database().ref('/queue').child(uid).set({});
    }

    return process(uid, sid).then(function(result) {
      console.log('reservation with uid: ' + uid + ' and sid: ' + sid + ' ended with result: ' + result);
      // Processing complete so emove request
      return admin.database().ref('/queue').child(uid).set({});
    });
  });
});

/**
 * Process reservation request by user with ID uid for session with ID sid.
 *
 * @param uid ID of user requesting reservation.
 * @param sid Session ID of requested reservation.
 * @returns {Promise.<TResult>} result of processing reservation.
 */
function process(uid, sid) {
  // Get session
  return admin.database().ref('/sessions').child(sid).once('value').then(function(snapshot) {
    return snapshot.val();
  }).then(function(curr_session) {
    // Check that are reservations still open.
    const now = new Date();
    if (curr_session.start_time - RES_CUT_OFF <= now.getTime()) {
      // Reservations are closed.
      return RESERVATION_CLOSED;
    }

    return checkForClash(uid, sid, curr_session).then(function(clash) {
      switch(clash) {
        case CLASH_WITH_SAME:
          // clash with this session so return
          return handleReturn(uid, sid);
        case CLASH_WITH_OTHER:
          // clash with other session so reject
          return handleClash(uid, sid);
        case CLASH_NONE:
          // no clash so proceed with reservation request
          return handleReservation(uid, sid);
        default:
          // do nothing
          return CLASH_CHECK_FAILED;
      }
    });
  });
}

/**
 * Check whether or not the user currently has any reservations that would
 * clash with the time of the session in the current reservation request.
 *
 * @param uid ID of user requesting reservation.
 * @param sid Session ID of requested reservation.
 * @param curr_session Session trying to be reserved.
 * @returns {Promise.<TResult>} result can be 2, 1 or 0.
 *  2 - The user is returning the current reservation.
 *  1 - The user has a clash with an existing reservation.
 *  0 - There is no clash.
 */
function checkForClash(uid, sid, curr_session) {
  // Get all existing reservations for this user
  return admin.database().ref('/sessions').orderByChild('reservations/' + uid).equalTo('granted')
      .once('value').then(function(snapshot) {
        const sessions = snapshot.val();
        for (var temp_session_id in sessions) {
          // Check for clash with existing reservation
          if (curr_session.time_start < sessions[temp_session_id].time_end ||
              curr_session.time_end > sessions[temp_session_id].time_start) {
            console.log('clash found');
            if (temp_session_id == sid) {
              return CLASH_WITH_SAME;
            }
            return CLASH_WITH_OTHER;
          }
        }
        return CLASH_NONE;
      });
}

/**
 * Handle a return request. If a user requests a reservation for a session that
 * has already been granted, that reservation should be returned.
 *
 * The result of the returned reservation is written to:
 * /sessions/<session id>/reservations/<user id>: <result>
 *
 * @param uid ID of user returning reservation.
 * @param sid Session ID of return request.
 * @returns {Promise.<TResult>} string result of return.
 */
function handleReturn(uid, sid) {
  // Remove session
  return admin.database().ref('/sessions').child(sid).child('seats').transaction(function(seats) {
    if (seats) {
      seats.reserved--;
    }
    return seats;
  }).then(function(result) {
    var committed = result.committed;
    var snapshot = result.snapshot;
    if (committed && snapshot != null) {
      return admin.database().ref('/sessions').child(sid).child('reservations').child(uid).set('returned')
          .then(function() {
            return RESERVATION_RETURNED;
          });
    } else {
      return RESERVATION_RETURN_FAILED;
    }
  });
}

/**
 * Handle a clashing reservation request. When a user requests a reservation
 * for a session that clashes with an existing reservation of the user this
 * reservation is denied.
 *
 * The result of the denied reservation is written to:
 * /sessions/<session id>/reservations/<user id>: <result>
 *
 * @param uid ID of user requesting reservation.
 * @param sid Session ID of requested reservation.
 * @returns {Promise.<TResult>} string result of denied reservation.
 */
function handleClash(uid, sid) {
  return admin.database().ref('/sessions').child(sid).child('reservations').child(uid).set('denied')
      .then(function() {
        return RESERVATION_DENIED_CLASHING;
      })
}

/**
 * Handle valid reservation request. A reservation request is considered valid
 * if there requesting user has no existing clashing reservations. Reservations
 * will be granted if there are available seats in the session.
 *
 * The result of the reservation is written to:
 * /sessions/<session id>/reservations/<user id>: <result>
 *
 * @param uid ID of user requesting reservation.
 * @param sid Session ID of requested reservation.
 * @returns {Promise.<TResult>} string result of handled reservation.
 */
function handleReservation(uid, sid) {
  var prevSeats = {};
  return admin.database().ref('/sessions').child(sid).child('seats').transaction(function(seats) {
    if (seats) {
      prevSeats.reserved = seats.reserved;
      // add it if possible
      if (seats.capacity > seats.reserved) {
        console.log(uid + ' has been granted a seat');
        seats.reserved++;
      } else {
        console.log('no seats available');
      }
    }
    return seats;
  }).then(function(result) {
    var committed = result.committed;
    var snapshot = result.snapshot;
    if (committed && snapshot != null) {
      currSeats = snapshot.val();
      if (currSeats.reserved - 1 == prevSeats.reserved) {
        return admin.database().ref('/sessions').child(sid).child('reservations').child(uid).set('granted')
            .then(function() {
              return RESERVATION_RESERVED;
            });
      } else {
        return admin.database().ref('/sessions').child(sid).child('reservations').child(uid).set('denied')
            .then(function() {
              return RESERVATION_DENIED_NO_SPACE;
            });
      }
    } else {
      return admin.database().ref('/sessions').child(sid).child('reservations').child(uid).set('failed')
          .then(function() {
            return RESERVATION_FAILED;
          });
    }
  });
}
