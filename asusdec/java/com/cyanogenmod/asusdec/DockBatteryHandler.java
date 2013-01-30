/*
 * Copyright (C) 2013 The CyanogenMod Project
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

package com.cyanogenmod.asusdec;

import android.app.ActivityManagerNative;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.UserHandle;
import android.util.Log;

import com.android.internal.os.DeviceDockBatteryHandler;

public final class DockBatteryHandler implements DeviceDockBatteryHandler {
    private static final String TAG = "AsusdecDockBatteryHandler";

    private static final boolean DEBUG = false;

    private Context mContext;
    private final Handler mHandler;

    /* Begin native fields: All of these fields are set by native code. */
    private int mDockBatteryStatus;
    private int mDockBatteryLevel;
    private boolean mDockBatteryPresent;
    private boolean mDockBatteryPlugged;
    /* End native fields. */

    private boolean mInitial;
    private int mLastDockBatteryStatus;
    private int mLastDockBatteryLevel;
    private boolean mLastDockBatteryPresent;
    private boolean mLastDockBatteryPlugged;

    private Object mLock = new Object();
    private boolean mIgnoreUpdates = false;

    static {
        AsusdecNative.loadAsusdecLib();
    }

    BroadcastReceiver mDockReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_DOCK_EVENT.equals(intent.getAction())) {
                int dockMode = intent.getIntExtra(
                        Intent.EXTRA_DOCK_STATE, Intent.EXTRA_DOCK_STATE_UNDOCKED);
                DockBatteryHandler.this.mDockBatteryPresent =
                        (dockMode != Intent.EXTRA_DOCK_STATE_UNDOCKED);
                handleProcessValues();
            }
        }
    };

    public DockBatteryHandler(Context context) {
        mContext = context;
        mHandler = new Handler(true /*async*/);
        mInitial = true;

        // set initial status
        refresh();

        // Listen for dock and screen on events
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_DOCK_EVENT);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        context.registerReceiver(mDockReceiver, filter);
    }

    private void refresh() {
        handleUpdate();
        handleProcessValues();
    }

    @Override
    public void handleUpdate() {
        nativeDockBatteryUpdate();
    }

    @Override
    public void handleProcessValues() {
        try {
            synchronized (mLock) {
                if (mIgnoreUpdates) return;
                mIgnoreUpdates = true;
            }

            if (DEBUG) {
                Log.d(TAG, "Processing new values: "
                        + "mDockBatteryStatus=" + mDockBatteryStatus
                        + ", mDockBatteryLevel=" + mDockBatteryLevel
                        + ", mDockBatteryPresent=" + mDockBatteryPresent
                        + ", mDockBatteryPlugged=" + mDockBatteryPlugged);
            }


            // TODO: Save dock battery stats


            // Has anything changed?
            if (mInitial ||
                mDockBatteryLevel != mLastDockBatteryLevel ||
                mDockBatteryStatus != mLastDockBatteryStatus ||
                mDockBatteryPresent != mLastDockBatteryPresent ||
                mDockBatteryPlugged != mLastDockBatteryPlugged) {

                // Notify the new status
                sendIntent();

                mInitial = false;
                mLastDockBatteryLevel = mDockBatteryLevel;
                mLastDockBatteryStatus = mDockBatteryStatus;
                mLastDockBatteryPresent = mDockBatteryPresent;
                mLastDockBatteryPlugged = mDockBatteryPlugged;
            }
        } finally {
            synchronized (mLock) {
                mIgnoreUpdates = false;
            }
        }
    }

    private void sendIntent() {
        //  Pack up the values and broadcast them to everyone
        final Intent intent = new Intent(Intent.ACTION_DOCK_BATTERY_CHANGED);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY
                | Intent.FLAG_RECEIVER_REPLACE_PENDING);


        intent.putExtra(BatteryManager.EXTRA_DOCK_PRESENT, mDockBatteryPresent);
        intent.putExtra(BatteryManager.EXTRA_DOCK_STATUS, mDockBatteryStatus);
        intent.putExtra(BatteryManager.EXTRA_DOCK_LEVEL, mDockBatteryLevel);
        intent.putExtra(BatteryManager.EXTRA_PLUGGED, mDockBatteryPlugged);

        if (DEBUG) {
            Log.d(TAG, "Sending ACTION_DOCK_BATTERY_CHANGED.  level:" + mDockBatteryLevel +
                    ", status:" + mDockBatteryStatus + ", present:" + mDockBatteryPresent +
                    ", plugged: " + mDockBatteryPlugged);
        }

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                ActivityManagerNative.broadcastStickyIntent(intent, null, UserHandle.USER_ALL);
            }
        });
    }

    @Override
    public boolean isPlugged() {
        return mDockBatteryPresent && mDockBatteryPlugged;
    }

    private native void nativeDockBatteryUpdate();
}
