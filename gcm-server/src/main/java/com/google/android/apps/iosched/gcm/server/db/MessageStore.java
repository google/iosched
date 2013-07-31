/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.apps.iosched.gcm.server.db;

import com.google.android.apps.iosched.gcm.server.db.models.MulticastMessage;

import java.util.List;
import java.util.logging.Logger;

import static com.google.android.apps.iosched.gcm.server.db.OfyService.ofy;

/**
 * Simple implementation of a data store using standard Java collections.
 * <p>
 * This class is neither persistent (it will lost the data when the app is
 * restarted) nor thread safe.
 */
public final class MessageStore {
    private static final Logger LOG = Logger.getLogger(MessageStore.class.getName());

    private MessageStore() {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a persistent record with the devices to be notified using a
     * multicast message.
     *
     * @param devices registration ids of the devices
     * @param type message type
     * @param extraData additional message payload
     * @return ID for the persistent record
     */
    public static Long createMulticast(List<String> devices,
                                         String type,
                                         String extraData) {
        LOG.info("Storing multicast for " + devices.size() + " devices. (type=" + type + ")");
        MulticastMessage msg = new MulticastMessage();
        msg.setDestinations(devices);
        msg.setAction(type);
        msg.setExtraData(extraData);
        ofy().save().entity(msg).now();
        Long id = msg.getId();
        LOG.fine("Multicast ID: " + id);
        return id;
    }

    /**
     * Gets a persistent record with the devices to be notified using a
     * multicast message.
     *
     * @param id ID for the persistent record
     */
    public static MulticastMessage getMulticast(Long id) {
        return ofy().load().type(MulticastMessage.class).id(id).get();
    }

    /**
     * Updates a persistent record with the devices to be notified using a
     * multicast message.
     *
     * @param id ID for the persistent record.
     * @param devices new list of registration ids of the devices.
     */
    public static void updateMulticast(Long id, List<String> devices) {
        MulticastMessage msg = ofy().load().type(MulticastMessage.class).id(id).get();
        if (msg == null) {
            LOG.severe("No entity for multicast ID: " + id);
            return;
        }
        msg.setDestinations(devices);
        ofy().save().entity(msg);
    }

    /**
     * Deletes a persistent record with the devices to be notified using a
     * multicast message.
     *
     * @param id ID for the persistent record.
     */
    public static void deleteMulticast(Long id) {
        ofy().delete().type(MulticastMessage.class).id(id);
    }

}
