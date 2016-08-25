package no.java.schedule.util;

import android.content.Context;
import android.util.Log;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Nearable;
import com.estimote.sdk.Region;
import com.estimote.sdk.connection.DeviceConnection;
import com.estimote.sdk.connection.exceptions.DeviceConnectionException;
import com.estimote.sdk.connection.settings.SettingCallback;

import java.util.List;
import java.util.UUID;

public class EstimoteBeaconManager {
    private BeaconManager mBeaconManager;
    private Region mRegion;
    private String scanId;
    private static final String TAG = "EstimoteBeaconManager";

    public EstimoteBeaconManager(Context context, String regionName) {
        mBeaconManager = new BeaconManager(context);
        //mRegion = new Region(regionName,
        //        UUID.fromString(), null, null);
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
        mBeaconManager.stopRanging(mRegion);
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
                mBeaconManager.startRanging(mRegion);
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
}
