/*
 * Copyright 2015 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.iosched.about;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

import com.google.samples.apps.iosched.lib.BuildConfig;
import com.google.samples.apps.iosched.lib.R;
import com.google.samples.apps.iosched.util.AboutUtils;

public class AboutActivity extends AppCompatActivity {

    private static final String URL_TERMS = "http://m.google.com/utos";
    private static final String URL_PRIVACY_POLICY = "http://www.google.com/policies/privacy/";

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int viewId = v.getId();
            if (viewId == R.id.about_terms) {
                openUrl(URL_TERMS);
            } else if (viewId == R.id.about_privacy_policy) {
                openUrl(URL_PRIVACY_POLICY);
            } else if (viewId == R.id.about_licenses) {
                AboutUtils.showOpenSourceLicenses(AboutActivity.this);
            } else if (viewId == R.id.about_eula) {
                AboutUtils.showEula(AboutActivity.this);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        TextView body = (TextView) findViewById(R.id.about_main);
        body.setText(Html.fromHtml(getString(R.string.about_main, BuildConfig.VERSION_NAME)));
        findViewById(R.id.about_terms).setOnClickListener(mOnClickListener);
        findViewById(R.id.about_privacy_policy).setOnClickListener(mOnClickListener);
        findViewById(R.id.about_licenses).setOnClickListener(mOnClickListener);
        findViewById(R.id.about_eula).setOnClickListener(mOnClickListener);

        overridePendingTransition(0, 0);
    }


    private void openUrl(String url) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }
}
