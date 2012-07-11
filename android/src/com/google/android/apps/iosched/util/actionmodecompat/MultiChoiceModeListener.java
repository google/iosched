/*
 * Copyright 2012 Google Inc.
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

package com.google.android.apps.iosched.util.actionmodecompat;

/**
 * A MultiChoiceModeListener receives events for {@link AbsListView#CHOICE_MODE_MULTIPLE_MODAL}.
 * It acts as the {@link ActionMode.Callback} for the selection mode and also receives {@link
 * #onItemCheckedStateChanged(ActionMode, int, long, boolean)} events when the user selects and
 * deselects list items.
 */
public interface MultiChoiceModeListener extends ActionMode.Callback {

    /**
     * Called when an item is checked or unchecked during selection mode.
     *
     * @param mode     The {@link ActionMode} providing the selection mode
     * @param position Adapter position of the item that was checked or unchecked
     * @param id       Adapter ID of the item that was checked or unchecked
     * @param checked  <code>true</code> if the item is now checked, <code>false</code> if the
     *                 item is now unchecked.
     */
    public void onItemCheckedStateChanged(ActionMode mode,
            int position, long id, boolean checked);
}
