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

package com.google.samples.apps.iosched.ui.schedule

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.shared.domain.invoke
import com.google.samples.apps.iosched.shared.domain.prefs.MarkScheduleUiHintsShownUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Dialog that shows the hints for the schedule.
 */
@AndroidEntryPoint
class ScheduleUiHintsDialogFragment : AppCompatDialogFragment() {

    @Inject
    lateinit var markScheduleUiHintsShownUseCase: MarkScheduleUiHintsShownUseCase

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.schedule_hint_title)
            .setView(R.layout.dialog_schedule_hints)
            .setPositiveButton(R.string.got_it, null)
            .create()
    }

    override fun onDismiss(dialog: DialogInterface) {
        markScheduleUiHintsShownUseCase()
        super.onDismiss(dialog)
    }
}
