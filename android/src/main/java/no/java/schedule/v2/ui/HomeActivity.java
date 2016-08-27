package no.java.schedule.v2.ui;


import android.accounts.Account;
import android.content.ContentResolver;
import android.content.SyncStatusObserver;
import android.os.AsyncTask;
import android.os.Bundle;

import com.google.samples.apps.iosched.explore.ExploreIOActivity;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.tracking.android.EasyTracker;

public class HomeActivity extends ExploreIOActivity {
    private Object mSyncObserverHandle;
    private AsyncTask<Void, Void, Void> mGCMRegisterTask;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isFinishing()) {
            return;
        }

        EasyTracker.getTracker().setContext(this);

        if (savedInstanceState == null) {
            triggerRefresh();
        }
    }

    private void triggerRefresh() {
        Bundle extras = new Bundle();
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);

            ContentResolver.requestSync(
                    new Account("JavaZone Schedule","no.java.schedule"),
                    ScheduleContract.CONTENT_AUTHORITY, extras);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mGCMRegisterTask != null) {
            mGCMRegisterTask.cancel(true);
        }
    }


            @Override
    protected void onPause() {
        super.onPause();
        if (mSyncObserverHandle != null) {
            ContentResolver.removeStatusChangeListener(mSyncObserverHandle);
            mSyncObserverHandle = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSyncStatusObserver.onStatusChanged(0);

        // Watch for sync state changes
        final int mask = ContentResolver.SYNC_OBSERVER_TYPE_PENDING |
                ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE;
        mSyncObserverHandle = ContentResolver.addStatusChangeListener(mask, mSyncStatusObserver);
    }

    private final SyncStatusObserver mSyncStatusObserver = new SyncStatusObserver() {
        @Override
        public void onStatusChanged(int which) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    Account account = new Account("JavaZone Schedule", "no.java.schedule");
                    boolean syncActive = ContentResolver.isSyncActive(
                            account, ScheduleContract.CONTENT_AUTHORITY);
                    boolean syncPending = ContentResolver.isSyncPending(
                            account, ScheduleContract.CONTENT_AUTHORITY);
                }
            });
        }
    };
}
