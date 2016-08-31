package com.google.samples.apps.iosched.videolibrary;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;

import com.google.samples.apps.iosched.ui.BaseActivity;
import com.google.samples.apps.iosched.ui.widget.DrawShadowFrameLayout;
import com.google.samples.apps.iosched.util.AnalyticsHelper;

import no.java.schedule.v2.R;

/**
 * Created by kkho on 28.05.2016.
 */
public class JzVideoLibraryActivity extends BaseActivity {
    private static final String SCREEN_LABEL = "JavaZone Video Library";
    private Fragment mFragment;
    public static final int ACTIVITY_RESULT = 90;
    public static final String SEARCH_PARAM = "SEARCH_PARAM";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.jz_video_library_act);

        if (savedInstanceState == null) {
            mFragment = new JzVideoLibraryFragment();
            mFragment.setArguments(intentToFragmentArguments(getIntent()));
            getFragmentManager().beginTransaction()
                    .add(R.id.root_container, mFragment, "single_pane")
                    .commit();
        } else {
            mFragment = getFragmentManager().findFragmentByTag("single_pane");
        }

        // ANALYTICS SCREEN: View the video library screen
        // Contains: Nothing (Page name is a constant)
        AnalyticsHelper.sendScreenView(SCREEN_LABEL);

        registerHideableHeaderView(findViewById(R.id.headerbar));
    }

    public Fragment getFragment() {
        return mFragment;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_VIDEO_LIBRARY;
    }

    @Override
    protected void onActionBarAutoShowOrHide(boolean shown) {
        super.onActionBarAutoShowOrHide(shown);
        DrawShadowFrameLayout frame = (DrawShadowFrameLayout) findViewById(R.id.main_content);
        frame.setShadowVisible(shown, shown);
    }
    /*

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        // Add the search button to the toolbar.
        Toolbar toolbar = getActionBarToolbar();
        toolbar.inflateMenu(R.menu.explore_io_menu);
        toolbar.setOnMenuItemClickListener(this);
        return true;
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.menu_search:
                startActivityForResult(new Intent(this, SearchVideoActivity.class), ACTIVITY_RESULT);
                return true;
        }
        return false;
    } */

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ACTIVITY_RESULT:
                if (resultCode == RESULT_OK) {
                    Bundle res = data.getExtras();

                    ((JzVideoLibraryFragment)mFragment).doWebViewCall(res.getString(SEARCH_PARAM));
                }
                break;
        }
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if(((JzVideoLibraryFragment)mFragment).webViewKeyDown(keyCode)) {
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
