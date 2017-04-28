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
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.TextView;

import com.google.samples.apps.iosched.info.BaseInfoFragment;
import com.google.samples.apps.iosched.lib.BuildConfig;
import com.google.samples.apps.iosched.lib.R;
import com.google.samples.apps.iosched.settings.SettingsUtils;
import com.google.samples.apps.iosched.util.AboutUtils;

public class SettingsFragment extends BaseInfoFragment<Object> {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.info_settings_frag, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Links
        view.findViewById(R.id.terms_of_service_link).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent termsLink = new Intent(Intent.ACTION_VIEW);
                termsLink.setData(Uri.parse(getResources()
                        .getString(R.string.about_terms_url)));
                startActivity(termsLink);
            }
        });
        view.findViewById(R.id.privacy_policy_link).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent privacyPolicyLink = new Intent(Intent.ACTION_VIEW);
                privacyPolicyLink.setData(Uri.parse(getResources()
                        .getString(R.string.about_privacy_policy_url)));
                startActivity(privacyPolicyLink);
            }
        });
        view.findViewById(R.id.open_source_licenses_link).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                AboutUtils.showOpenSourceLicenses(getActivity());
            }
        });

        // Version
        TextView appVersion = (TextView) view.findViewById(R.id.app_version);
        appVersion.setText(getResources()
                .getString(R.string.about_app_version, BuildConfig.VERSION_NAME));

        // Switches
        setupSettingsSwitch(R.id.settings_timezone_container,
                R.id.settings_timezone_label,
                R.id.settings_timezone_switch,
                !SettingsUtils.isUsingLocalTime(getContext()),
                new OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        SettingsUtils.setUsingLocalTime(getContext(), !isChecked);
                    }
                });
        setupSettingsSwitch(R.id.settings_notifications_container,
                R.id.settings_notifications_label,
                R.id.settings_notifications_switch,
                SettingsUtils.shouldShowNotifications(getContext()),
                new OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        SettingsUtils.setShowNotifications(getContext(), isChecked);
                    }
                });
        setupSettingsSwitch(R.id.settings_anon_statistics_container,
                R.id.settings_anon_statistics_label,
                R.id.settings_anon_statistics_switch,
                false, // TODO not implemented
                new OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        // TODO not implemented
                    }
                });
    }

    private void setupSettingsSwitch(int containerId, int labelId, int switchId, boolean checked,
            OnCheckedChangeListener checkedChangeListener) {
        ViewGroup container = (ViewGroup) getView().findViewById(containerId);
        String switchLabel = ((TextView) container.findViewById(labelId)).getText().toString();
        final Switch switchView = (Switch) container.findViewById(switchId);
        switchView.setContentDescription(switchLabel);
        switchView.setChecked(checked);
        container.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                switchView.performClick();
            }
        });
        switchView.setOnCheckedChangeListener(checkedChangeListener);
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
