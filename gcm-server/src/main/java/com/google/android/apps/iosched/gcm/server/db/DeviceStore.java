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

package com.google.android.apps.iosched.gcm.server.db;

import static com.google.android.apps.iosched.gcm.server.db.OfyService.ofy;

import com.google.android.apps.iosched.gcm.server.db.models.Device;

import java.util.List;
import java.util.logging.Logger;

public class DeviceStore {
    private static final Logger LOG = Logger.getLogger(DeviceStore.class.getName());

    /**
     * Registers a device.
     *
     * @param gcmId device's registration id.
     */
    public static void register(String gcmId, String gcmGroupId) {
        LOG.info("Registering device.\nGroup ID: " + gcmGroupId + "\nGCM ID: " + gcmId);
        Device oldDevice = findDeviceByGcmId(gcmId);
        if (oldDevice == null) {
            // Existing device not found (as expected)
            Device newDevice = new Device();
            newDevice.setGcmId(gcmId);
            newDevice.setGcmGroupId(gcmGroupId);
            ofy().save().entity(newDevice);
        } else {
            // Existing device found
            LOG.warning(gcmId + " is already registered");
            if (!gcmGroupId.equals(oldDevice.getGcmGroupId())) {
                LOG.info("GcmGroupId has changed from '" + oldDevice.getGcmGroupId() + "' to '"
                        + gcmGroupId + "'");
                oldDevice.setGcmGroupId(gcmGroupId);
                ofy().save().entity(oldDevice);
            }
        }
    }

    /**
     * Unregisters a device.
     *
     * @param gcmId device's registration id.
     */
    public static void unregister(String gcmId) {
        Device device = findDeviceByGcmId(gcmId);
        if (device == null) {
            LOG.warning("Device " + gcmId + " already unregistered");
            return;
        }
        LOG.info("Unregistering " + gcmId);
        ofy().delete().entity(device);
    }

    /**
     * Updates the registration id of a device.
     */
    public static void updateRegistration(String oldGcmId, String newGcmId) {
        LOG.info("Updating " + oldGcmId + " to " + newGcmId);
        Device oldDevice = findDeviceByGcmId(oldGcmId);
        if (oldDevice == null) {
            LOG.warning("No device for registration id " + oldGcmId);
            return;
        }
        // Device exists. Since we use the GCM key as the (immutable) primary key,
        // we must create a new entity.
        Device newDevice = new Device();
        newDevice.setGcmId(newGcmId);
        newDevice.setGcmGroupId(oldDevice.getGcmGroupId());
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
        return ofy().load().type(Device.class).list();
    }

    public static Device findDeviceByGcmId(String regId) {
        return ofy().load().type(Device.class).id(regId).get();
    }

    public static List<Device> findDevicesByGcmGroupId(String target) {
        return ofy().load().type(Device.class).filter("gPlusId", target).list();
    }
}
