package no.java.schedule.io.model;

import com.google.gson.annotations.SerializedName;

public class JzBeaconRegion {

    @SerializedName("beacons")
    @Expose
    private List<Beacon__> beacons = new ArrayList<Beacon__>();

    /**
     *
     * @return
     * The beacons
     */
    public List<Beacon_> getBeacons() {
        return beacons;
    }

    /**
     *
     * @param beacons
     * The beacons
     */
    public void setBeacons(List<Beacon> beacons) {
        this.beacons = beacons;
    }
}
