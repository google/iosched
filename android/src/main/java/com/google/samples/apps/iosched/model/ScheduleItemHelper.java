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

package com.google.samples.apps.iosched.model;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ScheduleItemHelper {

    public static final long ALLOWED_OVERLAP = 5 * 60 * 1000; // 5 minutes

    /**
     * Find and resolve time slot conflicts. Items should already be ordered by start time.
     * Conflicts among mutableItems, if any, won't be checked, and they will be left as is.
     **/
    public static ArrayList<ScheduleItem> processItems(
            @NonNull final ArrayList<ScheduleItem> items) {
        // mark conflicting immutable:
        markConflicting(items);

        final ArrayList<ScheduleItem> result = new ArrayList<>(items);
        Collections.sort(result, new Comparator<ScheduleItem>() {
            @Override
            public int compare(ScheduleItem lhs, ScheduleItem rhs) {
                return lhs.startTime < rhs.startTime ? -1 : 1;
            }
        });

        return result;
    }

    static void markConflicting(@NonNull final ArrayList<ScheduleItem> items) {
        final int size = items.size();
        for (int i = 0; i < size; i++) {
            final ScheduleItem item = items.get(i);
            // Notice that we only care about sessions when checking conflicts.
            if (item.type == ScheduleItem.SESSION && item.inSchedule) {
                for (int j = i + 1; j < size; j++) {
                    final ScheduleItem other = items.get(j);
                    if (other.type == ScheduleItem.SESSION && other.inSchedule) {
                        if (intersect(other, item, true)) {
                            other.flags |= ScheduleItem.FLAG_CONFLICTS_WITH_PREVIOUS;
                            item.flags |= ScheduleItem.FLAG_CONFLICTS_WITH_NEXT;
                        } else {
                            // we assume the list is ordered by starttime so break from inner loop
                            break;
                        }
                    }
                }
            }
        }
    }

    private static boolean intersect(ScheduleItem block1, ScheduleItem block2, boolean useOverlap) {
        return block2.endTime > (block1.startTime + (useOverlap ? ALLOWED_OVERLAP : 0))
                && (block2.startTime + (useOverlap ? ALLOWED_OVERLAP : 0)) < block1.endTime;
    }
}
