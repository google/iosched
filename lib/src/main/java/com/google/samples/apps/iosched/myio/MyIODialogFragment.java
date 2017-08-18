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

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.app.AlertDialog;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BulletSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.google.samples.apps.iosched.lib.R;
import com.google.samples.apps.iosched.util.AccountUtils;

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

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        boolean signedIn = AccountUtils.hasActiveAccount(context);
        if (signedIn) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View titleView = inflater.inflate(R.layout.myio_auth_dialog_signedin_title, null);
            TextView name = (TextView) titleView.findViewById(R.id.name);
            TextView email = (TextView) titleView.findViewById(R.id.email);
            final ImageView avatar = (ImageView) titleView.findViewById(R.id.avatar);

            name.setText(AccountUtils.getActiveAccountDisplayName(context));
            email.setText(AccountUtils.getActiveAccountName(context));

            // Note: this may be null if the user has not set up a profile photo.
            Uri url = AccountUtils.getActiveAccountPhotoUrl(context);
            if (url != null) {
                Glide.with(context)
                        .load(url.toString())
                        .asBitmap()
                        .fitCenter()
                        .placeholder(R.drawable.ic_default_avatar)
                        .into(new BitmapImageViewTarget(avatar) {
                            @Override
                            protected void setResource(Bitmap resource) {
                                RoundedBitmapDrawable roundedBmp = RoundedBitmapDrawableFactory
                                        .create(context.getResources(), resource);
                                roundedBmp.setCircular(true);
                                avatar.setImageDrawable(roundedBmp);
                            }
                        });
            }
            builder.setCustomTitle(titleView);
            builder.setMessage(buildDialogText(context,
                    R.string.my_io_body_intro_signed_in,
                    R.string.my_io_dialog_first_bullet_point_signed_in,
                    R.string.my_io_dialog_second_bullet_point_signed_in,
                    R.string.my_io_dialog_third_bullet_point_signed_in,
                    Color.BLACK));
        } else {
            builder.setMessage(buildDialogText(getContext(),
                    R.string.my_io_body_intro_signed_out,
                    R.string.my_io_dialog_first_bullet_point_signed_out,
                    R.string.my_io_dialog_second_bullet_point_signed_out,
                    R.string.my_io_dialog_third_bullet_point_signed_out,
                    Color.BLACK));
        }

        builder.setPositiveButton(signedIn ? R.string.signout_prompt : R.string.signin_prompt,
                new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MyIOActivity myIOActivity = ((MyIOActivity) getActivity());
                        if (AccountUtils.hasActiveAccount(myIOActivity)) {
                            myIOActivity.signOut();
                        } else {
                            myIOActivity.signIn();
                        }
                        dismiss();
                    }
                });

        return builder.create();
    }

    private CharSequence buildDialogText(@NonNull Context context, @StringRes int intro,
            @StringRes int bullet1, @StringRes int bullet2, @StringRes int bullet3,
            @ColorInt int color) {
        SpannableStringBuilder ssb = new SpannableStringBuilder(context.getString(intro));
        int padding = context.getResources().getDimensionPixelSize(R.dimen.padding_normal);
        ssb.append("\n\n");
        ssb.append(context.getString(bullet1),
                new BulletSpan(padding, color), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssb.append("\n\n");
        ssb.append(context.getString(bullet2),
                new BulletSpan(padding, color), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssb.append("\n\n");
        ssb.append(context.getString(bullet3),
                new BulletSpan(padding, color), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return ssb;
    }
}
