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
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.samples.apps.iosched.info.BaseInfoFragment;
import com.google.samples.apps.iosched.lib.BuildConfig;
import com.google.samples.apps.iosched.lib.R;

public class SettingsFragment extends BaseInfoFragment {

    TextView mTermsOfService;
    TextView mPrivacyPolicy;
    TextView mAppVersion;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.info_settings_frag, container, false);
        mTermsOfService = (TextView) root.findViewById(R.id.termsOfServiceLink);
        mPrivacyPolicy = (TextView) root.findViewById(R.id.privacyPolicyLink);
        mAppVersion = (TextView) root.findViewById(R.id.appVersion);
        return root;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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
        mAppVersion.setText(getResources()
                .getString(R.string.about_app_version, BuildConfig.VERSION_NAME));
    }

    @Override
    public String getTitle(@NonNull Resources resources) {
        return resources.getString(R.string.title_settings);
    }
}