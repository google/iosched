package com.google.samples.apps.iosched.port;

import co.touchlab.android.superbus.SuperbusConfig;
import co.touchlab.android.superbus.appsupport.SimpleCommandPersistedApplication;
import co.touchlab.android.superbus.errorcontrol.ConfigException;
import co.touchlab.android.superbus.network.ConnectionChangeBusEventListener;

/**
 * Created by kgalligan on 8/17/14.
 */
public class DroidconApplication extends SimpleCommandPersistedApplication
{
    @Override
    protected void buildConfig(SuperbusConfig.Builder builder) throws ConfigException
    {
        super.buildConfig(builder);
        builder.addEventListener(new ConnectionChangeBusEventListener());
    }

}
