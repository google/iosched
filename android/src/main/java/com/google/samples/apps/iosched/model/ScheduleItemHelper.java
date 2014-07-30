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

import java.util.*;

public class ScheduleItemHelper {

    private static final long FREE_BLOCK_MINIMUM_LENGTH = 10 * 60 * 1000; // 10 minutes
    public static final long ALLOWED_OVERLAP = 5 * 60 * 1000; // 5 minutes

    /**
     * Find and resolve time slot conflicts.
     * Items should already be ordered by start time. Conflicts among mutableItems, if any,
     * won't be checked, and they will be left as is.
     **/
    static public ArrayList<ScheduleItem> processItems(ArrayList<ScheduleItem> mutableItems, ArrayList<ScheduleItem> immutableItems) {

        // move mutables as necessary to accommodate conflicts with immutables:
        moveMutables(mutableItems, immutableItems);

        // mark conflicting immutable:
        markConflicting(immutableItems);

        ArrayList<ScheduleItem> result = new ArrayList<ScheduleItem>();
        result.addAll(immutableItems);
        result.addAll(mutableItems);

        Collections.sort(result, new Comparator<ScheduleItem>() {
            @Override
            public int compare(ScheduleItem lhs, ScheduleItem rhs) {
                return lhs.startTime < rhs.startTime ? -1 : 1;
            }
        });

        return result;
    }

    static protected void markConflicting(ArrayList<ScheduleItem> items) {
        for (int i=0; i<items.size(); i++) {
            ScheduleItem item = items.get(i);
            // Notice that we only care about sessions when checking conflicts.
            if (item.type == ScheduleItem.SESSION) for (int j=i+1; j<items.size(); j++) {
                ScheduleItem other = items.get(j);
                if (item.type == ScheduleItem.SESSION) {
                    if (intersect(other, item, true)) {
                        other.flags |= ScheduleItem.FLAG_CONFLICTS_WITH_PREVIOUS;
                    } else {
                        // we assume the list is ordered by starttime
                        break;
                    }
                }
            }
        }
    }

    static protected void moveMutables(ArrayList<ScheduleItem> mutableItems, ArrayList<ScheduleItem> immutableItems) {
        Iterator<ScheduleItem> immutableIt = immutableItems.iterator();

        while (immutableIt.hasNext()) {
            ScheduleItem immutableItem = immutableIt.next();
            if (immutableItem.type == ScheduleItem.BREAK) {
                // Breaks (lunch, after hours, etc) should not make free blocks to move
                continue;
            }
            ListIterator<ScheduleItem> mutableIt = mutableItems.listIterator();
            while (mutableIt.hasNext()) {
                ScheduleItem mutableItem = mutableIt.next();
                ScheduleItem split = null;

                // If mutable item is overlapping the immutable one
                if (intersect(immutableItem, mutableItem, true)) {
                    if (isContainedInto(mutableItem, immutableItem)) {
                        // if mutable is entirely contained into immutable, just remove it
                        mutableIt.remove();
                        continue;
                    } else if (isContainedInto(immutableItem, mutableItem)) {
                        // if immutable is entirely contained into mutable, split mutable if necessary:
                        if (isIntervalLongEnough(immutableItem.endTime, mutableItem.endTime)) {
                            split = (ScheduleItem) mutableItem.clone();
                            split.startTime = immutableItem.endTime;
                        }
                        mutableItem.endTime = immutableItem.startTime;
                    } else if (mutableItem.startTime < immutableItem.endTime) {
                        // Adjust the start of the mutable
                        mutableItem.startTime = immutableItem.endTime;
                    } else if (mutableItem.endTime > immutableItem.startTime) {
                        // Adjust the end of the mutable
                        mutableItem.endTime = immutableItem.startTime;
                    }

                    if (!isIntervalLongEnough(mutableItem.startTime, mutableItem.endTime)) {
                        mutableIt.remove();
                    }
                    if (split != null) {
                        mutableIt.add(split);
                    }
                }
            }
        }

    }

    static private boolean isIntervalLongEnough(long start, long end) {
        return ( end - start ) >= FREE_BLOCK_MINIMUM_LENGTH;
    }
    static private boolean intersect(ScheduleItem block1, ScheduleItem block2, boolean useOverlap) {
        return block2.endTime > ( block1.startTime + ( useOverlap ? ALLOWED_OVERLAP : 0 ) )
                && ( block2.startTime + ( useOverlap ?ALLOWED_OVERLAP : 0 ) ) < block1.endTime;
    }

    static private boolean isContainedInto(ScheduleItem contained, ScheduleItem container) {
        return contained.startTime >= container.startTime &&
                contained.endTime <= container.endTime;
    }


}
