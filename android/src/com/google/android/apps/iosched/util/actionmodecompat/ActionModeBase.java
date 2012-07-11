/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.apps.iosched.util.actionmodecompat;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A pre-Honeycomb, simple implementation of {@link ActionMode} that simply shows a context menu
 * for the action mode.
 */
class ActionModeBase extends ActionMode implements DialogInterface.OnClickListener {
    private FragmentActivity mActivity;
    private Callback mCallback;
    private MenuInflater mMenuInflater;

    private ContextMenuDialog mDialog;
    private CharSequence mTitle;
    private SimpleMenu mMenu;
    private ArrayAdapter<MenuItem> mMenuItemArrayAdapter;

    ActionModeBase(FragmentActivity activity, Callback callback) {
        mActivity = activity;
        mCallback = callback;
    }

    static ActionModeBase startInternal(final FragmentActivity activity,
            Callback callback) {
        final ActionModeBase actionMode = new ActionModeBase(activity, callback);
        actionMode.startInternal();
        return actionMode;
    }

    void startInternal() {
        mMenu = new SimpleMenu(mActivity);
        mCallback.onCreateActionMode(this, mMenu);
        mCallback.onPrepareActionMode(this, mMenu);
        mMenuItemArrayAdapter = new ArrayAdapter<MenuItem>(mActivity,
                android.R.layout.simple_list_item_1,
                android.R.id.text1);
        invalidate();

        // DialogFragment.show() will take care of adding the fragment
        // in a transaction.  We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.
        FragmentManager fm = mActivity.getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag("action_mode_context_menu");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        mDialog = new ContextMenuDialog();
        mDialog.mActionModeBase = this;
        mDialog.show(ft, "action_mode_context_menu");
    }

    /**{@inheritDoc}*/
    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
    }

    /**{@inheritDoc}*/
    @Override
    public void setTitle(int resId) {
        mTitle = mActivity.getResources().getString(resId);
    }

    /**{@inheritDoc}*/
    @Override
    public void invalidate() {
        mMenuItemArrayAdapter.clear();
        List<MenuItem> items = new ArrayList<MenuItem>();
        for (int i = 0; i < mMenu.size(); i++) {
            MenuItem item = mMenu.getItem(i);
            if (item.isVisible()) {
                items.add(item);
            }
        }
        Collections.sort(items, new Comparator<MenuItem>() {
            @Override
            public int compare(MenuItem a, MenuItem b) {
                return a.getOrder() - b.getOrder();
            }
        });
        for (MenuItem item : items) {
            mMenuItemArrayAdapter.add(item);
        }
    }

    /**{@inheritDoc}*/
    @Override
    public void finish() {
        if (mDialog != null) {
            mDialog.dismiss();
        }

        mCallback.onDestroyActionMode(this);

        mDialog = null;
        mMenu = null;
        mMenuItemArrayAdapter = null;
        mTitle = null;
    }

    /**{@inheritDoc}*/
    @Override
    public CharSequence getTitle() {
        return mTitle;
    }

    /**{@inheritDoc}*/
    @Override
    public MenuInflater getMenuInflater() {
        if (mMenuInflater == null) {
            mMenuInflater = mActivity.getMenuInflater();
        }
        return mMenuInflater;
    }

    public static void beginMultiChoiceMode(ListView listView, final FragmentActivity activity,
            final MultiChoiceModeListener listener) {
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view,
                    int position, long id) {
                ActionMode mode = ActionModeBase.start(activity, listener);
                listener.onItemCheckedStateChanged(mode, position, id, true);
                return true;
            }
        });
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int position) {
        mCallback.onActionItemClicked(this, mMenuItemArrayAdapter.getItem(position));
    }

    public static class ContextMenuDialog extends DialogFragment {
        ActionModeBase mActionModeBase;

        public ContextMenuDialog() {
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            if (mActionModeBase == null) {
                // TODO: support orientation changes and avoid this awful hack.
                final Dialog d = new AlertDialog.Builder(getActivity()).create();
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        d.dismiss();
                    }
                });
                return d;
            }

            return new AlertDialog.Builder(getActivity())
                    .setTitle(mActionModeBase.mTitle)
                    .setAdapter(mActionModeBase.mMenuItemArrayAdapter, mActionModeBase)
                    .create();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
            if (mActionModeBase == null) {
                return;
            }

            mActionModeBase.mDialog = null;
            mActionModeBase.finish();
        }
    }
}
