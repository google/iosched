/*
 * Copyright 2014 Google Inc. All rights reserved.
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

package com.google.samples.apps.iosched.ui;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ListFragment;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.samples.apps.iosched.Config;
import co.touchlab.droidconnyc.R;

import com.google.samples.apps.iosched.model.AllScheduleHelper;
import com.google.samples.apps.iosched.model.BaseScheduleHelper;
import com.google.samples.apps.iosched.model.ScheduleHelper;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.ui.widget.MyScheduleView;
import com.google.samples.apps.iosched.ui.widget.SlidingTabLayout;
import com.google.samples.apps.iosched.util.*;

import java.lang.ref.WeakReference;
import java.util.*;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

public class MyScheduleActivity extends BaseScheduleActivity implements MyScheduleFragment.Listener {

    @Override
    BaseScheduleHelper makeScheduleHelper() {
        return new ScheduleHelper(this);
    }

    @Override
    protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_MY_SCHEDULE;
    }

}
