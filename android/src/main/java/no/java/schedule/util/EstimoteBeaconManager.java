package no.java.schedule.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Nearable;
import com.estimote.sdk.Region;
import com.estimote.sdk.repackaged.gson_v2_3_1.com.google.gson.Gson;
import com.google.samples.apps.iosched.io.map.model.Marker;
import com.google.samples.apps.iosched.map.util.MarkerModel;
import com.google.samples.apps.iosched.provider.ScheduleContract;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import no.java.schedule.io.model.BeaconQueue;
import no.java.schedule.io.model.JzBeaconRegion;
import no.java.schedule.io.model.JzRegionList;

public class EstimoteBeaconManager {
    private BeaconManager mBeaconManager;
    private ArrayList<Region> mRegionList;
    private JzRegionList mJzRegionList;
    private Context mContext;
    private HashMap<String, String[]> mRegionBeaconMapping;
    private String scanId;
    private static final String TAG = "EstimoteBeaconManager";
    private static final String RegionInfoJson = "regioninfo.json";
    private BeaconQueue mBeaconQueue;
    private ScheduledExecutorService  mScheduledExecutorService;

    public EstimoteBeaconManager(Context context) {
        mContext = context;
        mBeaconManager = new BeaconManager(context);
        initializeRegionAndUUIDs();
        mBeaconQueue = new BeaconQueue();
        mScheduledExecutorService = Executors.newScheduledThreadPool(1);
    }

    private void initializeRegionAndUUIDs() {
        if (mRegionList == null) {
            mRegionList = new ArrayList<>();
        }


        if (mRegionBeaconMapping == null) {
            mRegionBeaconMapping = new HashMap<>();
        }


        // read json info
        try {
            String fileToJsonObj = JsonUtil.assetJSONFile(RegionInfoJson, mContext);
            mJzRegionList = new Gson().fromJson(fileToJsonObj, JzRegionList.class);
            if (mJzRegionList != null) {
                for (int i = 0; i < mJzRegionList.getRegions().size(); i++) {
                    JzBeaconRegion region = mJzRegionList.getRegions().get(i);
                    mRegionList.add(new Region(region.getName(),
                            UUID.fromString(mJzRegionList.getUUID()),
                            region.getMajor(),
                            null));
                    storeMarkerToDatabase(region);
                }
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void storeMarkerToDatabase(JzBeaconRegion region) {
        ContentValues values = new ContentValues();
        values.put(ScheduleContract.MapMarkers.MARKER_ID, region.getName());
        values.put(ScheduleContract.MapMarkers.MARKER_TYPE, region.getType());
        values.put(ScheduleContract.MapMarkers.MARKER_LABEL, region.getDescription());
        values.put(ScheduleContract.MapMarkers.MARKER_FLOOR, region.getLevel());
        values.put(ScheduleContract.MapMarkers.MARKER_LATITUDE, region.getCoordinates().getLatitude());
        values.put(ScheduleContract.MapMarkers.MARKER_LONGITUDE, region.getCoordinates().getLongitude());

        Uri uri = mContext.getContentResolver()
                .insert(ScheduleContract.MapMarkers.buildMarkerUri(),
                        values);
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
        mScheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if(!mBeaconQueue.isEmpty()) {
                    BeaconQueue.BeaconInformationItem item = mBeaconQueue.getHighestRssiBeacon();
                    Beacon highestRssiBeacon = item.getmBeacon();
                    JzBeaconRegion regionLocation = item.getmRegion();
                    mBeaconQueue.clearBeacons();

                }
            }
        },0, 15, TimeUnit.SECONDS);
    }

    public void stopRanging() {
        // invoked in #onPause
        mBeaconManager.stopRanging(mRegionList.get(0));
    }

    public void stopEstimoteBeaconManager() {
        // Should be invoked in #onStop.
        mBeaconManager.stopNearableDiscovery(scanId);
        mScheduledExecutorService.shutdown();
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

                for (Region region : mRegionList) {
                    mBeaconManager.startMonitoring(region);
                    mBeaconManager.startRanging(region);
                }

                mBeaconManager.setMonitoringListener(new BeaconManager.MonitoringListener() {
                    @Override
                    public void onEnteredRegion(Region region, List<Beacon> beacons) {
                        if (!beacons.isEmpty()) {
                        }
                    }

                    @Override
                    public void onExitedRegion(Region region) {

                    }

                });

                mBeaconManager.setRangingListener(new BeaconManager.RangingListener() {
                    @Override
                    public void onBeaconsDiscovered(Region region, List<Beacon> list) {
                        if (!list.isEmpty()) {
                            JzBeaconRegion beaconRegion = getRegion(region);
                            mBeaconQueue.insertBeaconInformation(beaconRegion,list);
                        }
                    }
                });
            }
        });
    }

    private JzBeaconRegion getRegion(Region region) {
        for(JzBeaconRegion beaconRegion : mJzRegionList.getRegions()) {
            if(beaconRegion.getName().equals(region.getIdentifier())) {
                return beaconRegion;
            }
        }

        return null;
    }
}
