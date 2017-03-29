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

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.view.Menu;
import android.view.MenuItem;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.google.samples.apps.iosched.lib.R;
import com.google.samples.apps.iosched.myschedule.ScheduleView;
import com.google.samples.apps.iosched.navigation.NavigationModel;
import com.google.samples.apps.iosched.ui.BaseActivity;

import com.google.samples.apps.iosched.util.AccountUtils;
import com.google.samples.apps.iosched.util.SessionsHelper;

public class MyIOActivity extends BaseActivity {

    private static final String SCREEN_LABEL = "My I/O";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.myio_act);
    }

    @Override
    protected NavigationModel.NavigationItemEnum getSelfNavDrawerItem() {
        return NavigationModel.NavigationItemEnum.MY_IO;
    }

    @Override
    public boolean canSwipeRefreshChildScrollUp() {
        final Fragment contentFragment = getSupportFragmentManager()
                .findFragmentById(R.id.schedule_content);

        if (contentFragment instanceof ScheduleView) {
            return ((ScheduleView) contentFragment).canSwipeRefreshChildScrollUp();
        }

        return false;
    }

    @Override
    protected String getScreenLabel() {
        return SCREEN_LABEL;
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        final MenuItem avatar = menu.findItem(R.id.menu_avatar);
        if (AccountUtils.hasActiveAccount(this)) {
            Uri photoUrl = AccountUtils.getActiveAccountPhotoUrl(this);
            if (photoUrl != null) {
                Glide.with(this).load(photoUrl.toString()).asBitmap()
                     .into(new SimpleTarget<Bitmap>(100, 100) {
                         @Override
                         public void onResourceReady(Bitmap resource,
                                 GlideAnimation glideAnimation) {
                             RoundedBitmapDrawable circularBitmapDrawable =
                                     RoundedBitmapDrawableFactory.create(getResources(), resource);
                             circularBitmapDrawable.setCircular(true);
                             avatar.setIcon(circularBitmapDrawable);
                         }
                     });
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.my_io, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_avatar) {
            showSignedOutDialogFragment();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void showSignedOutDialogFragment() {
        FragmentManager fm = getSupportFragmentManager();
        MyIODialogFragment myIODialogFragment = MyIODialogFragment.newInstance();
        myIODialogFragment.show(fm, "my_io_signed_in_dialog_frag");
    }
}
