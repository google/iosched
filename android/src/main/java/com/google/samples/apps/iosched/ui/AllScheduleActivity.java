package com.google.samples.apps.iosched.ui;

import com.google.samples.apps.iosched.model.AllScheduleHelper;
import com.google.samples.apps.iosched.model.BaseScheduleHelper;

/**
 * Created by kgalligan on 8/11/14.
 */
public class AllScheduleActivity extends BaseScheduleActivity {
    @Override
    BaseScheduleHelper makeScheduleHelper() {
        return new AllScheduleHelper(this);
    }

    @Override
    protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_FULL_SCHEDULE;
    }

}
