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

package com.google.samples.apps.iosched.myio;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.Guideline;
import android.support.v4.app.DialogFragment;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.google.samples.apps.iosched.lib.R;
import com.google.samples.apps.iosched.sync.account.Account;
import com.google.samples.apps.iosched.util.AccountUtils;
import com.google.samples.apps.iosched.util.WelcomeUtils;

import static android.view.View.GONE;

/**
 * DialogFragment that handles auth on the My I/O screen.
 */
public class MyIODialogFragment extends DialogFragment {

    public MyIODialogFragment() {
        // Required constructor.
    }

    public static MyIODialogFragment newInstance() {
        return new MyIODialogFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.my_io_dialog_frag, container);

        TextView name = (TextView) view.findViewById(R.id.name);
        TextView email = (TextView) view.findViewById(R.id.email);
        final ImageView avatar = (ImageView) view.findViewById(R.id.avatar);
        TextView bodyIntro = (TextView) view.findViewById(R.id.body_intro);
        Button authButton = (Button) view.findViewById(R.id.auth_button);

        // Note: this may be null if the user has not set up a profile photo.
        Uri url = AccountUtils.getActiveAccountPhotoUrl(getActivity());
        boolean signedIn = AccountUtils.hasActiveAccount(getActivity());

        if (signedIn) {
            name.setText(AccountUtils.getActiveAccountDisplayName(getActivity()));
            email.setText(AccountUtils.getActiveAccountName(getActivity()));
            // A default avatar is already specified in XML. This substitutes that default with the
            // photo associated with the account.
            if (url != null) {
                final Context context = getActivity().getApplicationContext();
                // TODO: refactor.
                Glide.with(context)
                        .load(url.toString())
                        .asBitmap()
                        .fitCenter()
                        .into(new BitmapImageViewTarget(avatar) {
                            @Override
                            protected void setResource(Bitmap resource) {
                                RoundedBitmapDrawable circularBitmapDrawable =
                                        RoundedBitmapDrawableFactory
                                                .create(context.getResources(), resource);
                                circularBitmapDrawable.setCircular(true);
                                avatar.setImageDrawable(circularBitmapDrawable);
                            }
                        });
            }
            bodyIntro.setText(R.string.my_io_body_intro_signed_in);
        } else {
            avatar.setVisibility(GONE);
            name.setVisibility(GONE);
            email.setVisibility(GONE);
            view.findViewById(R.id.signed_in_circle_check).setVisibility(GONE);
            view.findViewById(R.id.divider).setVisibility(GONE);
            ((ConstraintLayout.LayoutParams) view.findViewById(R.id.guide_header).getLayoutParams())
                    .guideBegin = 0;
            bodyIntro.setText(R.string.my_io_body_intro_signed_out);
        }

        authButton.setText(signedIn ? getResources().getString(R.string.signout_prompt) :
                getResources().getString(R.string.signin_prompt));
        authButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                MyIOActivity myIOActivity = ((MyIOActivity) getActivity());
                if (AccountUtils.hasActiveAccount(getActivity())) {
                    myIOActivity.signOut();
                } else {
                    myIOActivity.signIn();
                }
                dismiss();
            }
        });
        return view;
    }
}