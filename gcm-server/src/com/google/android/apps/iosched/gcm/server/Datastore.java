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
package com.google.android.apps.iosched.gcm.server;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Simple implementation of a data store using standard Java collections. This class is neither
 * persistent (it will lost the data when the app is restarted) nor thread safe.
 */
public final class Datastore {
    private static final Logger logger = Logger.getLogger(Datastore.class.getSimpleName());

    static final String MULTICAST_TYPE = "Multicast";
    static final String MULTICAST_REG_IDS_PROPERTY = "regIds";
    static final String MULTICAST_ANNOUNCEMENT_PROPERTY = "announcement";
    static final int MULTICAST_SIZE = 1000;

    static final String DEVICE_TYPE = "Device";
    static final String DEVICE_REG_ID_PROPERTY = "regid";

    private static final FetchOptions DEFAULT_FETCH_OPTIONS = FetchOptions.Builder
            .withPrefetchSize(MULTICAST_SIZE)
            .chunkSize(MULTICAST_SIZE);

    private static final DatastoreService ds = DatastoreServiceFactory.getDatastoreService();

    private Datastore() {
        throw new UnsupportedOperationException();
    }

    /**
     * Registers a device.
     */
    public static void register(String regId) {
        logger.info("Registering " + regId);
        Transaction txn = ds.beginTransaction();
        try {
            Entity entity = findDeviceByRegId(regId);
            if (entity != null) {
                logger.fine(regId + " is already registered; ignoring.");
                return;
            }
            entity = new Entity(DEVICE_TYPE);
            entity.setProperty(DEVICE_REG_ID_PROPERTY, regId);
            ds.put(entity);
            txn.commit();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }


    /**
     * Unregisters a device.
     */
    public static void unregister(String regId) {
        logger.info("Unregistering " + regId);
        Transaction txn = ds.beginTransaction();
        try {
            Entity entity = findDeviceByRegId(regId);
            if (entity == null) {
                logger.warning("Device " + regId + " already unregistered");
            } else {
                Key key = entity.getKey();
                ds.delete(key);
            }
            txn.commit();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }

    /**
     * Updates the registration id of a device.
     */
    public static void updateRegistration(String oldId, String newId) {
        logger.info("Updating " + oldId + " to " + newId);
        Transaction txn = ds.beginTransaction();
        try {
            Entity entity = findDeviceByRegId(oldId);
            if (entity == null) {
                logger.warning("No device for registration id " + oldId);
                return;
            }
            entity.setProperty(DEVICE_REG_ID_PROPERTY, newId);
            ds.put(entity);
            txn.commit();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }

    /**
     * Gets the number of total devices.
     */
    public static int getTotalDevices() {
        Transaction txn = ds.beginTransaction();
        try {
            Query query = new Query(DEVICE_TYPE).setKeysOnly();
            List<Entity> allKeys =
                    ds.prepare(query).asList(DEFAULT_FETCH_OPTIONS);
            int total = allKeys.size();
            logger.fine("Total number of devices: " + total);
            txn.commit();
            return total;
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }

    /**
     * Gets all registered devices.
     */
    public static List<String> getDevices() {
        List<String> devices;
        Transaction txn = ds.beginTransaction();
        try {
            Query query = new Query(DEVICE_TYPE);
            Iterable<Entity> entities =
                    ds.prepare(query).asIterable(DEFAULT_FETCH_OPTIONS);
            devices = new ArrayList<String>();
            for (Entity entity : entities) {
                String device = (String) entity.getProperty(DEVICE_REG_ID_PROPERTY);
                devices.add(device);
            }
            txn.commit();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
        return devices;
    }

    /**
     * Returns the device object with the given registration ID.
     */
    private static Entity findDeviceByRegId(String regId) {
        Query query = new Query(DEVICE_TYPE)
                .addFilter(DEVICE_REG_ID_PROPERTY, FilterOperator.EQUAL, regId);
        PreparedQuery preparedQuery = ds.prepare(query);
        List<Entity> entities = preparedQuery.asList(DEFAULT_FETCH_OPTIONS);
        Entity entity = null;
        if (!entities.isEmpty()) {
            entity = entities.get(0);
        }
        int size = entities.size();
        if (size > 0) {
            logger.severe(
                    "Found " + size + " entities for regId " + regId + ": " + entities);
        }
        return entity;
    }

    /**
     * Creates a persistent record with the devices to be notified using a multicast message.
     *
     * @param devices      registration ids of the devices.
     * @param announcement announcement messageage
     * @return encoded key for the persistent record.
     */
    public static String createMulticast(List<String> devices, String announcement) {
        String type = (announcement == null || announcement.trim().length() == 0)
                ? "sync"
                : ("announcement: " + announcement);
        logger.info("Storing multicast (" + type + ") for " + devices.size() + " devices");
        String encodedKey;
        Transaction txn = ds.beginTransaction();
        try {
            Entity entity = new Entity(MULTICAST_TYPE);
            entity.setProperty(MULTICAST_REG_IDS_PROPERTY, devices);
            entity.setProperty(MULTICAST_ANNOUNCEMENT_PROPERTY, announcement);
            ds.put(entity);
            Key key = entity.getKey();
            encodedKey = KeyFactory.keyToString(key);
            logger.fine("multicast key: " + encodedKey);
            txn.commit();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
        return encodedKey;
    }

    /**
     * Gets a persistent record with the devices to be notified using a multicast message.
     *
     * @param encodedKey encoded key for the persistent record.
     */
    public static Entity getMulticast(String encodedKey) {
        Key key = KeyFactory.stringToKey(encodedKey);
        Entity entity;
        Transaction txn = ds.beginTransaction();
        try {
            entity = ds.get(key);
            txn.commit();
            return entity;
        } catch (EntityNotFoundException e) {
            logger.severe("No entity for key " + key);
            return null;
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }

    /**
     * Updates a persistent record with the devices to be notified using a multicast message.
     *
     * @param encodedKey encoded key for the persistent record.
     * @param devices    new list of registration ids of the devices.
     */
    public static void updateMulticast(String encodedKey, List<String> devices) {
        Key key = KeyFactory.stringToKey(encodedKey);
        Entity entity;
        Transaction txn = ds.beginTransaction();
        try {
            try {
                entity = ds.get(key);
            } catch (EntityNotFoundException e) {
                logger.severe("No entity for key " + key);
                return;
            }
            entity.setProperty(MULTICAST_REG_IDS_PROPERTY, devices);
            ds.put(entity);
            txn.commit();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }

    /**
     * Deletes a persistent record with the devices to be notified using a multicast message.
     *
     * @param encodedKey encoded key for the persistent record.
     */
    public static void deleteMulticast(String encodedKey) {
        Transaction txn = ds.beginTransaction();
        try {
            Key key = KeyFactory.stringToKey(encodedKey);
            ds.delete(key);
            txn.commit();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }
}
