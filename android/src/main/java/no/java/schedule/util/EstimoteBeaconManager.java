package no.java.schedule.util;

import android.content.Context;
import android.util.Log;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Nearable;
import com.estimote.sdk.Region;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class EstimoteBeaconManager {
    private BeaconManager mBeaconManager;
    private Region mRegion;
    private ArrayList<Region> mRegionList;
    private ArrayList<String> mRoomUUIDs;
    private HashMap<String, String[]> mRegionBeaconMapping;
    private String scanId;
    private static final String TAG = "EstimoteBeaconManager";
    private static final String ROOM_1_UUID_1 = "b9407f30-f5f8-466e-aff9-25556b57fe6d";
    private static final String ROOM_1_UUID_2 = "";

    public EstimoteBeaconManager(Context context, String[] regionName) {
        mBeaconManager = new BeaconManager(context);
        initializeRegionAndUUIDs(regionName);
    }

    private void initializeRegionAndUUIDs(String[]regionName) {
        if (mRegionList == null) {
            mRegionList = new ArrayList<>();
        }

        if(mRoomUUIDs == null) {
            mRoomUUIDs = new ArrayList<>();
        }

        if(mRegionBeaconMapping == null) {
            mRegionBeaconMapping = new HashMap<>();
        }

        for (int i = 0; i < regionName.length; i++) {
            mRegionList.add(i, new Region(regionName[i],
                    UUID.fromString(ROOM_1_UUID_1)
                    , 14441, 57772));
        }

    }

    public void initializeEstimoteBeaconManager(Context context) {
        if (mBeaconManager == null) {
            mBeaconManager = new BeaconManager(context);
        }
        // Should be invoked in #onCreate.
        mBeaconManager.setNearableListener(new BeaconManager.NearableListener() {
            @Override
            public void onNearablesDiscovered(List<Nearable> nearables) {
                Log.d(TAG, "Discovered nearables: " + nearables);
            }
        });
    }

    public void startEstimoteBeaconManager() {
        // Should be invoked in #onStart.
        mBeaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                scanId = mBeaconManager.startNearableDiscovery();
            }
        });
    }

    public void stopRanging() {
        // invoked in #onPause
        mBeaconManager.stopRanging(mRegionList.get(0));
    }

    public void stopEstimoteBeaconManager() {
        // Should be invoked in #onStop.
        mBeaconManager.stopNearableDiscovery(scanId);
    }

    public void destroyEstimoteBeaconManager() {
        // When no longer needed. Should be invoked in #onDestroy.
        mBeaconManager.disconnect();
    }

    public void startMonitorEstimoteBeacons(Context context) {
        // called onResume
        if (mBeaconManager == null) {
            mBeaconManager = new BeaconManager(context);
        }

        mBeaconManager.connect(new BeaconManager.ServiceReadyCallback() {

            @Override
            public void onServiceReady() {
                mBeaconManager.startRanging(mRegionList.get(0));
                mBeaconManager.setRangingListener(new BeaconManager.RangingListener() {
                    @Override
                    public void onBeaconsDiscovered(Region region, List<Beacon> list) {
                        if (!list.isEmpty()) {
                            Beacon nearestBeacon = list.get(0);
                        }
                    }
                });
            }
        });
    }

    private class BeaconInformation {
        private String UUID;
        private int major;
        private int minor;

        public BeaconInformation(String uuid, int ma, int mi) {
            UUID = uuid;
            major = ma;
            minor = mi;
        }

        public String getUUID() {
            return UUID;
        }

        public void setUUID(String UUID) {
            this.UUID = UUID;
        }

        public int getMajor() {
            return major;
        }

        public void setMajor(int major) {
            this.major = major;
        }

        public int getMinor() {
            return minor;
        }

        public void setMinor(int minor) {
            this.minor = minor;
        }
    }
}
