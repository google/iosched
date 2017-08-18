/*
 * Copyright 2014 Google Inc. All rights reserved.
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

package com.google.samples.apps.iosched.server.gcm.db;

import static com.googlecode.objectify.ObjectifyService.ofy;

import com.google.samples.apps.iosched.server.gcm.db.models.Device;

import java.util.List;
import java.util.logging.Logger;

public class DeviceStore {
    private static final Logger LOG = Logger.getLogger(DeviceStore.class.getName());

    /**
     * Registers a device.
     *
     * @param deviceId device's registration id.
     */
    public static void register(String deviceId, String userId) {
        LOG.info("Registering device.\nGroup ID: " + userId + "\nGCM ID: " + deviceId);
        Device oldDevice = findDeviceByDeviceId(deviceId);
        if (oldDevice == null) {
            // Existing device not found (as expected)
            Device newDevice = new Device();
            newDevice.setDeviceId(deviceId);
            newDevice.setUserId(userId);
            ofy().save().entity(newDevice);
        } else {
            // Existing device found
            LOG.warning(deviceId + " is already registered");
            if (userId == null || !userId.equals(oldDevice.getUserId())) {
                LOG.info("User ID has changed from '" + oldDevice.getUserId() + "' to '"
                        + userId + "'");
                oldDevice.setUserId(userId);
                ofy().save().entity(oldDevice);
            }
        }
    }

    /**
     * Unregisters a device.
     *
     * @param deviceId device's registration id.
     */
    public static void unregister(String deviceId) {
        Device device = findDeviceByDeviceId(deviceId);
        if (device == null) {
            LOG.warning("Device " + deviceId + " already unregistered");
            return;
        }
        LOG.info("Unregistering " + deviceId);
        ofy().delete().entity(device);
    }

    /**
     * Updates the registration id of a device.
     */
    public static void updateRegistration(String oldDeviceId, String newDeviceId) {
        LOG.info("Updating " + oldDeviceId + " to " + newDeviceId);
        Device oldDevice = findDeviceByDeviceId(oldDeviceId);
        if (oldDevice == null) {
            LOG.warning("No device for registration id " + oldDeviceId);
            return;
        }
        // Device exists. Since we use the GCM key as the (immutable) primary key,
        // we must create a new entity.
        Device newDevice = new Device();
        newDevice.setDeviceId(newDeviceId);
        newDevice.setUserId(oldDevice.getUserId());
        ofy().save().entity(newDevice);
        ofy().delete().entity(oldDevice);
    }

    /**
     * Gets registered device count.
     */
    public static int getDeviceCount() {
        return ofy().load().type(Device.class).count();
    }

    public static List<Device> getAllDevices() {
        return ofy().load().type(Device.class).filter("userId >", "").list();
    }

    public static Device findDeviceByDeviceId(String deviceId) {
        return ofy().load().type(Device.class).id(deviceId).now();
    }

    public static List<Device> findDevicesByUserId(String target) {
        return ofy().load().type(Device.class).filter("userId", target).list();
    }
}
