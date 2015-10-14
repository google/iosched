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
package com.google.samples.apps.iosched.framework;

import com.bumptech.glide.request.ResourceCallback;

import android.app.LoaderManager;
import android.content.Loader;
import android.support.test.espresso.IdlingResource;

import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of an {@link android.support.test.espresso.IdlingResource} to allow the UI test
 * framework to track the idle/busy state of Loaders in order to avoid flakiness of UI tests.
 */
public class LoaderIdlingResource implements IdlingResource {

    /**
     * ResourceCallback to be notified when this resource goes idle.
     */
    private ResourceCallback mResourceCallback;

    /**
     * List of Ids of loaders that we know have been started and are likely to be loading.
     */
    private Set<Integer> mLoadersLoading = new HashSet<>();

    private final String mName;

    private final LoaderManager mLoaderManager;

    public LoaderIdlingResource(String name, LoaderManager loaderManager) {
        mName = name;
        mLoaderManager = loaderManager;
    }

    @Override
    public String getName() {
        return mName;
    }

    @Override
    public boolean isIdleNow() {
        cleanupLoaders();
        return mLoadersLoading.isEmpty();
    }

    /**
     * Look at the list of loading loaders and remove them if they have been completed or abandoned.
     * This is necessary as there are cases where a LoaderManager will notify LoaderCallbacks that
     * a Loader has been abandoned.
     */
    private void cleanupLoaders() {
        for (int loaderId : mLoadersLoading) {
            Loader loader = mLoaderManager.getLoader(loaderId);
            // If a Loader has completed, abandoned etc... it is not referenced anymore by the
            // LoaderManager so in this case we can remove it from the list of loading loaders.
            if (loader == null) {
                mLoadersLoading.remove(loaderId);
            }
        }
    }

    /**
     * Indicates the given {@code Loader} started loading. This is typically called from an
     * {@link android.app.LoaderManager.LoaderCallbacks#onCreateLoader(int, android.os.Bundle)}.
     */
    public void onLoaderStarted(Loader loader) {
        mLoadersLoading.add(loader.getId());
    }

    /**
     * Indicates the given {@code Loader} has finished loading. This is typically called from an
     * {@link android.app.LoaderManager.LoaderCallbacks#onLoadFinished(android.content.Loader, Object)}
     * and {@link android.app.LoaderManager.LoaderCallbacks#onLoaderReset(android.content.Loader)}.
     */
    public void onLoaderFinished(Loader loader) {
        mLoadersLoading.remove(loader.getId());
        if (isIdleNow() && mResourceCallback != null) {
            mResourceCallback.onTransitionToIdle();
        }
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback resourceCallback) {
        mResourceCallback = resourceCallback;
    }
}
