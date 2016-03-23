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

package com.google.samples.apps.iosched.ui.widget;

import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ListView;

/**
 * Defines the expected interactions between an activity and the header view. Both {@link
 * com.google.samples.apps.iosched.ui.BaseActivity} and {@link HeaderViewImpl} implement this.
 * <p/>
 * The main feature is auto-hiding the HeaderView upon scrolling.
 */
public interface HeaderView {

    /**
     * Sets the correct offset for the progress bar when the action bar is shown.
     */
    void setProgressBarTopWhenActionBarShown(int progressBarTopWhenActionBarShown);

    /**
     * Implements auto hiding the HeaderView when scrolling the {@code listView}.
     */
    void enableActionBarAutoHide(ListView listView);

    /**
     * Adds the {@code hideableHeaderView} to the views being hidden when the auto-hide feature.
     */
    void registerHideableHeaderView(View hideableHeaderView);

    /**
     * Removes the {@code hideableHeaderView} from the views being hidden when the auto-hide
     * feature.
     */
    void deregisterHideableHeaderView(View hideableHeaderView);

    /**
     * @return the {@link Toolbar} to allow customisation by each Activity.
     */
    Toolbar getActionBarToolbar();
}
