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

package com.google.samples.apps.iosched.social;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.ui.widget.DrawShadowFrameLayout;
import com.google.samples.apps.iosched.util.UIUtils;

import com.google.samples.apps.iosched.social.SocialModel.SocialLinksEnum;

/**
 * Displays links for navigating to social media channels.
 */
public class SocialFragment extends Fragment {
    SocialModel mModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.social_frag, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mModel = new SocialModel(getActivity());
        initViewListeners();
    }

    /**
     * Sets up listeners for social media panels.
     */
    private void initViewListeners() {
        ViewGroup io15Panel = (ViewGroup) getActivity().findViewById(R.id.io15_panel);
        setUpSocialIcons(io15Panel, SocialLinksEnum.GPLUS_IO15, SocialLinksEnum.TWITTER_IO15);

        TextView socialGplusDevs = (TextView) getActivity().findViewById(R.id.social_gplus_devs);
        socialGplusDevs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UIUtils.fireSocialIntent(
                        getActivity(),
                        SocialLinksEnum.GPLUS_DEVS.getUri(),
                        UIUtils.GOOGLE_PLUS_PACKAGE_NAME);
            }
        });

        TextView socialTwitterDevs = (TextView) getActivity().findViewById(
                R.id.social_twitter_devs);
        socialTwitterDevs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UIUtils.fireSocialIntent(
                        getActivity(),
                        SocialLinksEnum.TWITTER_DEVS.getUri(),
                        UIUtils.TWITTER_PACKAGE_NAME
                );
            }
        });

        ViewGroup extendedPanel = (ViewGroup) getActivity().findViewById(R.id.extended_panel);
        setUpSocialIcons(extendedPanel, SocialLinksEnum.GPLUS_EXTENDED,
                SocialLinksEnum.TWITTER_EXTENDED);

        ViewGroup requestPanel = (ViewGroup) getActivity().findViewById(R.id.request_panel);
        // Make the "Request" panel visible only a few days before I/O.
        if (UIUtils.getCurrentTime(getActivity()) < Config.SHOW_IO15_REQUEST_SOCIAL_PANEL_TIME) {
            requestPanel.setVisibility(View.GONE);
        } else {
            setUpSocialIcons(requestPanel, SocialLinksEnum.GPLUS_REQUEST,
                    SocialLinksEnum.TWITTER_REQUEST);
            requestPanel.setVisibility(View.VISIBLE);
        }
        setupLogoAnim();
    }

    /**
     * Adds listeners to a panel to open the G+ and Twitter apps via an intent.
     */
    private void setUpSocialIcons(final View panel, final SocialLinksEnum gPlusValue,
                                  final SocialLinksEnum twitterValue) {

        final View twitterIconBox = panel.findViewById(R.id.twitter_icon_box);
        twitterIconBox.setContentDescription(mModel.getContentDescription(twitterValue));

        twitterIconBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UIUtils.fireSocialIntent(
                        getActivity(),
                        twitterValue.getUri(),
                        UIUtils.TWITTER_PACKAGE_NAME
                );
            }
        });

        final View gPlusIconBox = panel.findViewById(R.id.gplus_icon_box);
        gPlusIconBox.setContentDescription(mModel.getContentDescription(gPlusValue));

        gPlusIconBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UIUtils.fireSocialIntent(
                        getActivity(),
                        gPlusValue.getUri(),
                        UIUtils.GOOGLE_PLUS_PACKAGE_NAME
                );
            }
        });
    }

    private void setContentTopClearance(int clearance) {
        if (getView() != null) {
            getView().setPadding(getView().getPaddingLeft(), clearance,
                    getView().getPaddingRight(), getView().getPaddingBottom());
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        getActivity().invalidateOptionsMenu();

        // Configure the fragment's top clearance to take our overlaid controls (Action Bar
        // and spinner box) into account.
        int actionBarSize = UIUtils.calculateActionBarSize(getActivity());
        DrawShadowFrameLayout drawShadowFrameLayout =
                (DrawShadowFrameLayout) getActivity().findViewById(R.id.main_content);
        if (drawShadowFrameLayout != null) {
            drawShadowFrameLayout.setShadowTopOffset(actionBarSize);
        }
        setContentTopClearance(actionBarSize);
    }

    @TargetApi(21)
    private void setupLogoAnim() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final ImageView iv = (ImageView) getActivity().findViewById(R.id.io_logo);
            final AnimatedVectorDrawable logoAnim =
                    (AnimatedVectorDrawable) getActivity().getDrawable(
                            R.drawable.io_logo_social_anim);
            iv.setImageDrawable(logoAnim);
            logoAnim.start();
            iv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    logoAnim.start();
                }
            });
        }
    }
}
