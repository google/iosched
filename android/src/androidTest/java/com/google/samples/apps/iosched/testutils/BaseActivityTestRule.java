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

package com.google.samples.apps.iosched.testutils;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.intent.rule.IntentsTestRule;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.samples.apps.iosched.archframework.Model;
import com.google.samples.apps.iosched.injection.ModelProvider;
import com.google.samples.apps.iosched.settings.ConfMessageCardUtils;
import com.google.samples.apps.iosched.settings.SettingsUtils;
import com.google.samples.apps.iosched.util.AccountUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An {@link IntentsTestRule} bypassing the {@link com.google.samples.apps.iosched.welcome
 * .WelcomeActivity}. If passed in the constructor, the {@code model} is injected into the app.
 */
public class BaseActivityTestRule<T extends Activity> extends IntentsTestRule<T> {

    private Model mModel;

    private boolean mAttending;

    /**
     * @param activityClass The Activity under test
     */
    public BaseActivityTestRule(final Class<T> activityClass) {
        super(activityClass);
    }

    /**
     * @param activityClass The Activity under test
     * @param model         A stub model to inject into the {@link ModelProvider}
     * @param attending     Whether the user should be set as attending or not
     */
    public BaseActivityTestRule(final Class<T> activityClass, Model model, boolean attending) {
        super(activityClass);
        mModel = model;
        mAttending = attending;
    }

    @Override
    protected void beforeActivityLaunched() {
        if (mAttending) {
            prepareActivityForInPersonAttendee();
        } else {
            prepareActivityForRemoteAttendee();
        }
        ModelProvider.setStubModel(mModel);
    }

    protected void prepareActivityForRemoteAttendee() {
        bypassTOsAndConduct();
        SettingsUtils.setAttendeeAtVenue(InstrumentationRegistry.getTargetContext(), false);
        SettingsUtils.markAnsweredLocalOrRemote(InstrumentationRegistry.getTargetContext(), false);
        selectFirstAccount();
    }

    protected void prepareActivityForInPersonAttendee() {
        bypassTOsAndConduct();
        SettingsUtils.setAttendeeAtVenue(InstrumentationRegistry.getTargetContext(), true);
        SettingsUtils.markAnsweredLocalOrRemote(InstrumentationRegistry.getTargetContext(), true);
        selectFirstAccount();
        disableConferenceMessages();
    }

    private void bypassTOsAndConduct() {
        SettingsUtils.markTosAccepted(InstrumentationRegistry.getTargetContext(), true);
        SettingsUtils.markConductAccepted(InstrumentationRegistry.getTargetContext(), true);
    }

    private void selectFirstAccount() {
        List<Account> availableAccounts = new ArrayList<Account>(
                Arrays.asList(AccountManager.get(InstrumentationRegistry.getTargetContext())
                                            .getAccountsByType(
                                                    GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE)));
        if (availableAccounts.size() > 0) {
            AccountUtils.setActiveAccount(InstrumentationRegistry.getTargetContext(),
                    availableAccounts.get(0).name);
        }
    }

    private void disableConferenceMessages() {
        ConfMessageCardUtils
                .markAnsweredConfMessageCardsPrompt(InstrumentationRegistry.getTargetContext(),
                        true);
    }

}
