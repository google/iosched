/*
 * Copyright (c) 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.samples.apps.iosched.info.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.google.samples.apps.iosched.info.BaseInfoFragment;
import com.google.samples.apps.iosched.lib.BuildConfig;
import com.google.samples.apps.iosched.lib.R;
import com.google.samples.apps.iosched.settings.SettingsUtils;
import com.google.samples.apps.iosched.util.AboutUtils;

public class SettingsFragment extends BaseInfoFragment<Object> {

    TextView mTermsOfService;
    TextView mPrivacyPolicy;
    TextView mOpenSourceLicenses;
    TextView mAppVersion;
    Switch mTimeZoneSetting;
    Switch mNotificationsSetting;
    Switch mAnonStatisticsSetting;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.info_settings_frag, container, false);
        mTermsOfService = (TextView) root.findViewById(R.id.termsOfServiceLink);
        mPrivacyPolicy = (TextView) root.findViewById(R.id.privacyPolicyLink);
        mOpenSourceLicenses = (TextView) root.findViewById(R.id.openSourceLicensesLink);
        mAppVersion = (TextView) root.findViewById(R.id.appVersion);
        mTimeZoneSetting = (Switch) root.findViewById(R.id.settingsTimeZoneSwitch);
        mNotificationsSetting = (Switch) root.findViewById(R.id.settingsNotificationsSwitch);
        mAnonStatisticsSetting = (Switch) root.findViewById(R.id.settingsAnonStatisticsSwitch);
        return root;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mNotificationsSetting.setChecked(SettingsUtils.shouldShowNotifications(getContext()));
        mTimeZoneSetting.setChecked(!SettingsUtils.isUsingLocalTime(getContext()));
        mTermsOfService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent termsLink = new Intent(Intent.ACTION_VIEW);
                termsLink.setData(Uri.parse(getResources()
                        .getString(R.string.about_terms_url)));
                startActivity(termsLink);
            }
        });
        mPrivacyPolicy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent privacyPolicyLink = new Intent(Intent.ACTION_VIEW);
                privacyPolicyLink.setData(Uri.parse(getResources()
                        .getString(R.string.about_privacy_policy_url)));
                startActivity(privacyPolicyLink);
            }
        });
        mOpenSourceLicenses.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AboutUtils.showOpenSourceLicenses(getActivity());
            }
        });
        mAppVersion.setText(getResources()
                .getString(R.string.about_app_version, BuildConfig.VERSION_NAME));
        mNotificationsSetting.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        SettingsUtils.setShowNotifications(getContext(), isChecked);
                    }
                });
        mTimeZoneSetting.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SettingsUtils.setUsingLocalTime(getContext(), !isChecked);
            }
        });
    }

    @Override
    public String getTitle(@NonNull Resources resources) {
        return resources.getString(R.string.title_settings);
    }

    public void updateInfo(Object info) {
    }

    @Override
    protected void showInfo() {
    }
}