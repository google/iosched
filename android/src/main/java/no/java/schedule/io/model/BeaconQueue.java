package no.java.schedule.io.model;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.Region;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.TreeSet;

public class BeaconQueue {
    private ArrayList<BeaconInformationItem> mBeaconList ;

    public BeaconQueue() {
        mBeaconList =  new ArrayList<>();
    }

    public void insertBeaconInformation(JzBeaconRegion region, List<Beacon> beacons) {
        for(Beacon beacon : beacons) {
            mBeaconList.add(new BeaconInformationItem(region, beacon));
        }
        Collections.sort(mBeaconList, new BeaconComparator());
    }

    public boolean isEmpty() {
        return mBeaconList.isEmpty();
    }

    public BeaconInformationItem getHighestRssiBeacon() {
        return mBeaconList.get(0);
    }

    public void clearBeacons() {
        mBeaconList.clear();
    }

    public class BeaconComparator implements Comparator<BeaconInformationItem> {
        @Override
        public int compare(BeaconInformationItem u1, BeaconInformationItem u2) {
            return Integer.compare(u2.getmBeacon().getRssi(), u1.getmBeacon().getRssi());
        }
    }

    public class BeaconInformationItem {
        private JzBeaconRegion mRegion;
        private Beacon mBeacon;

        public BeaconInformationItem(JzBeaconRegion region, Beacon beacon) {
            mRegion = region;
            mBeacon = beacon;
        }

        public JzBeaconRegion getmRegion() {
            return mRegion;
        }

        public Beacon getmBeacon() {
            return mBeacon;
        }
    }
}
