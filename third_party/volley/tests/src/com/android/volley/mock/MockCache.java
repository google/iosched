/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.volley.mock;

import com.android.volley.Cache;

public class MockCache implements Cache {

    public boolean clearCalled = false;
    @Override
    public void clear() {
        clearCalled = true;
    }

    public boolean getCalled = false;
    private Entry mFakeEntry = null;

    public void setEntryToReturn(Entry entry) {
        mFakeEntry = entry;
    }

    @Override
    public Entry get(String key) {
        getCalled = true;
        return mFakeEntry;
    }

    public boolean putCalled = false;
    public String keyPut = null;
    public Entry entryPut = null;

    @Override
    public void put(String key, Entry entry) {
        putCalled = true;
        keyPut = key;
        entryPut = entry;
    }

    @Override
    public void invalidate(String key, boolean fullExpire) {
    }

    @Override
    public void remove(String key) {
    }

	@Override
	public void initialize() {
	}

}
