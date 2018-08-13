/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.iosched.ui.sessioncommon

import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.shared.data.userevent.UserEventMessageChangeType
import com.google.samples.apps.iosched.shared.data.userevent.UserEventMessageChangeType.CANCELLATION_DENIED_CUTOFF
import com.google.samples.apps.iosched.shared.data.userevent.UserEventMessageChangeType.CANCELLATION_DENIED_UNKNOWN
import com.google.samples.apps.iosched.shared.data.userevent.UserEventMessageChangeType.CHANGES_IN_RESERVATIONS
import com.google.samples.apps.iosched.shared.data.userevent.UserEventMessageChangeType.CHANGES_IN_WAITLIST
import com.google.samples.apps.iosched.shared.data.userevent.UserEventMessageChangeType.RESERVATIONS_REPLACED
import com.google.samples.apps.iosched.shared.data.userevent.UserEventMessageChangeType.RESERVATION_CANCELED
import com.google.samples.apps.iosched.shared.data.userevent.UserEventMessageChangeType.RESERVATION_DENIED_CLASH
import com.google.samples.apps.iosched.shared.data.userevent.UserEventMessageChangeType.RESERVATION_DENIED_CUTOFF
import com.google.samples.apps.iosched.shared.data.userevent.UserEventMessageChangeType.RESERVATION_DENIED_UNKNOWN
import com.google.samples.apps.iosched.shared.data.userevent.UserEventMessageChangeType.WAITLIST_CANCELED

fun UserEventMessageChangeType.stringRes(): Int {
    return when (this) {
        CHANGES_IN_RESERVATIONS -> R.string.reservation_new
        RESERVATIONS_REPLACED -> R.string.reservation_replaced
        CHANGES_IN_WAITLIST -> R.string.waitlist_new
        RESERVATION_CANCELED -> R.string.reservation_cancel_succeeded
        WAITLIST_CANCELED -> R.string.waitlist_cancel_succeeded
        RESERVATION_DENIED_CUTOFF -> R.string.reservation_denied_cutoff
        RESERVATION_DENIED_CLASH -> R.string.reservation_denied_clash
        RESERVATION_DENIED_UNKNOWN -> R.string.reservation_denied_unknown
        CANCELLATION_DENIED_CUTOFF -> R.string.cancellation_denied_cutoff
        CANCELLATION_DENIED_UNKNOWN -> R.string.cancellation_denied_unknown
    }
}
