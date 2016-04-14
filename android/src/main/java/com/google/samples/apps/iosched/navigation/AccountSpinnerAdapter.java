/*
 * Copyright (c) 2016 Google Inc.
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

package com.google.samples.apps.iosched.navigation;

import android.accounts.Account;
import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.ui.widget.BezelImageView;
import com.google.samples.apps.iosched.util.AccountUtils;
import com.google.samples.apps.iosched.util.ImageLoader;

/**
 * Adapter for the spinner showing the accounts in the navigation header.
 */
public class AccountSpinnerAdapter extends ArrayAdapter<Account> {

    private final ImageLoader mImageLoader;

    public AccountSpinnerAdapter(Context context, int textViewResourceId, Account[] accounts,
            ImageLoader imageLoader) {
        super(context, textViewResourceId, accounts);
        mImageLoader = imageLoader;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            holder = new ViewHolder();
            convertView =
                    LayoutInflater.from(getContext()).inflate(R.layout.account_spinner, null);
            holder.name = (TextView) convertView.findViewById(R.id.profile_name_text);
            holder.email = (TextView) convertView.findViewById(R.id.profile_email_text);
            convertView.setTag(R.layout.account_spinner, holder);
        } else {
            holder = (ViewHolder) convertView.getTag(R.layout.account_spinner);
        }

        holder.name.setText(AccountUtils.getPlusName(getContext()));
        holder.email.setText(getItem(position).name);

        return convertView;
    }

    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        DropViewHolder holder;

        if (convertView == null) {
            holder = new DropViewHolder();
            convertView =
                    LayoutInflater.from(getContext())
                                  .inflate(R.layout.account_spinner_dropdown, null);
            holder.image = (BezelImageView) convertView.findViewById(R.id.profile_image);
            holder.email = (TextView) convertView.findViewById(R.id.profile_email_text);
            convertView.setTag(R.layout.account_spinner_dropdown, holder);
        } else {
            holder = (DropViewHolder) convertView.getTag(R.layout.account_spinner_dropdown);
        }

        String profileImageUrl = AccountUtils.getPlusImageUrl(getContext(), getItem(position).name);
        if (profileImageUrl != null) {
            mImageLoader.loadImage(AccountUtils.getPlusImageUrl(getContext(), getItem(position).name),
                    holder.image);
        } else {
            holder.image.setImageResource(R.drawable.ic_default_avatar);
        }
        String email = getItem(position).name;
        holder.email.setText(email);
        String chosenAccount = AccountUtils.getActiveAccountName(getContext());
        Resources res = getContext().getResources();
        holder.email.setContentDescription(email.equals(chosenAccount) ?
                res.getString(R.string.talkback_selected, email) :
                res.getString(R.string.talkback_not_selected, email));

        return convertView;
    }

    static class ViewHolder {
        TextView name;
        TextView email;
    }

    static class DropViewHolder {
        BezelImageView image;
        TextView email;
    }

}