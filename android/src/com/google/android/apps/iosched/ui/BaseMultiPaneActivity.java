/*
 * Copyright 2011 Google Inc.
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

package com.google.android.apps.iosched.ui;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import java.util.List;

/**
 * A {@link BaseActivity} that can contain multiple panes, and has the ability to substitute
 * fragments for activities when intents are fired using
 * {@link BaseActivity#openActivityOrFragment(android.content.Intent)}.
 */
public abstract class BaseMultiPaneActivity extends BaseActivity {
    /** {@inheritDoc} */
    @Override
    public void openActivityOrFragment(final Intent intent) {
        final PackageManager pm = getPackageManager();
        List<ResolveInfo> resolveInfoList = pm
                .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resolveInfoList) {
            final FragmentReplaceInfo fri = onSubstituteFragmentForActivityLaunch(
                    resolveInfo.activityInfo.name);
            if (fri != null) {
                final Bundle arguments = intentToFragmentArguments(intent);
                final FragmentManager fm = getSupportFragmentManager();

                try {
                    Fragment fragment = (Fragment) fri.getFragmentClass().newInstance();
                    fragment.setArguments(arguments);

                    FragmentTransaction ft = fm.beginTransaction();
                    ft.replace(fri.getContainerId(), fragment, fri.getFragmentTag());
                    onBeforeCommitReplaceFragment(fm, ft, fragment);
                    ft.commit();
                } catch (InstantiationException e) {
                    throw new IllegalStateException(
                            "Error creating new fragment.", e);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(
                            "Error creating new fragment.", e);
                }
                return;
            }
        }
        super.openActivityOrFragment(intent);
    }

    /**
     * Callback that's triggered to find out if a fragment can substitute the given activity class.
     * Base activites should return a {@link FragmentReplaceInfo} if a fragment can act in place
     * of the given activity class name.
     */
    protected FragmentReplaceInfo onSubstituteFragmentForActivityLaunch(String activityClassName) {
        return null;
    }

    /**
     * Called just before a fragment replacement transaction is committed in response to an intent
     * being fired and substituted for a fragment.
     */
    protected void onBeforeCommitReplaceFragment(FragmentManager fm, FragmentTransaction ft,
            Fragment fragment) {
    }

    /**
     * A class describing information for a fragment-substitution, used when a fragment can act
     * in place of an activity.
     */
    protected class FragmentReplaceInfo {
        private Class mFragmentClass;
        private String mFragmentTag;
        private int mContainerId;

        public FragmentReplaceInfo(Class fragmentClass, String fragmentTag, int containerId) {
            mFragmentClass = fragmentClass;
            mFragmentTag = fragmentTag;
            mContainerId = containerId;
        }

        public Class getFragmentClass() {
            return mFragmentClass;
        }

        public String getFragmentTag() {
            return mFragmentTag;
        }

        public int getContainerId() {
            return mContainerId;
        }
    }
}
