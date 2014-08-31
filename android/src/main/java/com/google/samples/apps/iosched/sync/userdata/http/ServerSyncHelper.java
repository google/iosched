/**
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

package com.google.samples.apps.iosched.sync.userdata.http;

import android.content.Context;

import com.google.samples.apps.iosched.port.superbus.SyncAllRsvpsCommand;
import com.google.samples.apps.iosched.port.tasks.AppPrefs;
import com.google.samples.apps.iosched.sync.userdata.AbstractUserDataSyncHelper;

import co.touchlab.android.superbus.appsupport.CommandBusHelper;

/**
 * Submits a remote sync call.  This isn't doing much, to be honest.  The work is happening elsewhere.
 * I really didn't want to modify the existing architecture unless there was a good reason to do so.
 * Lazy, but we've got other things to do.
 */
public class ServerSyncHelper extends AbstractUserDataSyncHelper
{
    public ServerSyncHelper(Context context, String accountName) {
        super(context, accountName);
    }

    @Override
    public boolean sync()
    {
        if(AppPrefs.getInstance(mContext).isLoggedIn())
            CommandBusHelper.submitCommandSync(mContext, new SyncAllRsvpsCommand(mAccountName));
        return false;
    }
}
