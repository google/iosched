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
import android.text.Html;
import android.view.View;
import android.widget.TextView;

import com.google.samples.apps.iosched.BuildConfig;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.navigation.NavigationModel;
import com.google.samples.apps.iosched.ui.BaseActivity;
import com.google.samples.apps.iosched.ui.widget.DrawShadowFrameLayout;
import com.google.samples.apps.iosched.util.AboutUtils;
import com.google.samples.apps.iosched.util.UIUtils;

public class AboutActivity extends BaseActivity {

    private static final String URL_TERMS = "http://m.google.com/utos";
    private static final String URL_PRIVACY_POLICY = "http://www.google.com/policies/privacy/";

    private View rootView;

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.about_terms:
                    openUrl(URL_TERMS);
                    break;
                case R.id.about_privacy_policy:
                    openUrl(URL_PRIVACY_POLICY);
                    break;
                case R.id.about_licenses:
                    AboutUtils.showOpenSourceLicenses(AboutActivity.this);
                    break;
                case R.id.about_eula:
                    AboutUtils.showEula(AboutActivity.this);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        rootView = findViewById(R.id.about_container);

        TextView body = (TextView) rootView.findViewById(R.id.about_main);
        body.setText(Html.fromHtml(getString(R.string.about_main, BuildConfig.VERSION_NAME)));
        rootView.findViewById(R.id.about_terms).setOnClickListener(mOnClickListener);
        rootView.findViewById(R.id.about_privacy_policy).setOnClickListener(mOnClickListener);
        rootView.findViewById(R.id.about_licenses).setOnClickListener(mOnClickListener);
        rootView.findViewById(R.id.about_eula).setOnClickListener(mOnClickListener);

        overridePendingTransition(0, 0);
    }


    @Override
    protected NavigationModel.NavigationItemEnum getSelfNavDrawerItem() {
        return NavigationModel.NavigationItemEnum.ABOUT;
    }

    private void setContentTopClearance(int clearance) {
        if (rootView != null) {
            rootView.setPadding(rootView.getPaddingLeft(), clearance,
                    rootView.getPaddingRight(), rootView.getPaddingBottom());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        int actionBarSize = UIUtils.calculateActionBarSize(this);
        DrawShadowFrameLayout drawShadowFrameLayout =
                (DrawShadowFrameLayout) findViewById(R.id.main_content);
        if (drawShadowFrameLayout != null) {
            drawShadowFrameLayout.setShadowTopOffset(actionBarSize);
        }
        setContentTopClearance(actionBarSize);
    }

    private void openUrl(String url) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

}
