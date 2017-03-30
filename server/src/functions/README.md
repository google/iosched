# Reservation System
Reservations for I/O are processed in Firebase using RTDB and Firebase Functions.

## RTDB schema

    - queue
        - <user id>: <session id>
    - sessions
        - <session_id>
            - time_start: <start time in millis>
            - time_end: <end time in millis>
            - seats
                - capacity: <total number of seats in room>
                - reserved: <number of seats currently reserved>
            - reservations
                - <user id>: <reservation status>

## RTDB Rules
    {
      "rules": {
        "queue": {
          "$uid": {
            ".read": "auth != null && auth.uid == $uid",
          	".write": "auth != null && auth.uid == $uid && !data.exists() && root.child('/users').child(auth.uid).val() == true"
          }
        },
        "sessions": {
          "$sid": {
            "seats": {
              "seats_available": {
                ".read": true,
                ".write" false
              }
            },
            "reservations": {
              "$uid": {
                ".read": "auth != null && auth.uid == $uid",
                ".write": false
              }
            }
          }
        },
        "users": {
          "$uid": {
            ".write": false,
            ".read": false
          }
        }
      }
    }

## Reservation Request Processing
Client applications are allowed to write one reservation request at a time to the queue.
A function listens for new requests being written to the queue. The function checks that
the request is valid and if so it is then processed. A request is valid if:
- The session exists
- Reservations for the session are still open, reservations are closed some time before
  the session starts.
- The user requesting the reservation does not have any existing reservations at the same
  time.

Valid reservations are granted if there are available seats in the session. Each reservation
request is processed in a transaction. The available seats are updated if necessary in the
transaction.

If a user requests a reservation for a session that has already been reserved by the user,
the reservation is "returned" and the seat is made available for reservation.

## Clients
Client applications are expected to write their requests to the queue. The client should
consider the request as processing until the request is deleted. Then the result should be
read from:

    /sessions/<session id>/reservations/<user id>: <result>