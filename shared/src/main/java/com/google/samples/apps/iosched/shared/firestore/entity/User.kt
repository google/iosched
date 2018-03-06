package com.google.samples.apps.iosched.shared.firestore.entity

/**
 * Data for a user stored in a Firestore document. The events which have been personalized
 * by the user correspond to the `events` subcollection.
 */
data class User(
        /**
         * The unique id for the user. Corresponds to the value returned by
         * https://developers.google.com/android/reference/com/google/android/gms/auth/api/signin/GoogleSignInAccount.html#getId()
         */
        val uid: String,

        /**
         * The token that identifies the user's devices so that Firebase Messaging can send
         * downstream messages to those devices. Corresponds to the value returned by calling
         * https://firebase.google.com/docs/reference/android/com/google/firebase/iid/FirebaseInstanceId.html#getToken(java.lang.String, java.lang.String).
         */
        val fcmIds: List<String>,

        /**
         * Tracks whether the user is an attendee or a remote user.
         */
        val isRegistered: Boolean,

        /**
         * Corresponds to the Firestore subcollection of documents that sture the user's events data.
         */
        val events: List<UserEvent>
)
