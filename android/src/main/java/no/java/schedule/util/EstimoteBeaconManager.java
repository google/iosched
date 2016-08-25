package no.java.schedule.util;

import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.connection.DeviceConnection;
import com.estimote.sdk.connection.exceptions.DeviceConnectionException;
import com.estimote.sdk.connection.settings.SettingCallback;

public class EstimoteBeaconManager {

    public BeaconManager InitializeAndGetBeaconManager() {
        DeviceConnection connection = connectionProvider.getConnection(device);
        boolean enable = true;
        connection.settings.beacon.secure().set(enable, new SettingCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean value) {
                // Handle success here
            }

            @Override
            public void onFailure(DeviceConnectionException exception) {
                // Handle failure here
            }
        });    }

}
