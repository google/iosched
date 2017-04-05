package com.google.samples.apps.iosched.sync.userdata;

import android.content.Context;

import java.util.List;

/**
 * Responsible for managing user data sync.
 */
public class UserDataSyncHelper extends AbstractUserDataSyncHelper {

    /**
     * Tracks if the sync process changed local data.
     */
    private boolean mDataChanged = false;

    /**
     * A list of {@link UserAction}s that are involved in the data sync.
     */
    private List<UserAction> mActions;

    /**
     * Constructor.
     *
     * @param context     The {@link Context}.
     * @param accountName The name associated with the currently chosen account.
     */
    public UserDataSyncHelper(final Context context, final String accountName) {
        super(context, accountName);
        // Note: mContext set in parent.
    }

    @Override
    protected boolean syncImpl(final List<UserAction> actions, final boolean hasPendingLocalData) {
        mActions = actions;
        performSync(actions);
        return mDataChanged;
    }

    private void performSync(final List<UserAction> actions){
        // TODO: add sync logic.
    }
}
