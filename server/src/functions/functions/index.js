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
// reservations to be allowed. 30 minutes.
const RES_CUT_OFF = 1800000;

const PATH_SESSIONS = 'sessions';
const PATH_RESERVATIONS = 'reservations';
const PATH_RESULT = 'result';
const PATH_STATUS = 'status';
const PATH_SEATS = 'seats';
const PATH_ACTION = 'action';
const PATH_SESSION = 'session';
const PATH_QUEUE = 'queue';

const ACTION_RESERVE = 'reserve';
const ACTION_RETURN = 'return';

const STATUS_GRANTED = 'granted';
const STATUS_DENIED = 'denied';

const RESERVATION_RESERVED = 'reserved';
const RESERVATION_DENIED_NO_SPACE = 'denied no space';
const RESERVATION_CLOSED = 'reservations closed';
const RESERVATION_RETURNED = 'returned';
const RESERVATION_RETURN_FAILED = 'unable to complete return';
const RESERVATION_FAILED = 'unable to complete reservation';
const RESERVATION_DENIED_CLASHING = 'denied other session clashes';

const RESULT_RESERVED = 'reserved';
const RESULT_RESERVED_NO_SPACE = 'reserve_denied_space';
const RESULT_RESERVED_CUTOFF = 'reserve_denied_cutoff';
const RESULT_RESERVED_CLASH = 'reserve_denied_clash';
const RESULT_RESERVED_FAILED = 'reserve_failed';

const RESULT_RETURNED = 'returned';
const RESULT_RETURNED_CUTOFF = 'return_denied_cutoff';
const RESULT_RETURNED_FAILED = 'return_failed';

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

  const request = event.data.val();
  const action = request[PATH_ACTION];
  const sid = request[PATH_SESSION];
  const uid = event.params.uid;

  return process(uid, sid, action).then(function(result) {
    console.log(action + ' with uid: ' + uid + ' and sid: ' + sid
        + ' ended with result: ' + result);
    return getQueueReference(uid).set({});
  });
});

/**
 * Process action (reserve or return) request by user with ID uid for session
 * with ID sid.
 *
 * @param uid ID of user requesting action (reserve or return).
 * @param sid Session ID of requested action (reserve or return).
 * @param action Intent of the request, to reserve or return reservation.
 * @returns {Promise.<TResult>} result of processing action.
 */
function process(uid, sid, action) {
  // Get session.
  return getSessionReference(sid).once('value')
      .then(function(snapshot) {
    return snapshot.val();
  }).then(function(curr_session) {
    // Check that are reservations still open.
    const now = new Date();
    if (curr_session.time_start - RES_CUT_OFF <= now.getTime()) {
      return handleCutoff(uid, sid, action);
    }

    if (action == ACTION_RESERVE) {
      return processReserve(uid, sid, curr_session);
    } else if (action == ACTION_RETURN) {
      return processReturn(uid, sid);
    }
  });
}

/**
 * Process a reservation request made by user with ID uid for a seat in session
 * with ID sid.
 *
 * @param uid ID of user requesting reservation.
 * @param sid Session ID of requested reservation.
 * @param curr_session Session body of requested reservation.
 * @returns {Promise.<TResult>} result of processing reservation.
 */
function processReserve(uid, sid, curr_session) {
  return checkForClash(uid, curr_session).then(function(clash) {
    if (clash) {
      // Clash with other session so reject.
      return handleClash(uid, sid);
    } else {
      // No clash so proceed with reservation request.
      return handleReservation(uid, sid);
    }
  });
}

/**
 * Process a return request of an existing reservation for user with ID uid
 * for a session with ID sid.
 *
 * @param uid ID of user requesting return.
 * @param sid Session ID of requested return.
 * @returns {Promise.<TResult>} result of processing return.
 */
function processReturn(uid, sid) {
  return getReservationStatusReference(uid, sid).once('value')
      .then(function(snapshot) {
    const currentStatus = snapshot.val();
    if (currentStatus == STATUS_GRANTED) {
      return handleReturn(uid, sid);
    } else {
      return RESERVATION_RETURN_FAILED;
    }
  });
}

/**
 * Check whether or not the user currently has any reservations that would
 * clash with the time of the session in the current reservation request.
 *
 * @param uid ID of user requesting reservation.
 * @param curr_session Session trying to be reserved.
 * @returns {Promise.<TResult>} result can be true or false.
 */
