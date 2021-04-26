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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.material.composethemeadapter.MdcTheme
import com.google.samples.apps.iosched.BuildConfig
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.model.Theme
import com.google.samples.apps.iosched.ui.util.GoogleSansMediumFamily
import com.google.samples.apps.iosched.ui.util.IoschedWebViewDialog
import com.google.samples.apps.iosched.ui.util.rememberFlowWithLifecycle
import com.google.samples.apps.iosched.util.openWebsiteUrl
import kotlinx.coroutines.flow.collect

/**
 * Compose code that emits the UI that goes into the content of the Settings screen.
 *
 * To learn more about Jetpack Compose, check out the docs: goo.gle/compose-docs
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    // Internal state for showing the choose theme and the oss licenses dialogs
    var showChooseThemeDialog by rememberSaveable { mutableStateOf(false) }
    var showWebViewDialog: ShowWebViewDialog by rememberSaveable(
        stateSaver = showWebViewDialogSaver
    ) {
        mutableStateOf(doNotShowWebViewDialog)
    }

    Box(Modifier.fillMaxSize()) {
        val context = LocalContext.current
        SettingsScreenContent(
            viewModel = viewModel,
            openWebsiteLink = { url -> openWebsiteUrl(context, url) },
            openDialog = { title, url -> showWebViewDialog = ShowWebViewDialog(title, url) }
        )

        // Show the choose theme dialog if needed
        if (showChooseThemeDialog) {
            val availableThemes by rememberFlowWithLifecycle(viewModel.availableThemes)
                .collectAsState(listOf())
            val selectedTheme by rememberFlowWithLifecycle(viewModel.theme)
                .collectAsState(Theme.SYSTEM)

            ThemeSettingDialog(
                onDismissRequest = { showChooseThemeDialog = false },
                onThemeSelected = {
                    viewModel.setTheme(it)
                    showChooseThemeDialog = false
                },
                availableThemes = availableThemes,
                selectedTheme = selectedTheme
            )
        }

        // Show the oss licenses dialog if needed
        if (showWebViewDialog != doNotShowWebViewDialog) {
            IoschedWebViewDialog(
                title = showWebViewDialog.title,
                url = showWebViewDialog.url,
                onDismissRequest = { showWebViewDialog = doNotShowWebViewDialog }
            )
        }
    }

    // Listen for navigation events
    val navigationActions = rememberFlowWithLifecycle(viewModel.navigationActions)
    LaunchedEffect(navigationActions) {
        navigationActions.collect {
            if (it is SettingsNavigationAction.NavigateToThemeSelector) {
                showChooseThemeDialog = true
            }
        }
    }
}

@Composable
private fun SettingsScreenContent(
    viewModel: SettingsViewModel,
    openWebsiteLink: (String) -> Unit,
    openDialog: (String, String) -> Unit
) {
    val uiState by rememberFlowWithLifecycle(viewModel.uiState).collectAsState(
        SettingsUiModel(isLoading = true)
    )

    if (uiState.isLoading) {
        Box(Modifier.fillMaxWidth()) {
            CircularProgressIndicator(
                Modifier
                    .padding(top = dimensionResource(R.dimen.margin_large))
                    .align(Alignment.Center)
            )
        }
    } else {
        Column(Modifier.padding(vertical = dimensionResource(R.dimen.margin_normal))) {
            SettingsSection(
                onThemeSettingClicked = { viewModel.onThemeSettingClicked() },
                preferConferenceTimeZone = uiState.preferConferenceTimeZone,
                onPreferConferenceTimeZoneCheck = { viewModel.setTimeZone(it) },
                enableNotifications = uiState.enableNotifications,
                onEnableNotificationsCheck = { viewModel.setEnableNotifications(it) },
                sendUsageStatistics = uiState.sendUsageStatistics,
                onSendUsageStatisticsCheck = { viewModel.setSendUsageStatistics(it) }
            )
            Divider(Modifier.fillMaxWidth())
            Spacer(Modifier.height(dimensionResource(R.dimen.spacing_normal)))
            AboutSection(openWebsiteLink, openDialog)
        }
    }
}

@Composable
private fun SettingsSection(
    onThemeSettingClicked: () -> Unit,
    preferConferenceTimeZone: Boolean,
    onPreferConferenceTimeZoneCheck: (Boolean) -> Unit,
    enableNotifications: Boolean,
    onEnableNotificationsCheck: (Boolean) -> Unit,
    sendUsageStatistics: Boolean,
    onSendUsageStatisticsCheck: (Boolean) -> Unit,
) {
    ChooseTheme(
        onClick = onThemeSettingClicked,
        modifier = Modifier
            .padding(dimensionResource(R.dimen.margin_small))
            .fillMaxWidth()
    )

    SwitchSetting(
        text = stringResource(R.string.settings_time_zone_label),
        checked = preferConferenceTimeZone,
        onCheck = onPreferConferenceTimeZoneCheck
    )
    SwitchSetting(
        text = stringResource(R.string.settings_enable_notifications),
        checked = enableNotifications,
        onCheck = onEnableNotificationsCheck
    )
    SwitchSetting(
        text = stringResource(R.string.settings_send_anonymous_usage_statistics),
        checked = sendUsageStatistics,
        onCheck = onSendUsageStatisticsCheck
    )
}

@Composable
private fun ChooseTheme(onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colors.onSurface
        ),
        onClick = onClick
    ) {
        Text(
            text = stringResource(R.string.settings_theme_title),
            style = MaterialTheme.typography.body2,
            modifier = modifier
        )
    }
}

@Composable
private fun AboutSection(
    openWebsiteLink: (String) -> Unit,
    openDialog: (String, String) -> Unit
) {
    Text(
        text = stringResource(R.string.about_title).toUpperCase(
            LocaleListCompat.getDefault().get(0)
        ),
        style = MaterialTheme.typography.body2,
        fontFamily = GoogleSansMediumFamily,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
        modifier = Modifier
            .semantics { heading() }
            .padding(dimensionResource(R.dimen.margin_normal))
    )

    val tosUrl = stringResource(R.string.tos_url)
    TextButton(
        modifier = Modifier.padding(dimensionResource(R.dimen.margin_small)),
        onClick = {
            openWebsiteLink(tosUrl)
        }
    ) {
        LinkText(stringResource(R.string.settings_tos))
    }

    val privacyPolicyUrl = stringResource(R.string.privacy_policy_url)
    TextButton(
        modifier = Modifier.padding(dimensionResource(R.dimen.margin_small)),
        onClick = {
            openWebsiteLink(privacyPolicyUrl)
        }
    ) {
        LinkText(stringResource(R.string.settings_privacy_policy))
    }

    val ossLicensesTitle = stringResource(R.string.settings_oss_licenses)
    val ossLicensesFile = stringResource(R.string.oss_file)
    TextButton(
        modifier = Modifier.padding(dimensionResource(R.dimen.margin_small)),
        onClick = {
            openDialog(ossLicensesTitle, ossLicensesFile)
        }
    ) {
        LinkText(stringResource(R.string.settings_oss_licenses))
    }

    Text(
        text = stringResource(R.string.version_name, BuildConfig.VERSION_NAME),
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 48.dp)
            .padding(dimensionResource(R.dimen.margin_normal)),
        style = MaterialTheme.typography.body2,
    )

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(dimensionResource(R.dimen.margin_normal))
            .fillMaxWidth()
    ) {
        Image(
            painter = painterResource(R.drawable.ic_logo_components),
            contentDescription = null // Descriptive element
        )
        Spacer(Modifier.width(dimensionResource(R.dimen.margin_small)))
        Text(stringResource(R.string.built_with_material_components))
    }
}

@Composable
private fun LinkText(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.body1,
        fontFamily = GoogleSansMediumFamily
    )
}

@Composable
private fun SwitchSetting(
    text: String,
    checked: Boolean,
    onCheck: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheck
            )
            .padding(dimensionResource(R.dimen.margin_normal))
    ) {
        Text(
            text = text,
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically),
            style = MaterialTheme.typography.body2
        )
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colors.primary
            )
        )
    }
}

private data class ShowWebViewDialog(val title: String, val url: String)
private val showWebViewDialogSaver: Saver<ShowWebViewDialog, *> = listSaver(
    save = { listOf(it.title, it.url) },
    restore = { ShowWebViewDialog(it[0], it[1]) }
)
// A `Saver` doesn't accept nullable values so `null` cannot be used to indicate the absence of the
// dialog on the screen. This instance is used for that instead.
private val doNotShowWebViewDialog = ShowWebViewDialog("", "")

@Preview
@Composable
private fun SettingsSectionPreview() {
    MdcTheme {
        Surface {
            Column {
                SettingsSection(
                    onThemeSettingClicked = { },
                    preferConferenceTimeZone = true,
                    onPreferConferenceTimeZoneCheck = { },
                    enableNotifications = false,
                    onEnableNotificationsCheck = { },
                    sendUsageStatistics = false,
                    onSendUsageStatisticsCheck = { }
                )
            }
        }
    }
}

@Preview
@Composable
private fun AboutPreview() {
    MdcTheme {
        Surface {
            Column {
                AboutSection({}, { _, _ -> })
            }
        }
    }
}
