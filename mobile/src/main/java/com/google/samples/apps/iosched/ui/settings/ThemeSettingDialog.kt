/*
 * Copyright 2021 Google LLC
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

package com.google.samples.apps.iosched.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.RadioButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.google.android.material.composethemeadapter.MdcTheme
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.model.Theme
import com.google.samples.apps.iosched.ui.util.IoschedDialog

@Composable
fun ThemeSettingDialog(
    onDismissRequest: () -> Unit,
    onThemeSelected: (Theme) -> Unit,
    availableThemes: List<Theme>,
    selectedTheme: Theme,
    modifier: Modifier = Modifier
) {
    IoschedDialog(
        title = stringResource(R.string.settings_theme_title),
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        ThemeDialogContent(onThemeSelected, availableThemes, selectedTheme)
    }
}

@Composable
private fun ThemeDialogContent(
    onThemeSelected: (Theme) -> Unit,
    availableThemes: List<Theme>,
    selectedTheme: Theme
) {
    Column(Modifier.padding(top = dimensionResource(R.dimen.margin_normal))) {
        Column(Modifier.selectableGroup()) {
            availableThemes.forEach { theme ->
                Row(
                    modifier = Modifier
                        .selectable(
                            selected = theme == selectedTheme,
                            onClick = { onThemeSelected(theme) },
                            role = Role.RadioButton
                        )
                        .padding(dimensionResource(R.dimen.margin_normal)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = getTitleForTheme(theme),
                        modifier = Modifier
                            .weight(1f)
                            .padding()
                    )
                    RadioButton(
                        selected = theme == selectedTheme,
                        onClick = null // null recommended for accessibility with screenreaders
                    )
                }
            }
        }
    }
}

@Composable
private fun getTitleForTheme(theme: Theme) = when (theme) {
    Theme.LIGHT -> stringResource(R.string.settings_theme_light)
    Theme.DARK -> stringResource(R.string.settings_theme_dark)
    Theme.SYSTEM -> stringResource(R.string.settings_theme_system)
    Theme.BATTERY_SAVER -> stringResource(R.string.settings_theme_battery)
}

@Preview
@Composable
private fun ChooseThemeDialogPreview() {
    MdcTheme {
        Surface {
            ThemeSettingDialog({}, {}, Theme.values().toList(), Theme.LIGHT)
        }
    }
}