function checkForClash(uid, curr_session) {
  // Get all existing reservations for this user
  return admin.database().ref(PATH_SESSIONS)
      .orderByChild(PATH_RESERVATIONS + '/' + uid + '/' + PATH_STATUS)
      .equalTo(STATUS_GRANTED).once('value').then(function(snapshot) {
        const sessions = snapshot.val();
        for (var temp_session_id in sessions) {
          // Check for clash with existing reservation
          if (curr_session.time_start < sessions[temp_session_id].time_end &&
              curr_session.time_end > sessions[temp_session_id].time_start) {
            return true;
          }
        }
        return false;
      });
}

/**
 * Handle any requested action that is made after the reservation cutoff time.
 * Reservation alterations are only allowed RES_CUT_OFF millis before a session
 * starts.
 *
 * @param uid ID of user requesting action (reserve or return).
 * @param sid Session ID of requested action (reserve or return).
 * @param action String defining request type, reserve or return.
 * @returns {Promise.<TResult>} string result indicating reservations are closed.
 */
function handleCutoff(uid, sid, action) {
  if (action == ACTION_RESERVE) {
    return getReservationStatusReference(uid, sid).set(RESULT_RESERVED_CUTOFF)
        .then(function() {
          // Reservations are closed.
          return RESERVATION_CLOSED;
        });
  } else if (action == ACTION_RETURN) {
    return getReservationStatusReference(uid, sid).set(RESULT_RETURNED_CUTOFF)
        .then(function() {
          // Reservations are closed.
          return RESERVATION_CLOSED;
        });
  }
  return new Promise(function(res, rej) {
    res(RESERVATION_CLOSED);
  });
}

/**
 * Handle a return request. If a user requests a reservation for a session that
 * has already been granted, that reservation should be returned.
 *
 * @param uid ID of user returning reservation.
 * @param sid Session ID of return request.
 * @returns {Promise.<TResult>} string result of return.
 */
function handleReturn(uid, sid) {
  // Remove session
  return getSeatsReference(sid)
      .transaction(function(seats) {
    if (seats) {
      seats.reserved--;
    }
    return seats;
  }).then(function(result) {
    var committed = result.committed;
    var snapshot = result.snapshot;
    reservationResult = {};
    if (committed && snapshot != null) {
      reservationResult[PATH_RESULT] = RESULT_RETURNED;
      reservationResult[PATH_STATUS] = 'returned';
      return getReservationReference(uid, sid).set(reservationResult)
          .then(function() {
            return RESERVATION_RETURNED;
          });
    } else {
      return getReservationResultReference(uid, sid)
          .set(RESULT_RETURNED_FAILED).then(function() {
            return RESERVATION_RETURN_FAILED;
          });
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
  return getReservationResultReference(uid, sid).set(RESULT_RESERVED_CLASH)
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
  return getSeatsReference(sid).transaction(function(seats) {
    if (seats) {
      prevSeats.reserved = seats.reserved;
      // Add reservation if possible
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
      reservationResult = {};
      currSeats = snapshot.val();
      if (currSeats.reserved - 1 == prevSeats.reserved) {
        reservationResult[PATH_RESULT] = RESULT_RESERVED;
        reservationResult[PATH_STATUS] = STATUS_GRANTED;
        return getReservationReference(uid, sid).set(reservationResult)
            .then(function() {
              return RESERVATION_RESERVED;
            });
      } else {
        reservationResult[PATH_RESULT] = RESULT_RESERVED_NO_SPACE;
        reservationResult[PATH_STATUS] = STATUS_DENIED;
        return getReservationReference(uid, sid).set(reservationResult)
            .then(function() {
              return RESERVATION_DENIED_NO_SPACE;
            });
      }
    } else {
      return getReservationResultReference(uid, sid)
          .set(RESULT_RESERVED_FAILED).then(function() {
            return RESERVATION_FAILED;
          });
    }
  });
}

// Helper functions for getting database references.
function getSessionReference(sid) {
  return admin.database().ref(PATH_SESSIONS).child(sid);
}

function getSeatsReference(sid) {
  return getSessionReference(sid).child(PATH_SEATS);
}

function getReservationReference(uid, sid) {
  return getSessionReference(sid).child(PATH_RESERVATIONS).child(uid);
}

function getReservationStatusReference(uid, sid) {
  return getReservationReference(uid, sid).child(PATH_STATUS);
}

function getReservationResultReference(uid, sid) {
  return getReservationReference(uid, sid).child(PATH_RESULT);
}

function getQueueReference(uid) {
  return admin.database().ref(PATH_QUEUE).child(uid);
}
